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

package io.tokhn.node.message;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import io.tokhn.node.Message;
import io.tokhn.node.Network;
import io.tokhn.node.Version;

public abstract class AbstractMessage implements Message {
	private static final long serialVersionUID = 2295820372130639338L;
	private final Network network;
	private final Version version;
	private final List<String> relayHosts;
	
	public AbstractMessage(Network network) {
		this(network, network.getVersion());
	}
	
	public AbstractMessage(Network network, Version version) {
		this.network = network;
		this.version = version;
		relayHosts = new LinkedList<>();
	}
	
	public Network getNetwork() {
		return network;
	}
	
	public Version getVersion() {
		return version;
	}
	
	public void addRelayHost(InetAddress addr) {
		addRelayHost(addr.getHostName());
	}
	
	public void addRelayHost(String host) {
		relayHosts.add(host);
	}
	
	public List<String> getRelayHosts() {
		return relayHosts;
	}
	
	public String toString() {
		String relay = relayHosts.stream().reduce("", (a, b) -> a + "->" + b);
		return String.format("%s (NET:%s, VER:%s) [%s]", getClass().getSimpleName(), network, version, relay);
	}
}