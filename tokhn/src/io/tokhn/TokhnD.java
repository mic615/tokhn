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

import java.io.IOException;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.tokhn.core.Blockchain;
import io.tokhn.node.Network;
import io.tokhn.node.TokhnServiceImpl;
import io.tokhn.store.MapDBBlockStore;
import io.tokhn.store.MapDBUTXOStore;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "Tokhn Daemon", version = { "Tokhn 0.0.1", "(c) 2018 Matt Liotta" }, showDefaultValues = true)
public class TokhnD extends Thread {
	private final static Map<Network, Blockchain> chains = new HashMap<>();
	
	@Option(names = { "-h", "--help" }, usageHelp = true, description = "displays this help message and exit")
	private boolean helpRequested = false;
	
	@Option(names = { "-P", "--port" }, required = false, description = "the remote port")
	private int PORT = 1337;

	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		System.out.println("Daemon running...");
		CommandLine.run(new TokhnD(), System.out, args);
	}
	
	@Override
	public void run() {
		Set<Network> networks = Network.getAll();
		for(Network network: networks) {
			chains.put(network, new Blockchain(network, new MapDBBlockStore(network), new MapDBUTXOStore(network)));
		}
		Server server = ServerBuilder.forPort(PORT).addService(new TokhnServiceImpl(chains)).build();

		try {
			server.start();
			server.awaitTermination();
		} catch (IOException e) {
			System.err.println(e);
		} catch (InterruptedException e) {
			System.err.println(e);
		}
	}
}