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

import io.tokhn.node.Network;
import io.tokhn.util.Hash;

public class UTXO implements Serializable {
	private static final long serialVersionUID = -411556459302981328L;
	private final Network network;
	private final Hash utxoId;
	private final Hash sourceTxId;
	private final int sourceTxoIndex;
	private final Address address;
	private final Token amount;
	private final String script;
	
	public UTXO(Network network, TXO txo, TXI txi) {
		this(network, txi.getSourceTxId(), txi.getSourceTxoIndex(), txo.getAddress(), txo.getAmount(), txo.getScript());
	}
	
	public UTXO(Network network, Hash sourceTxId, int sourceTxoIndex, Address address, Token amount) {
		this(network, sourceTxId, sourceTxoIndex, address, amount, "");
	}
	
	public UTXO(Network network, Hash sourceTxId, int sourceTxoIndex, Address address, Token amount, String script) {
		this.network = network;
		this.utxoId = hash(network, sourceTxId, sourceTxoIndex);
		this.sourceTxId = sourceTxId;
		this.sourceTxoIndex = sourceTxoIndex;
		this.address = address;
		this.amount = amount;
		this.script = script;
	}
	
	public static Hash hash(Network network, TXI txi) {
		return hash(network, txi.getSourceTxId(), txi.getSourceTxoIndex());
	}
	
	public static Hash hash(Network network, Hash txoId, int txoIndex) {
		String toHash = network + txoId.toString() + txoIndex;
		return Hash.of(toHash);
	}
	
	public String toString() {
		return String.format("%s (%s [%d] %s:%s)\n", utxoId, sourceTxId, sourceTxoIndex, address, amount);
	}
	
	public Network getNetwork() {
		return network;
	}
	
	public Hash getUtxoId() {
		return utxoId;
	}
	
	public Hash getSourceTxoId() {
		return sourceTxId;
	}

	public int getSourceTxoIndex() {
		return sourceTxoIndex;
	}

	public Address getAddress() {
		return address;
	}

	public Token getAmount() {
		return amount;
	}
	
	public String getScript() {
		return script;
	}
}