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

package io.tokhn.core;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.bitcoinj.core.Base58;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;

import io.tokhn.node.InvalidNetworkException;
import io.tokhn.node.Network;
import io.tokhn.util.Hash;

public class Address implements Serializable {
	private static final long serialVersionUID = 4952348156124651935L;
	private final byte[] bytes;
	private final Network network;
	
	public Address(PublicKey publicKey, Network network) {
		this.network = network;
		/*
		 * 		Byte
		 * Start	:End	:Description
		 * 	0	: 0	:	Network ID
		 * 	1	: 20	:	RIPEMD160 of publicKey (keyHash)
		 * 	21	: 24	:	SHA3-256 of keyHash (checksum)
		 */
		bytes = new byte[25];
		
		byte[] publicKeyBytes = publicKey.getEncoded();
		Hash publicKeyHash = Hash.of(publicKeyBytes);
		RIPEMD160Digest digest = new RIPEMD160Digest();
		digest.update(publicKeyHash.getBytes(), 0, publicKeyHash.getBytes().length);
		byte[] ripemdHash = new byte[21];
		//put in the network id at byte 0
		ripemdHash[0] = network.getId();
		//start at byte 1
		digest.doFinal(ripemdHash, 1);
		//put in the keyHash starting at byte 0 (includes network byte)
		IntStream.rangeClosed(0, 20).forEach(i -> bytes[i] = ripemdHash[i]);
		
		Hash checksum = Hash.of(ripemdHash);
		//put in the checksum starting at byte 21
		IntStream.rangeClosed(21, 24).forEach(i -> bytes[i] = checksum.getBytes()[i - 21]);
	}
	
	public Address(String base58Address) throws InvalidNetworkException {
		this(Base58.decodeChecked(base58Address));
	}
	
	public Address(byte[] bytes) throws InvalidNetworkException {
		this.bytes = bytes;
		network = Network.valueOf(bytes[0]);
	}
	
	public String toString() {
		return Base58.encode(bytes);
	}

	public byte[] getBytes() {
		return bytes;
	}
	
	public Network getNetwork() {
		return network;
	}

	public String getUTF8String() {
		return new String(bytes, StandardCharsets.UTF_8);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bytes);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Address other = (Address) obj;
		if (!Arrays.equals(bytes, other.bytes))
			return false;
		return true;
	}
}