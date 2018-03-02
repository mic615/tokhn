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
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import io.tokhn.node.Version;
import io.tokhn.util.Hash;

public class Transaction implements Serializable {
	private static final long serialVersionUID = -8510962834025629565L;
	private final Version version;
	private final Hash id;
	private final long timestamp;
	private final List<TXI> txis;
	private final List<TXO> txos;
	
	public Transaction(Version version, long timestamp, List<TXI> txis, List<TXO> txos) {
		this.version = version;
		this.timestamp = timestamp;
		this.txis = txis;
		this.txos = txos;
		id = hash(timestamp, txis, txos);
	}
	
	public static Hash hash(long timestamp, List<TXI> txis, List<TXO> txos) {
		/*
		 * create a stream of transactionIns
		 * map a transactionIn to a concatenation of outId and outIndex
		 * reduce to a concatenation of the above
		 */
		String ins = txis.stream().map(t -> t.getSourceTxoId().toString() + t.getSourceTxoIndex()).reduce("", (a, b) -> a + b);
		/*
		 * create a stream of transactionOuts
		 * map a transactionOut to a concatenation of address and amount
		 * reduce to a concatenation of the above
		 */
		String outs = txos.stream().map(t -> t.getAddress().toString() + t.getAmount()).reduce("", (a, b) -> a + b);
		
		String toHash = timestamp + ins + outs;
		return Hash.of(toHash);
	}
	
	public static Transaction rewardOf(Version version, Address address, int ones) {
		return rewardOf(version, address, Token.valueOfInOnes(ones));
	}
	
	public static Transaction rewardOf(Version version, Address address, long megas) {
		return rewardOf(version, address, Token.valueOfInMegas(megas));
	}
	
	private static Transaction rewardOf(Version version, Address address, Token amount) {
		List<TXO> txos = new LinkedList<>();
		txos.add(new TXO(address, amount));
		return new Transaction(version, Instant.now().getEpochSecond(), new LinkedList<>(), txos);
	}
	
	public List<Address> getAllAddresses() {
		return txos.stream().map(t -> t.getAddress()).distinct().collect(Collectors.toList());
	}

	public Version getVersion() {
		return version;
	}

	public Hash getId() {
		return id;
	}
	
	public long getTimestamp() {
		return timestamp;
	}

	public List<TXI> getTxis() {
		return txis;
	}

	public List<TXO> getTxos() {
		return txos;
	}
}