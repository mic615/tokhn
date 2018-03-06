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
import java.util.List;
import java.util.stream.LongStream;

import io.tokhn.node.Network;
import io.tokhn.node.Version;
import io.tokhn.util.Hash;

public class Block implements Comparable<Block>, Serializable {
	private static final long serialVersionUID = -3502917216334358464L;
	private final Network network;
	private final Version version;
	private final int index;
	private final Hash hash;
	private final Hash previousHash;
	private final long timestamp;
	private final List<Transaction> transactions;
	private final int difficulty;
	private final long nonce; //bigger makes it better
	
	public Block(Network network, Version version, int index, Hash previousHash, long timestamp, List<Transaction> transactions, int difficulty, long nonce) {
		this(network, version, index, hash(index, previousHash, timestamp, transactions, difficulty, nonce), previousHash, timestamp, transactions, difficulty, nonce);
	}
	
	public Block(Network network, Version version, int index, Hash hash, Hash previousHash, long timestamp, List<Transaction> transactions, int difficulty, long nonce) {
		this.network = network;
		this.version = version;
		this.index = index;
		this.hash = hash;
		this.previousHash = previousHash;
		this.timestamp = timestamp;
		this.transactions = transactions;
		this.difficulty = difficulty;
		this.nonce = nonce;
	}
	
	public static Block findBlock(Network network, Version version, int index, Hash previousHash, List<Transaction> transactions, int difficulty) {
		Block result = LongStream.iterate(0, i -> i + 1).parallel()
				.mapToObj(i -> new Block(network, version, index, previousHash, Instant.now().getEpochSecond(), transactions, difficulty, i))
				.filter(block -> hashMatchesDifficulty(block.getHash(), difficulty)).findAny().orElse(null);
		
		return result;
	}
	
	public static boolean hashMatchesDifficulty(Hash hash, int difficulty) {
		if(hash.numberOfLeadingZeros() >= difficulty) {
			return true;
		} else {
			return false;
		}
	}
	
	public static Hash hash(int index, Hash previousHash, long timestamp, List<Transaction> transactions, int difficulty, long nonce) {
		String toHash = index + previousHash.toString() + timestamp + hashTransactions(transactions).toString() + difficulty + nonce;
		return Hash.of(toHash);
	}
	
	private static Hash hashTransactions(List<Transaction> transactions) {
		/*
		 * create a stream of transactions
		 * map a transaction id to its UTF8 string format
		 * reduce to a concatenation of transaction ids
		 */
		String TXs = transactions.stream().map(t -> t.getId().toString()).reduce("", (a, b) -> a + b);
		return Hash.of(TXs);
	}
	
	public String toString() {
		return String.format("Block[%d:%s] (%s)", index, timestamp, hash);
	}

	public Network getNetwork() {
		return network;
	}

	public Version getVersion() {
		return version;
	}

	public int getIndex() {
		return index;
	}

	public Hash getHash() {
		return hash;
	}

	public Hash getPreviousHash() {
		return previousHash;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public List<Transaction> getTransactions() {
		return transactions;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public long getNonce() {
		return nonce;
	}

	@Override
	public int compareTo(Block o) {
		return Integer.compare(this.getIndex(), o.getIndex());
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
		
		Block other = (Block) o;
		return this.getHash().equals(other.getHash());
	}
	
	public static Block max(Block a, Block b) {
		if(a.getIndex() >= b.getIndex()) {
			return a;
		} else {
			return b;
		}
	}
}