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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.LongStream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.tokhn.core.Block;
import io.tokhn.core.TXO;
import io.tokhn.core.Token;
import io.tokhn.core.Transaction;
import io.tokhn.core.UTXO;
import io.tokhn.core.Wallet;
import io.tokhn.node.InvalidNetworkException;
import io.tokhn.node.Message;
import io.tokhn.node.Network;
import io.tokhn.node.message.BlockMessage;
import io.tokhn.node.message.DifficultyMessage;
import io.tokhn.node.message.ExitMessage;
import io.tokhn.node.message.PingMessage;
import io.tokhn.node.message.TransactionMessage;
import io.tokhn.node.message.WelcomeMessage;
import io.tokhn.store.MapDBWalletStore;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "Miner", version = { "Tokhn 0.0.1", "(c) 2018 Matt Liotta" }, showDefaultValues = true)
public class Miner extends Thread {
	private static final int TIMEOUT = 5000;//in milliseconds
	private State state = State.CLOSED;
	private Socket clientSocket = null;
	private ObjectInputStream ois = null;
	private ObjectOutputStream oos = null;
	private Map<Network, Block> tails = new HashMap<>();
	private Map<Network, Integer> difficulties = new HashMap<>();
	private Map<Network, Integer> rewards = new HashMap<>();
	/*
	 * the below data structure contains all the pending transactions for all the networks
	 * it is going to keep them in the natural of the networks, so TKHN will be first
	 * it could be supplied a different Comperator if a differen't order is desired
	 */
	private ConcurrentSkipListMap<Network,List<Transaction>> pendingTxs = new ConcurrentSkipListMap<>();
	private Wallet wallet = null;
	
	@Option(names = { "-h", "--help" }, usageHelp = true, description = "displays this help message and exit")
	private boolean helpRequested = false;
	
	@Option(names = { "-n", "--network" }, required = false, description = "the network")
	private Set<Network> networks = Network.getAll();
	
	@Option(names = { "-H", "--host" }, required = false, description = "the remote host")
	private String HOST = "localhost";
	
	@Option(names = { "-P", "--port" }, required = false, description = "the remote port")
	private int PORT = 1337;
	
