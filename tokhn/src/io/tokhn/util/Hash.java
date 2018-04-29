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

package io.tokhn.util;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

public class Hash implements Comparable<Hash>, Serializable {
	public static final Hash EMPTY_HASH = new Hash("0000000000000000000000000000000000000000000000000000000000000000");
	private static final long serialVersionUID = 9107863736213087765L;
	private final byte[] bytes;

	public Hash(byte[] bytes) {
		this(bytes, 0, bytes.length);
	}

	public Hash(byte[] bytes, int offset, int length) {
		this.bytes = Arrays.copyOfRange(bytes, offset, length);
	}

	public Hash(String hexEncoded) {
		bytes = Hex.decode(hexEncoded);
	}

	public static Hash of(String utf8Data) {
		return of(utf8Data, StandardCharsets.UTF_8);
	}
	
	public static Hash of(String data, Charset charset) {
		return of(data.getBytes(charset));
	}

	public static Hash of(byte[] byteData) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-512/256", "BC");
			return new Hash(digest.digest(byteData));
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		return null;
	}

	public int numberOfLeadingZeros() {
		int result = 0;
		int temp = 0;
		for (int itr = 0; itr < bytes.length; itr++) {

			temp = numberOfLeadingZeros(bytes[itr]);

			result += temp;
			if (temp != 8) {
				break;
			}
		}

		return result;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public String toString() {
		return Hex.toHexString(bytes);
	}

	private int numberOfLeadingZeros(byte value) {
		if (value < 0)
			return 0;
		if (value < 1)
			return 8;
		else if (value < 2)
			return 7;
		else if (value < 4)
			return 6;
		else if (value < 8)
			return 5;
		else if (value < 16)
			return 4;
		else if (value < 32)
			return 3;
		else if (value < 64)
			return 2;
		else if (value < 128)
			return 1;
		else
			return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(getBytes());
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o == null) {
			return false;
		} else if (getClass() != o.getClass()) {
			return false;
		}

		Hash other = (Hash) o;
		if(!Arrays.areEqual(this.getBytes(), other.getBytes())) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(Hash o) {
		return Arrays.compareUnsigned(this.getBytes(), o.getBytes());
	}
}