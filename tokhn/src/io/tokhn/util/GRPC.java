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

package io.tokhn.util;

import java.util.List;
import java.util.stream.Collectors;

import com.google.protobuf.ByteString;

import io.tokhn.core.Address;
import io.tokhn.core.Block;
import io.tokhn.core.TXI;
import io.tokhn.core.TXO;
import io.tokhn.core.Token;
import io.tokhn.core.Transaction;
import io.tokhn.core.UTXO;
import io.tokhn.grpc.BlockModel;
import io.tokhn.grpc.NetworkModel;
import io.tokhn.grpc.TransactionModel;
import io.tokhn.grpc.TxiModel;
import io.tokhn.grpc.TxoModel;
import io.tokhn.grpc.UtxoModel;
import io.tokhn.node.InvalidNetworkException;
import io.tokhn.node.Network;

public class GRPC {
	
	public static Block transform(BlockModel block) {
		return new Block(Network.valueOf(block.getNetwork().name()), block.getIndex(), new Hash(block.getHash()), new Hash(block.getPreviousHash()), block.getTimestamp(), block.getTransactionsList().stream().map(tx -> transform(tx)).collect(Collectors.toList()), block.getDifficulty(), block.getNonce());
	}
	
	public static BlockModel transform(Block block) {
		return BlockModel.newBuilder()
				.setNetwork(NetworkModel.valueOf(block.getNetwork().name()))
				.setIndex(block.getIndex())
				.setHash(block.getHash().toString())
				.setPreviousHash(block.getPreviousHash().toString())
				.setTimestamp(block.getTimestamp())
				.addAllTransactions(transform(block.getNetwork(), block.getTransactions()))
				.setDifficulty(block.getDifficulty())
				.setNonce(block.getNonce())
				.build();
	}
	
	public static Transaction transform(TransactionModel tx) {
		return new Transaction(new Hash(tx.getId()), tx.getTimestamp(), Transaction.Type.valueOf(tx.getType().name()), tx.getTxisList().stream().map(txi -> transform(txi)).collect(Collectors.toList()), tx.getTxosList().stream().map(txo -> transform(txo)).collect(Collectors.toList()));
	}
	
	public static TransactionModel transform(Network network, Transaction tx) {
		return TransactionModel.newBuilder()
				.setNetwork(NetworkModel.valueOf(network.name()))
				.setId(tx.getId().toString())
				.setTimestamp(tx.getTimestamp())
				.setType(TransactionModel.Type.valueOf(tx.getType().name()))
				.addAllTxis(tx.getTxis().stream().map(txi -> transform(txi)).collect(Collectors.toList()))
				.addAllTxos(tx.getTxos().stream().map(txo -> transform(txo)).collect(Collectors.toList()))
				.build();
	}
	
	public static TXI transform(TxiModel txi) {
		return new TXI(new Hash(txi.getSourceTxId()), txi.getSourceTxoIndex(), txi.getScript(), txi.getSignature().toByteArray());
	}
	
	public static TxiModel transform(TXI txi) {
		return TxiModel.newBuilder()
				.setSourceTxId(txi.getSourceTxId().toString())
				.setSourceTxoIndex(txi.getSourceTxoIndex())
				.setScript(txi.getScript())
				.setSignature(ByteString.copyFrom(txi.getSignature()))
				.build();
	}
	
	public static TXO transform(TxoModel txo) {
		Address address = null;
		
		try {
			address = new Address(txo.getAddress());
			return new TXO(address, Token.valueOfInMegas(txo.getAmount()), txo.getScript());
		} catch (InvalidNetworkException e) {
			System.err.println(e);
		}
		
		return null;
	}
	
	public static TxoModel transform(TXO txo) {
		return TxoModel.newBuilder()
				.setAddress(txo.getAddress().toString())
				.setAmount(txo.getAmount().getValue())
				.setScript(txo.getScript())
				.build();
	}
	
	public static UtxoModel transform(UTXO utxo) {
		return UtxoModel.newBuilder()
				.setNetwork(NetworkModel.valueOf(utxo.getNetwork().name()))
				.setUtxoId(utxo.getUtxoId().toString())
				.setSourceTxId(utxo.getSourceTxoId().toString())
				.setSourceTxoIndex(utxo.getSourceTxoIndex())
				.setAddress(utxo.getAddress().toString())
				.setAmount(utxo.getAmount().getValue())
				.setScript(utxo.getScript())
				.build();
	}
	
	public static List<TransactionModel> transform(Network network, List<Transaction> txs) {
		return txs.stream().map(tx -> transform(network, tx)).collect(Collectors.toList());
	}
}