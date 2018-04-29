/*
 * Copyright 2018 Matt Liotta
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.tokhn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.tokhn.core.Address;
import io.tokhn.core.UTXO;
import io.tokhn.core.Wallet;
import io.tokhn.node.Message;
import io.tokhn.node.Network;
import io.tokhn.node.message.BlockMessage;
import io.tokhn.node.message.DifficultyMessage;
import io.tokhn.node.message.ExitMessage;
import io.tokhn.node.message.PartialChainMessage;
import io.tokhn.node.message.PartialChainRequestMessage;
import io.tokhn.node.message.PingMessage;
import io.tokhn.node.message.TransactionMessage;
import io.tokhn.node.message.UtxoMessage;
import io.tokhn.node.message.UtxoRequestMessage;
import io.tokhn.node.message.WelcomeMessage;
import io.tokhn.store.MapDBWalletStore;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "Client", version = { "Tokhn 0.0.1", "(c) 2018 Matt Liotta" }, showDefaultValues = true)
public class Client implements Runnable {
	private static final int TIMEOUT = 5000; //in milliseconds
	private Wallet wallet = null;
	@Option(names = { "-h", "--help" }, usageHelp = true, description = "displays this help message and exit")
	private boolean helpRequested = false;
	
	@Option(names = { "-n", "--network" }, required = false, description = "the network")
	private Network network = Network.TEST;
	
	@Option(names = { "-H", "--host" }, required = false, description = "the remote host")
	private String HOST = network.getParams().getHost();
	
	@Option(names = { "-P", "--port" }, required = false, description = "the remote port")
	private int PORT = network.getParams().getPort();
	
	@Option(names = { "-v", "--version" }, versionHelp = true, description = "print version information and exit")
	private boolean versionRequested;
	
	@Option(names = { "-g", "--generate" }, description = "generate wallet and exit")
	private boolean generateRequested;
	
	private ObjectInputStream ois = null;
	private ObjectOutputStream oos = null;
	
	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		CommandLine.run(new Client(), System.out, args);
	}
	
	@Override
	public void run() {
		try {
			if(generateRequested) {
				wallet = Wallet.build();
				byte[] publicKey = Base64.getEncoder().encode(wallet.getPublicKey().getEncoded());
				System.out.printf("Public Key: %s\nAddresses:\n", new String(publicKey, StandardCharsets.UTF_8));
				Map<Network, Address> addresses = wallet.getAddresses();
				for(Entry<Network, Address> entry : addresses.entrySet()) {
					System.out.printf("Network: %s, Address: %s\n", entry.getKey(), entry.getValue());
				}
				System.exit(0);
			} else {
				wallet = new Wallet(new MapDBWalletStore());
			}
			System.out.printf("For %s, your address is %s and your last saved balance is %s\n", network, wallet.getAddress(network), wallet.getBalance(network));
			
			Socket clientSocket = new Socket();
			clientSocket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT);
			ois = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));
			oos = new ObjectOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
			while(true) {
				Object read = null;
				try {
					read = ois.readObject();
				} catch(ClassNotFoundException e) {
					System.err.println(e);
					System.exit(-1);
				}
				
				if(!validMessage((Message) read)) {
					System.err.println("invalid message");
				} else if(read instanceof ExitMessage) {
					System.out.println("Goodbye!");
					break;
				} else if(read instanceof PingMessage) {
					PingMessage pingMessage = (PingMessage) read;
					System.out.println(pingMessage);
				} else if(read instanceof DifficultyMessage) {
					DifficultyMessage difficultyMessage = (DifficultyMessage) read;
					System.out.println(difficultyMessage);
				} else if(read instanceof WelcomeMessage) {
					WelcomeMessage welcomeMessage = (WelcomeMessage) read;
					System.out.println(welcomeMessage);
					sendMessage(new UtxoRequestMessage(network, wallet.getAddress(network)));
				} else if(read instanceof TransactionMessage) {
					TransactionMessage transactionMessage = (TransactionMessage) read;
					System.out.println(transactionMessage);
					//TODO: we could verify that our transaction was received
				} else if(read instanceof BlockMessage) {
					BlockMessage blockMessage = (BlockMessage) read;
					System.out.println(blockMessage);
					//TODO: maybe this is where we find our transaction embedded in a block
				} else if(read instanceof PartialChainMessage) {
					PartialChainMessage partialChainMessage = (PartialChainMessage) read;
					System.out.println(partialChainMessage);
				} else if(read instanceof PartialChainRequestMessage) {
					PartialChainRequestMessage partialChainRequestMessage = (PartialChainRequestMessage) read;
					System.out.println(partialChainRequestMessage);
				} else if(read instanceof UtxoMessage) {
					UtxoMessage utxoMessage = (UtxoMessage) read;
					System.out.println(utxoMessage);
					List<UTXO> filtered = utxoMessage.utxos.stream().filter(utxo -> utxo.getAddress().equals(wallet.getAddress(network))).collect(Collectors.toList());
					wallet.addUtxos(filtered);
					System.out.printf("Balance has been updated for %s to %s\n", network, wallet.getBalance(network));
				} else if(read instanceof UtxoRequestMessage) {
					UtxoRequestMessage utxoRequestMessage = (UtxoRequestMessage) read;
					System.out.println(utxoRequestMessage);
				}
			}
			ois.close();
			clientSocket.close();
		} catch(Exception e) {
			System.err.println(e);
			System.exit(-1);
		}
	}
	
	private void sendMessage(Object message) throws IOException {
		oos.writeObject(message);
		oos.flush();
	}
	
	private boolean validMessage(Message message) {
		if(message == null) {
			return false;
		} else if(message.getNetwork() == network) {
			return true;
		} else {
			return false;
		}
	}
}