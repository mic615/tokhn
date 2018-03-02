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
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import io.tokhn.core.Address;

public enum Network implements Serializable {
	TKHN((byte) 0x00), TEST((byte) 0xFF);
	
	private final String CHARITY_PUBLIC_KEY = "MEkwEwYHKoZIzj0CAQYIKoZIzj0DAQEDMgAEnVXogujT/TEPj2c1JbuJxpa6ZZ8YUcBoqLfvn1M13/CNEI9boWJKiSjCLUwlgjz1";
	private final byte id;
	
	private Network(byte id) {
		this.id = id;
	}

	public byte getId() {
		return id;
	}
	
	public Version getVersion() {
		return Version.valueOf(getProperties().getProperty("VERSION"));
	}
	
	public static Set<Network> getAll() {
		return Arrays.stream(values()).collect(Collectors.toSet());
	}
	
	public static Network valueOf(byte id) throws InvalidNetworkException {
		for(Network n : Network.values()) {
			if(n.getId() == id) {
				return n;
			}
		}
		throw new InvalidNetworkException();
	}
	
	public Properties getProperties() {
		Properties props = new Properties();
		switch(this) {
			case TKHN:
				props.setProperty("VERSION", "ZERO");
				return props;
			case TEST:
				props.setProperty("VERSION", "ZERO");
				return props;
		}
		return null;
	}
	
	public Address getCharityAddress() {
		Address address = null;
		try {
			KeyFactory fact = KeyFactory.getInstance("ECDSA", "BC");
			byte[] publicKey = Base64.getDecoder().decode(CHARITY_PUBLIC_KEY.getBytes(StandardCharsets.UTF_8));
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKey);
			address = new Address(fact.generatePublic(publicKeySpec), this);
		} catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
			e.printStackTrace();
		}
		
		return address;
	}
	
	public Peer[] getSeedPeers() {
		switch(this) {
			case TKHN:
				Peer[] phePeers = {new Peer("127.0.0.1", 1337)};
				return phePeers;
			case TEST:
				Peer[] testPeers = {new Peer("127.0.0.1", 1337)};
				return testPeers;
		}
		return null;
	}
}