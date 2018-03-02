/*
 * Copyright 2018 Matt Liotta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tokhn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Security;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.tokhn.core.Block;
import io.tokhn.core.Blockchain;
import io.tokhn.core.LocalBlock;
import io.tokhn.core.UTXO;
import io.tokhn.node.InvalidNetworkException;
import io.tokhn.node.Message;
import io.tokhn.node.Network;
import io.tokhn.node.Peer;
import io.tokhn.node.message.BlockMessage;
import io.tokhn.node.message.BlockRequestMessage;
import io.tokhn.node.message.DifficultyMessage;
import io.tokhn.node.message.ExitMessage;
import io.tokhn.node.message.PartialChainMessage;
import io.tokhn.node.message.PartialChainRequestMessage;
import io.tokhn.node.message.PingMessage;
import io.tokhn.node.message.TransactionMessage;
import io.tokhn.node.message.UtxoMessage;
import io.tokhn.node.message.UtxoRequestMessage;
import io.tokhn.node.message.WelcomeMessage;
import io.tokhn.store.MapDBBlockStore;
import io.tokhn.store.MapDBUTXOStore;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "Daemon", version = { "Tokhn 0.0.1", "(c) 2018 Matt Liotta" }, showDefaultValues = true)
public class Daemon extends Thread {
	private static final int TIMEOUT = 5000;//in milliseconds
	private final Map<Socket, NetworkThread> peers = Collections.synchronizedMap(new HashMap<>());
	private Map<Network, Blockchain> chains = new HashMap<>();
	private ServerSocket serverSocket;
	
	@Option(names = { "-h", "--help" }, usageHelp = true, description = "displays this help message and exit")
	private boolean helpRequested = false;
	
	@Option(names = { "-n", "--network" }, required = false, description = "the network")
	private Set<Network> networks = Network.getAll();
	
	@Option(names = { "-P", "--port" }, required = false, description = "the local port")
	private int PORT = 1337;
	
	@Option(names = { "-mp", "--max-peers" }, required = false, description = "the maximum number of peers")
	private int MAX_PEERS = 10;
	
	@Option(names = { "-bp", "--block-peer" }, required = false, description = "the peer to block")
	private List<InetAddress> blockPeers = new LinkedList<>();
	
	@Option(names = { "-v", "--version" }, versionHelp = true, description = "print version information and exit")
	boolean versionRequested;

	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		System.out.println("Daemon running...");
		CommandLine.run(new Daemon(), System.out, args);
	}

	@Override
	public void run() {
		Set<Peer> seededPeers = new HashSet<>();
		for(Network n : networks) {
			chains.put(n, new Blockchain(n, n.getVersion(), new MapDBBlockStore(n), new MapDBUTXOStore(n)));
			Arrays.stream(n.getSeedPeers()).filter(peer -> !blockPeers.contains(peer.getInetAddress())).forEach(p -> seededPeers.add(p));
		}

		// this is the start of client code
		seededPeers.forEach(peer -> {
			Socket peerSocket = new Socket();
			try {
				if (peers.size() <= MAX_PEERS) {
					peerSocket.connect(new InetSocketAddress(peer.host, peer.port), TIMEOUT);
					NetworkThread nt = new NetworkThread(peerSocket);
					peers.put(peerSocket, nt);
					nt.start();
				}
			} catch (IOException e) {
				//TODO: reconnect to lost peer connection
				System.err.printf("%s with %s\n", e, peerSocket.getInetAddress());
			}
		});
		
		// this is for our background heartbeat
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				broadcastMessage(new PingMessage(Network.TKHN));
			}
		}, 30, 30, TimeUnit.SECONDS);

		// this is the start of server code
		try {
			serverSocket = new ServerSocket(PORT);
		} catch (IOException e) {
			System.out.println(e);
		}

		while (true) {
			try {
				Socket clientSocket = serverSocket.accept();
				if (peers.size() > MAX_PEERS) {
					try {
						ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
						oos.writeObject(new ExitMessage(Network.TKHN));
						oos.flush();
						oos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					clientSocket.close();
				} else {
					NetworkThread nt = new NetworkThread(clientSocket);
					peers.put(clientSocket, nt);
					nt.start();
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}

	public WelcomeMessage getWelcomeMessage(Network network) {
		Blockchain chain = chains.get(network);
		WelcomeMessage welcomeMessage = new WelcomeMessage(network, Instant.now().getEpochSecond(), chain.getDifficulty(), chain.getReward(), chain.getLatestBlock());
		return welcomeMessage;
	}

	public void handleBlockMessage(BlockMessage blockMessage, Socket source) {
		Blockchain chain = chains.get(blockMessage.getNetwork());
		int chainDifficulty = chain.getDifficulty();
		if (chainDifficulty > blockMessage.block.getDifficulty()) {
			/*
			 * sending a block at too low a difficulty is either being naive or malicious
			 * given that our miner is actually naive, let's tell everyone the new
			 * difficulty
			 */
			broadcastMessage(new DifficultyMessage(blockMessage.getNetwork(), chainDifficulty));
		}
		if(chain.getBlock(blockMessage.block.getHash()) != null) {
			//we already have it so don't bother
		} else if (chain.addBlockToChain(blockMessage.block)) {
			// broadcast new block to peers
			broadcastMessage(blockMessage);
		}
	}

	public void handleTransactionMessage(TransactionMessage transactionMessage) {
		broadcastMessage(transactionMessage);
	}

	public void handleWelcomeMessage(WelcomeMessage welcomeMessage, Socket source) {
		Blockchain chain = chains.get(welcomeMessage.getNetwork());
		int chainIndex = chain.getLength();
		int welcomeIndex = welcomeMessage.latestBlock.getIndex();
		
		if(welcomeIndex > chainIndex) {
			//someone is claiming a further along chain
			if(welcomeIndex - chainIndex != 1) {
				//we are missing a bunch of blocks
				sendPeerMessage(source, new PartialChainRequestMessage(welcomeMessage.getNetwork(), chainIndex + 1, welcomeIndex));
			}
		}
	}

	public void handleDifficultyMessage(DifficultyMessage difficultyMessage, Socket source) {
		Blockchain chain = chains.get(difficultyMessage.getNetwork());
		int chainDifficulty = chain.getDifficulty();
		if (difficultyMessage.difficulty < chainDifficulty) {
			// this peer should learn about the higher difficulty
			sendPeerMessage(source, new DifficultyMessage(difficultyMessage.getNetwork(), chainDifficulty));
		}
	}
	
	public void handleBlockRequestMessage(BlockRequestMessage blockRequestMessage, Socket source) {
		Blockchain chain = chains.get(blockRequestMessage.getNetwork());
		Block b = chain.getBlock(blockRequestMessage.hash);
		if(b == null) {
			//we don't have the requested block, so relay the message
			broadcastMessage(blockRequestMessage);
		} else {
			//we have the requested block, so send it over
			sendPeerMessage(source, new BlockMessage(blockRequestMessage.getNetwork(), b));
		}
	}
	
	public void handlePartialChainMessage(PartialChainMessage partialChainMessage) {
		Blockchain chain = chains.get(partialChainMessage.getNetwork());
		List<Block> blocks = partialChainMessage.blocks;
		for(Block b : blocks) {
			if(chain.getBlock(b.getHash()) == null) {
				//we don't have block, so add it
				chain.addBlockToChain(b);
			}
		}
	}
	
	public void handlePartialChainRequestMessage(PartialChainRequestMessage partialChainRequestMessage, Socket source) {
		Blockchain chain = chains.get(partialChainRequestMessage.getNetwork());
		if(chain.getLength() >= partialChainRequestMessage.endIndex) {
			//we have the requested blocks
			LinkedList<Block> blocks = new LinkedList<>();
			LocalBlock b = chain.getLatestBlock();
			while(b != null) {
				if(b.getIndex() >= partialChainRequestMessage.startIndex && b.getIndex() <= partialChainRequestMessage.endIndex) {
					blocks.addFirst(b);
				}
				b = chain.getBlock(b.getPreviousHash());
			}
			sendPeerMessage(source, new PartialChainMessage(partialChainRequestMessage.getNetwork(), blocks));
		} else {
			//we don't have the blocks, so relay the message
			broadcastMessage(partialChainRequestMessage);
		}
	}
	
	public void handleUtxoRequestMessage(UtxoRequestMessage utxoRequestMessage, Socket source) {
		Blockchain chain = chains.get(utxoRequestMessage.getNetwork());
		List<UTXO> utxos = chain.getUtxosForAddress(utxoRequestMessage.address);
		sendPeerMessage(source, new UtxoMessage(utxoRequestMessage.getNetwork(), utxos));
	}

	private void sendPeerMessage(Socket peer, Message message) {
		try {
			message.addRelayHost(serverSocket.getInetAddress());
			
			peers.get(peer).sendMessage(message);
		} catch (IOException e) {
			System.err.println(e);
			// you're outta here
			peers.remove(peer);
		}
	}

	private void broadcastMessage(Message message) {
		message.addRelayHost(serverSocket.getInetAddress().getHostAddress());
		
		List<Socket> remove = new LinkedList<>();
		peers.entrySet().stream().forEach(p -> {
			try {
				NetworkThread nt = p.getValue();
				nt.sendMessage(message);
			} catch (IOException e) {
				System.err.println(e);
				// you're outta here
				remove.add(p.getKey());
			}
		});
		remove.stream().forEach(s -> peers.remove(s));
	}

	private boolean validMessage(Message message) {
		if(message == null) {
			return false;
		} else {
			try {
				Network network = Network.valueOf(message.getNetwork().getId());
				if(networks.contains(network) && network.getVersion() == message.getVersion()) {
					return true;
				} else {
					return false;
				}
			} catch (InvalidNetworkException e) {
				return false;
			}
		}
	}

	private class NetworkThread extends Thread {
		private Socket clientSocket = null;
		private ObjectInputStream ois = null;
		private ObjectOutputStream oos = null;

		public NetworkThread(Socket clientSocket) {
			this.clientSocket = clientSocket;
			System.out.printf("Connected to peer %s\n", clientSocket.getInetAddress());
		}

		@Override
		public void run() {
			try {
				for(Network n : networks) {
					sendMessage(getWelcomeMessage(n));
				}

				ois = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));

				while (true) {
					Object read = null;
					try {
						read = ois.readObject();
					} catch (EOFException e) {
						// client unexpectedly terminated connection
						break;
					} catch (ClassNotFoundException e) {
						System.err.println(e);
						break;
					}
					
					if(!validMessage((Message) read)) {
						System.err.println("invalid message");
					} else if (read instanceof ExitMessage) {
						break;
					} else if (read instanceof BlockMessage) {
						BlockMessage blockMessage = (BlockMessage) read;
						System.out.println(blockMessage);
						handleBlockMessage(blockMessage, clientSocket);
					} else if (read instanceof TransactionMessage) {
						TransactionMessage transactionMessage = (TransactionMessage) read;
						System.out.println(transactionMessage);
						handleTransactionMessage(transactionMessage);
					} else if (read instanceof WelcomeMessage) {
						WelcomeMessage welcomeMessage = (WelcomeMessage) read;
						System.out.println(welcomeMessage);
						handleWelcomeMessage(welcomeMessage, clientSocket);
					} else if (read instanceof DifficultyMessage) {
						DifficultyMessage difficultyMessage = (DifficultyMessage) read;
						System.out.println(difficultyMessage);
						handleDifficultyMessage(difficultyMessage, clientSocket);
					} else if (read instanceof BlockRequestMessage) {
						BlockRequestMessage blockRequestMessage = (BlockRequestMessage) read;
						System.out.println(blockRequestMessage);
						handleBlockRequestMessage(blockRequestMessage, clientSocket);
					} else if (read instanceof PartialChainMessage) {
						PartialChainMessage partialChainMessage = (PartialChainMessage) read;
						System.out.println(partialChainMessage);
						handlePartialChainMessage(partialChainMessage);
					} else if (read instanceof PartialChainRequestMessage) {
						PartialChainRequestMessage partialChainRequestMessage = (PartialChainRequestMessage) read;
						System.out.println(partialChainRequestMessage);
						handlePartialChainRequestMessage(partialChainRequestMessage, clientSocket);
					} else if (read instanceof UtxoMessage) {
						UtxoMessage utxoMessage = (UtxoMessage) read;
						System.out.println(utxoMessage);
					} else if (read instanceof UtxoRequestMessage) {
						UtxoRequestMessage utxoRequestMessage = (UtxoRequestMessage) read;
						System.out.println(utxoRequestMessage);
						handleUtxoRequestMessage(utxoRequestMessage, clientSocket);
					}
				}
				ois.close();
				clientSocket.close();
			} catch (IOException e) {
				System.err.printf("%s with %s\n", e, clientSocket.getInetAddress());
			}
		}

		public void sendMessage(Object message) throws IOException {
			if (oos == null) {
				// set this up for the first time
				oos = new ObjectOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
			}
			oos.writeObject(message);
			oos.flush();
		}
	}
}