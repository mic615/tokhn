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

package io.tokhn.node;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Peer implements Serializable {
	private static final long serialVersionUID = 5689132905549792119L;
	public String host;
	public int port;
	
	public Peer(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	public InetAddress getInetAddress() {
		try {
			return InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			System.err.println(e);
		}
		return null;
	}
}