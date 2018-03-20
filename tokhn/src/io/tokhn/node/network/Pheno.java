/*
 * Copyright 2018 TriGrow Systems
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

package io.tokhn.node.network;

import java.util.LinkedList;
import java.util.List;

import io.tokhn.core.Address;
import io.tokhn.core.Block;
import io.tokhn.core.TXO;
import io.tokhn.core.Token;
import io.tokhn.core.Transaction;
import io.tokhn.node.InvalidNetworkException;
import io.tokhn.node.Network;
import io.tokhn.node.NetworkParams;
import io.tokhn.util.Hash;

public class Pheno implements NetworkParams {
	
	public Block getGenesisBlock() {
		Address trigrow = null;
		try {
			trigrow = new Address("Z5bwn2gJ384Ec83rmPpmRptQxkb57z1Ua");
		} catch(InvalidNetworkException e) {
			System.err.println(e);
		}
		long genesisTime = 1521555882;
		List<Transaction> transactions = new LinkedList<>();
		List<TXO> txos = new LinkedList<>();
		txos.add(new TXO(trigrow, Token.valueOfInOnes(100000)));
		transactions.add(new Transaction(genesisTime, new LinkedList<>(), txos));
		return new Block(Network.PHNO, 0, Hash.EMPTY_HASH, genesisTime, transactions, 1, 0);
	}
	
	@Override
	public int getBlockGenerationInterval() {
		return 900;
	}
	
	@Override
	public int getDifficultyAdjustmentInterval() {
		return 50;
	}
	
	@Override
	public String getHost() {
		return "node.pheno.ag";
	}
}