	@Option(names = { "-v", "--version" }, versionHelp = true, description = "print version information and exit")
	private boolean versionRequested;

	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		CommandLine.run(new Miner(), System.out, args);
	}
	
	@Override
	public void run() {
		try {
			wallet = new Wallet(Network.TKHN.getVersion(), new MapDBWalletStore());
			if(wallet.getPrivateKey() == null || wallet.getPublicKey() == null) {
				System.err.println("Wallet needs to be generated before running Miner.");
				System.exit(-1);
			}
		} catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException | InvalidNetworkException e) {
			System.err.println(e);
			System.exit(-1);
		}

		try {
			clientSocket = new Socket();
			clientSocket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT);
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + HOST);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to  " + HOST);
		}

		if (clientSocket != null) {
			try {
				// let's open this baby up
				state = State.OPENING;
				new NetworkThread().start();
				while (state != State.CLOSED) {
					while (state != State.RUNNING) {
						// blocking like its 1999 (rewrite this shit in NIO)
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
					if(pendingTxs.isEmpty()) {
						//no pending transactions, so mine TKHN
						mineBlock(Network.TKHN, null);
					} else {
						Entry<Network, List<Transaction>> firstEntry = pendingTxs.firstEntry();
						mineBlock(firstEntry.getKey(), firstEntry.getValue());
					}
				}
				clientSocket.close();
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
			}
		}
	}
	
	private void mineBlock(Network network, List<Transaction> txs) {
		List<Transaction> transactions = new LinkedList<>();
		//add a reward for ourselves first
		int difficulty = difficulties.get(network);
		int reward = rewards.get(network);
		Block tail = tails.get(network);
		transactions.add(Transaction.rewardOf(network.getVersion(), wallet.getAddress(network), reward));
		//add all the pending transactions if any
		if(txs != null) {
			txs.forEach(tx -> transactions.add(tx));
		}
		
		Metric metric = new Metric();
		metric.start();
		Block block = LongStream.iterate(0, i -> i + 1).parallel()
				.peek(metric::handleLong)
				.mapToObj(i -> new Block(network, network.getVersion(), tail.getIndex() + 1, tail.getHash(), Instant.now().getEpochSecond(), transactions, difficulty, i))
				.filter(b -> Block.hashMatchesDifficulty(b.getHash(), difficulty)).findAny().orElse(null);
		metric.end();
		
		sendMessage(new BlockMessage(network, block));
		// let's wait to get our block back confirming it was added to chain
		state = State.WAITING;
		
		System.out.printf("Mined new block with difficulty of %d at a hash rate of %,.2f Mh/s\n", difficulty, metric.getRate());
	}

	private void sendMessage(Object message) {
		try {
			if (oos == null) {
				// set this up for the first time
				oos = new ObjectOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
			}
			oos.writeObject(message);
			oos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void updatePendingTxs(Network network) {
		List<Transaction> networkTxs = pendingTxs.get(network);
		if(networkTxs != null) {
			tails.get(network).getTransactions().stream().forEach(tx -> {
				networkTxs.remove(tx);
			});
		}
	}

	private void checkForReward(Block block) {
		List<UTXO> rewards = new LinkedList<>();
		for(Transaction tx : block.getTransactions()) {
			if(tx.getTxis().size() == 0 && tx.getTxos().size() == 1) {
				//this is a miner reward
				TXO txo = tx.getTxos().get(0);
				//make sure this is our reward
				if(txo.getAddress().equals(wallet.getAddress(block.getNetwork()))) {
					rewards.add(new UTXO(block.getNetwork(), tx.getId(), 0, txo.getAddress(), txo.getAmount()));
				}
			}
		}
		Token reward = rewards.stream().map(utxo -> utxo.getAmount()).reduce(Token.ZERO, (a, b) -> Token.sum(a, b));
		wallet.addUtxos(rewards);
		System.out.printf("Earned a reward of %s; wallet balace is now %s\n", reward, wallet.getBalance(block.getNetwork()));
	}

	private static enum State {
		CLOSED, OPENING, WAITING, RUNNING;
	}
	
	private class NetworkThread extends Thread {
		@Override
		public void run() {
			try {
				ois = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));

				/*
				 * I'm not saying its good, but always change state last to avoid thread issues
				 * And... since it got me, make sure EVERY case sets the state on the way out
				 */
				while (true) {
					Object read = null;
					try {
						read = ois.readObject();
					} catch (ClassNotFoundException e) {
						System.err.println(e);
						System.exit(-1);
					}
					
					if(!validMessage((Message) read)) {
						System.err.println("invalid message");
					} else if (read instanceof ExitMessage) {
						state = Miner.State.CLOSED;
						break;
					} else if (read instanceof PingMessage) {
						//PingMessage pingMessage = (PingMessage) read;
						//don't change state with a ping message
					} else if (read instanceof WelcomeMessage) {
						WelcomeMessage welcomeMessage = (WelcomeMessage) read;
						tails.put(welcomeMessage.getNetwork(), welcomeMessage.latestBlock);
						difficulties.put(welcomeMessage.getNetwork(), welcomeMessage.difficulty);
						rewards.put(welcomeMessage.getNetwork(), welcomeMessage.reward);
						state = Miner.State.RUNNING;
					} else if (read instanceof DifficultyMessage) {
						DifficultyMessage difficultyMessage = (DifficultyMessage) read;
						difficulties.put(difficultyMessage.getNetwork(), difficultyMessage.difficulty);
						state = Miner.State.RUNNING;
					} else if (read instanceof TransactionMessage) {
						// TODO: consider the case where we are RUNNING, but might get a new
						// TransactionMessage
						TransactionMessage transactionMessage = (TransactionMessage) read;
						if(!pendingTxs.containsKey(transactionMessage.getNetwork())) {
							pendingTxs.put(transactionMessage.getNetwork(), new LinkedList<Transaction>());
						}
						pendingTxs.get(transactionMessage.getNetwork()).add(transactionMessage.transaction);
						state = Miner.State.RUNNING;
					} else if (read instanceof BlockMessage) {
						BlockMessage blockMessage = (BlockMessage) read;
						tails.put(blockMessage.getNetwork(), blockMessage.block);
						updatePendingTxs(blockMessage.getNetwork());
						checkForReward(blockMessage.block);
						state = Miner.State.RUNNING;
					}
				}
				ois.close();
				clientSocket.close();
			} catch (IOException e) {
				System.err.println(e);
				state = Miner.State.CLOSED;
			}
		}
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
	
	private class Metric {
		Instant start;
		Instant end;
		long count = 0;
		
		public void start() {
			start = Instant.now();
		}
		
		public void end() {
			end = Instant.now();
		}
		
		public void handleLong(long l) {
			synchronized (this) {
				count++;
			}
		}
		
		public long getSeconds() {
			return Duration.between(start, end).getSeconds();
		}
		
		public double getRate() {
			return (double) count / getSeconds() / 1000000;
		}
	}
}