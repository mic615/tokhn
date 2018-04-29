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

package io.tokhn.node;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Peer implements Serializable {
	private static final long serialVersionUID = 5689132905549792119L;
	private final String host;
	private final int port;
	private String ipAddress = null;
	
	public Peer(String host, int port) {
		this.host = host;
		this.port = port;
		ipAddress = getIpAddress();
	}
	
	public String getIpAddress() {
		if(ipAddress == null) {
			try {
				ipAddress = InetAddress.getByName(host).getHostAddress();
				return ipAddress;
			} catch(UnknownHostException e) {
				System.err.println(e);
			}
		}
		
		return ipAddress;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
		result = prime * result + port;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Peer other = (Peer) obj;
		if(host == null) {
			if(other.host != null)
				return false;
		} else if(!host.equals(other.host))
			return false;
		if(ipAddress == null) {
			if(other.ipAddress != null)
				return false;
		} else if(!ipAddress.equals(other.ipAddress))
			return false;
		if(port != other.port)
			return false;
		return true;
	}
}