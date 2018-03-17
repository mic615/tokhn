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

package io.tokhn.node.network;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.tokhn.core.Block;
import io.tokhn.core.TXO;
import io.tokhn.core.Token;
import io.tokhn.core.Transaction;
import io.tokhn.node.Network;
import io.tokhn.node.NetworkParams;
import io.tokhn.util.Hash;

public class Test implements NetworkParams {
	
	public Block getGenesisBlock() {
		long genesisTime = 1514764800; //this is Unix time
		ArrayList<Transaction> transactions = new ArrayList<>();
		List<TXO> txos = new LinkedList<>();
		txos.add(new TXO(Network.TEST.getCharityAddress(), Token.ONE));
		transactions.add(new Transaction(genesisTime, new LinkedList<>(), txos));
		return new Block(Network.TEST, 0, Hash.EMPTY_HASH, genesisTime, transactions, 1, 0);
	}
	
	public int getBlockGenerationInterval() {
		return 1; //this is in seconds
	}
	
	public int getDifficultyAdjustmentInterval() {
		return 1000; //this is in increments
	}
}