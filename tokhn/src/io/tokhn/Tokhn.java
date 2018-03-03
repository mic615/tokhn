package io.tokhn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.tokhn.core.Address;
import io.tokhn.core.Token;
import io.tokhn.core.UTXO;
import io.tokhn.core.Wallet;
import io.tokhn.node.InvalidNetworkException;
import io.tokhn.node.Network;
import io.tokhn.node.Peer;
import io.tokhn.node.Version;
import io.tokhn.node.message.BlockMessage;
import io.tokhn.node.message.TransactionMessage;
import io.tokhn.node.message.UtxoMessage;
import io.tokhn.node.message.UtxoRequestMessage;
import io.tokhn.store.MapDBWalletStore;

public class Tokhn implements Runnable, AutoCloseable {
	private static Scanner scanner = new Scanner( System.in );
	private Wallet wallet = null;
	private Network network = null;
	Socket clientSocket = null;
	private ObjectInputStream ois = null;
	private ObjectOutputStream oos = null;
	
	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());	
		Tokhn t = new Tokhn();
		t.run();
		try {
			t.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			wallet = new Wallet(Version.ZERO, new MapDBWalletStore());
		} catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException | InvalidNetworkException e) {
			System.err.println(e);
			System.exit(-1);
		}
		
		while(true) {
			System.out.print("Command: ");
			String input = scanner.nextLine();
			String[] parts = input.split(" ");
			Command c = Command.valueOf(parts[0].toUpperCase());
			switch(c) {
			case BALANCE:
				if(network != null) {
					handleBalance();
				}
				break;
			case EXIT:
				System.exit(0);
				break;
			case NETWORK:
				if(parts.length == 2) {
					network = Network.valueOf(parts[1].toUpperCase());
				} else {
					System.err.println("Invalid command");
				}
				break;
			case PAYADDRESS:
				if(network != null) {
					try {
						if(parts.length == 3) {
							handlePayAddress(new Address(parts[1]), Token.parseToken(parts[2]));
						} else {
							System.err.println("Invalid command");
						}
					} catch (InvalidNetworkException e) {
						System.err.println(e);
					}
				}
				break;
			default:
				System.err.println("Invalid command");
			case HELP:
				//TODO: implement this
				System.out.println("This is where the help info goes.");
				break;
			}
		}
	}
	
	private void handleBalance() {
		sendMessage(new UtxoRequestMessage(network, wallet.getAddress(network)));
		while(true) {
			try {
				Object read = ois.readObject();
				if (read instanceof UtxoMessage) {
					UtxoMessage utxoMessage = (UtxoMessage) read;
					List<UTXO> filtered = utxoMessage.utxos.stream()
							.filter(utxo -> utxo.getAddress().equals(wallet.getAddress(network)))
							.collect(Collectors.toList());
					wallet.addUtxos(filtered);
					System.out.printf("Balance has been updated for %s to %s\n", network, wallet.getBalance(network));
					break;
				}
			} catch (ClassNotFoundException e) {
				System.err.println(e);
				System.exit(-1);
			} catch (IOException e) {
				System.err.println(e);
			}
		}
	}
	
	private void handlePayAddress(Address address, Token amount) {
		try {
			sendMessage(new TransactionMessage(network, wallet.newTx(network, address, amount)));
			while(true) {
				try {
					Object read = ois.readObject();
					if (read instanceof BlockMessage) {
						BlockMessage blockMessage = (BlockMessage) read;
						if(blockMessage.block.getTransactions().stream().anyMatch(tx -> tx.getAllAddresses().contains(wallet.getAddress(network)))) {
							System.out.printf("Block %s contains a transaction for you.\n", blockMessage.block.getHash());
							break;
						} else {
							System.out.printf("Block %s just came by, but it doesn't have your transaction.\n");
						}
					}
				} catch (ClassNotFoundException e) {
					System.err.println(e);
					System.exit(-1);
				} catch (IOException e) {
					System.err.println(e);
				}
			}
		} catch (Exception e) {
			System.err.println(e);
		}
	}
	
	private void sendMessage(Object message) {
		if(clientSocket == null) {
			connect();
		}
		try {
			oos.writeObject(message);
			oos.flush();
		} catch (IOException e) {
			System.err.println(e);
		}
	}
	
	private void connect() {
		if(network != null) {
			//just get the first one until we switch to DNS management of seeded peers
			Peer peer = network.getSeedPeers()[0];
			clientSocket = new Socket();
			try {
				clientSocket.connect(new InetSocketAddress(peer.host, peer.port), 5000);
				ois = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));
				oos = new ObjectOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
			} catch (IOException e) {
				System.err.println(e);
			}
		}
	}
	
	@Override
	public void close() throws Exception {
		clientSocket.close();
	}
	
	private enum Command {
		BALANCE, EXIT, HELP, NETWORK, PAYADDRESS
	}
}