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
import java.util.Set;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.tokhn.node.Network;
import io.tokhn.node.TokhnServiceImpl;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "Tokhn Daemon", version = { "Tokhn 0.0.1", "(c) 2018 Matt Liotta" }, showDefaultValues = true)
public class TokhnD extends Thread {
	@Option(names = { "-h", "--help" }, usageHelp = true, description = "displays this help message and exit")
	private boolean helpRequested = false;
	
	@Option(names = { "-P", "--port" }, required = false, description = "the remote port")
	private int PORT = 1337;
	
	@Option(names = { "-n", "--network" }, required = false, description = "the list of networks to support")
	Set<Network> NETWORKS = Network.getAll();

	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		CommandLine.run(new TokhnD(), System.out, args);
	}
	
	@Override
	public void run() {
		String version = getClass().getPackage().getImplementationVersion();
		if(version != null) {
			System.out.println("Daemon [" + version + "] running...");
		} else {
			System.out.println("Daemon running...");
		}
		Server server = ServerBuilder.forPort(PORT).addService(new TokhnServiceImpl(NETWORKS)).build();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				server.shutdownNow();
			}
		});
		
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