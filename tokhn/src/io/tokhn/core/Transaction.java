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

import io.tokhn.util.Hash;

public class Transaction implements Comparable<Transaction>, Serializable {
	private static final long serialVersionUID = -8510962834025629565L;
	private final Hash id;
	private final long timestamp;
	private final Type type;
	private final List<TXI> txis;
	private final List<TXO> txos;
	
	public Transaction(long timestamp, List<TXI> txis, List<TXO> txos) {
		if(txis.isEmpty() && txos.size() == 1) {
			this.type = Type.REWARD;
		} else if(txis.stream().anyMatch(txi -> !txi.getScript().isEmpty()) || txos.stream().anyMatch(txo -> !txo.getScript().isEmpty())) {
			this.type = Type.FEE;
		} else {
			this.type = Type.REGULAR;
		}
		this.timestamp = timestamp;
		this.txis = txis;
		this.txos = txos;
		id = hash(timestamp, type, txis, txos);
	}
	
	public Transaction(Hash id, long timestamp, Type type, List<TXI> txis, List<TXO> txos) {
		this.id = id;
		this.timestamp = timestamp;
		this.type = type;
		this.txis = txis;
		this.txos = txos;
	}
	
	public static Hash hash(long timestamp, Type type, List<TXI> txis, List<TXO> txos) {
		/*
		 * create a stream of transactionIns
		 * map a transactionIn to a concatenation of outId, outIndex, and script
		 * reduce to a concatenation of the above
		 */
		String ins = txis.stream().map(txi -> {
			switch(type) {
				case FEE:
				case REGULAR:
					return txi.getSourceTxId().toString() + txi.getSourceTxoIndex() + txi.getScript();
				case REWARD:
				default:
					return "";
			}
		}).reduce("", (a, b) -> a + b);
		/*
		 * create a stream of transactionOuts
		 * map a transactionOut to a concatenation of address, amount, and script
		 * reduce to a concatenation of the above
		 */
		String outs = txos.stream().map(txo -> txo.getAddress().toString() + txo.getAmount().getValue() + txo.getScript()).reduce("", (a, b) -> a + b);
		
		String toHash = timestamp + ins + outs;
		return Hash.of(toHash);
	}
	
	public static Transaction rewardOf(Address address, int ones) {
		return rewardOf(address, Token.valueOfInOnes(ones));
	}
	
	public static Transaction rewardOf(Address address, long megas) {
		return rewardOf(address, Token.valueOfInMegas(megas));
	}
	
	private static Transaction rewardOf(Address address, Token amount) {
		List<TXO> txos = new LinkedList<>();
		txos.add(new TXO(address, amount));
		return new Transaction(Instant.now().getEpochSecond(), new LinkedList<>(), txos);
	}
	
	public List<Address> getAllAddresses() {
		return txos.stream().map(t -> t.getAddress()).distinct().collect(Collectors.toList());
	}

	public Hash getId() {
		return id;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public Type getType() {
		return type;
	}

	public List<TXI> getTxis() {
		return txis;
	}

	public List<TXO> getTxos() {
		return txos;
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

		Transaction other = (Transaction) o;
		if(!id.equals(other.getId())) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(Transaction o) {
		return id.compareTo(o.getId());
	}
	
	@Override
	public String toString() {
		return String.format("%s@%d [%d:%d]", getId(), getTimestamp(), getTxis().size(), getTxos().size());
	}
	
	public enum Type {
		FEE, REGULAR, REWARD
	}
}