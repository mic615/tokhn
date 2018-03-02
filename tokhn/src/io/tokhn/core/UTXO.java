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

import io.tokhn.util.Hash;

public class UTXO implements Serializable {
	private static final long serialVersionUID = -411556459302981328L;
	private final Hash utxoId;
	private final Hash sourceTxoId;
	private final int sourceTxoIndex;
	private final Address address;
	private final Token amount;
	
	public UTXO(TXO txo, TXI txi) {
		this(txi.getSourceTxoId(), txi.getSourceTxoIndex(), txo.getAddress(), txo.getAmount());
	}
	
	public UTXO(Hash sourceTxoId, int sourceTxoIndex, Address address, Token amount) {
		this.utxoId = hash(sourceTxoId, sourceTxoIndex);
		this.sourceTxoId = sourceTxoId;
		this.sourceTxoIndex = sourceTxoIndex;
		this.address = address;
		this.amount = amount;
	}
	
	public static Hash hash(TXI txi) {
		return hash(txi.getSourceTxoId(), txi.getSourceTxoIndex());
	}
	
	public static Hash hash(Hash txoId, int txoIndex) {
		String toHash = txoId.getUTF8String() + txoIndex;
		return Hash.of(toHash);
	}
	
	public String toString() {
		return String.format("%s (%s [%d] %s:%s)\n", utxoId, sourceTxoId, sourceTxoIndex, address, amount);
	}
	
	public Hash getUtxoId() {
		return utxoId;
	}
	
	public Hash getSourceTxoId() {
		return sourceTxoId;
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
}