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

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.script.Invocable;
import javax.script.ScriptException;

import delight.nashornsandbox.NashornSandbox;
import delight.nashornsandbox.NashornSandboxes;
import delight.nashornsandbox.exceptions.ScriptCPUAbuseException;
import io.tokhn.node.Network;
import io.tokhn.node.Version;
import io.tokhn.store.BlockStore;
import io.tokhn.store.UTXOStore;
import io.tokhn.util.Hash;

public class Blockchain {
	private final Network network;
	private final Version version;
	private final BlockStore bStore;
	private final UTXOStore uStore;
	private LocalBlock genesisBlock = null;
	private LocalBlock latestBlock = null;
	
	public Blockchain(Network network, Version version, BlockStore bStore, UTXOStore uStore) {
		this.network = network;
		this.version = version;
		this.bStore = bStore;
		this.uStore = uStore;
		latestBlock = bStore.getLatestBlock();
		if(latestBlock == null || !isValidChain()) {
			genesisBlock = new LocalBlock(network.getParams().getGenesisBlock(), this);
			storeAndProcess(genesisBlock);
			latestBlock = genesisBlock;
		}
	}
	
	public boolean addBlockToChain(Block block) {
		if(block.getDifficulty() < getDifficulty()) {
			return false;
		} else if(block.getPreviousHash().equals(getLatestBlock().getHash()) && isValidBlock(block, getLatestBlock())) {
			//new latest block
			latestBlock = storeAndProcess(block);
			return true;
		}
		
		LocalBlock prevBlock = getBlock(block.getPreviousHash());		
		if(prevBlock != null) {
			//this must be a branch block
			System.out.println("Branch block found");
			if(isValidBlock(block, prevBlock)) {
				LocalBlock newLatest = storeAndProcess(block);
				if(getLatestBlock().getAggregatedDifficulty().compareTo(newLatest.getAggregatedDifficulty()) == -1) {
					//this is from a better chain
					System.out.println("Branch block is from superior chain");
					try {
						handleChainBranch(newLatest);
					} catch (Exception e) {
						System.err.println(e);
						return false;
					}
					return true;
				} else {
					//our chain is better
					System.err.println("block should be pruned");
					return false;
				}
			} else {
				System.err.println("Invalid block");
				return false;
			}
		} else {	
			if(!Block.hash(block.getIndex(), block.getPreviousHash(), block.getTimestamp(), block.getTransactions(), block.getDifficulty(), block.getNonce()).equals(block.getHash())) {
				System.err.println("Invalid block");
				return false;
			} else {
				//TODO: optimization opportunity to store orphans and process later
				System.out.println("Orphan block found");
				return false;
			}
		}
	}
	
	public int getDifficulty() {
		LocalBlock latestBlock = getLatestBlock();
		if(latestBlock.getIndex() % network.getParams().getDifficultyAdjustmentInterval() == 0 && latestBlock.getIndex() != 0) {
			return getAdjustedDifficulty();
		} else {
			return latestBlock.getDifficulty();
		}
	}
	
	public boolean isValidChain() {
		LocalBlock b = getLatestBlock();
		while(true) {
			b = getBlock(b.getPreviousHash());
			if(b == null) {
				return false;
			} else if(b.getPreviousHash().equals(Hash.EMPTY_HASH) && genesisBlock == null) {
				//we are going to assume that if we don't have a genesisBlock and the previousHash is EMPTY_HASH then we are the genesisBlock
				genesisBlock = b;
				return true;
			} else if(b.equals(genesisBlock)) {
				return true;
			}
		}
	}
	
	public boolean isValidBlock(Block block, Block previousBlock) {
		if(previousBlock.getIndex() + 1 != block.getIndex()) {
			return false;
		} else if(!previousBlock.getHash().equals(block.getPreviousHash())) {
			return false;
		} else if(block.getTimestamp() < previousBlock.getTimestamp() - network.getParams().getValidDrift() || block.getTimestamp() > Instant.now().getEpochSecond() + network.getParams().getValidDrift()) {
			//a new block can't be before the previous block and it shouldn't be from the future either
			return false;
		} else if(!Block.hash(block.getIndex(), previousBlock.getHash(), block.getTimestamp(), block.getTransactions(), block.getDifficulty(), block.getNonce()).equals(block.getHash())) {
			return false;
		} else if(!block.getTransactions().stream().allMatch(tx -> isValidTransaction(tx))) {
			return false;
		}
		
		return true;
	}
	
	public boolean isValidTransaction(Transaction tx) {
		if (!Transaction.hash(tx.getTimestamp(), tx.getType(), tx.getTxis(), tx.getTxos()).equals(tx.getId())) {
			return false;
		} else if(tx.getTxis().size() == 0 && tx.getTxos().size() == 1) {
			//this is a miner reward
			//TODO: verify the correct reward
		} else {
			/*
			 * we want to find all the UTXOs associated with the TXIs, but we might not find
			 * them for our purposes then, we will assume that not finding a UTXOs is going
			 * to be okay because then the amount will be smaller than it should and the
			 * transaction won't verify.
			 */
			Token totalTxiAmounts = tx.getTxis().stream()
					.map(txi -> uStore.get(UTXO.hash(network, txi)))
					.filter(utxo -> utxo != null)
					.map(utxo -> utxo.getAmount())
					.reduce(Token.ZERO, (a, b) -> Token.sum(a, b));
			Token totalTxoAmounts = tx.getTxos().stream()
					.map(txo -> txo.getAmount())
					.reduce(Token.ZERO, (a, b) -> Token.sum(a, b));
	
			if (totalTxoAmounts.compareTo(totalTxiAmounts) == 1) {
				// txoAmounts shouldn't be bigger than txiAmounts, but we will allow for it to be smaller
				return false;
			}
		}
		
		/*
		 * check that TXIs verify and their scripts return true
		 * 
		 * since this one liner is complicated, let me break it down:
		 * for the given transaction, get each TXI
		 * for each TXI, verify its signature and execute its script if it has one
		 */
		if (!tx.getTxis().stream().allMatch(txi -> txi.verify(tx) && executeScript(tx, txi.getScript()))) {
			return false;
		}
		
		/*
		 * check that any TXIs that point to TXOs with scripts are executed and return true
		 * 
		 * since this one liner is complicated, let me break it down:
		 * for the given transaction, get the associated UTXO for each TXI
		 * assuming we find any associated UTXOs, then execute its script if it has one
		 */
		if(!tx.getTxis().stream().map(txi -> uStore.get(UTXO.hash(network, txi))).filter(utxo -> utxo != null).allMatch(utxo -> executeScript(tx, utxo.getScript()))) {
			return false;
		}

		return true;
	}
	
	public int getReward() {
		Set<Address> addresses = new HashSet<>();
		LocalBlock b = getLatestBlock();
		while(true) {
			if(b == null) {
				//TODO: is it possible for b to be null?
			}
			b.getTransactions().stream().flatMap(t -> t.getAllAddresses().stream()).forEach(a -> addresses.add(a));
			if(b.equals(genesisBlock)) {
				int reward = (int) Math.log10(addresses.size());
				return reward > 0 ? reward : 1;
			}
			b = getBlock(b.getPreviousHash());
		}
	}
	
	public UTXO getUtxo(Hash utxoId) {
		return uStore.get(utxoId);
	}
	
	public List<UTXO> getUtxosForAddress(Address address) {
		return uStore.getUtxosForAddress(address);
	}
	
	public LocalBlock getBlock(Hash hash) {
		return bStore.get(hash);
	}
	
	public LocalBlock getGenesisBlock() {
		return genesisBlock;
	}
	
	public LocalBlock getLatestBlock() {
		return latestBlock;
	}
	
	public int getLength() {
		return latestBlock.getIndex();
	}
	
	public Network getNetwork() {
		return network;
	}

	public Version getVersion() {
		return version;
	}

	private void processBlockTransactions(LocalBlock block) {
		List<UTXO> consumeUtxos = new LinkedList<>();
		List<UTXO> generateUtxos = new LinkedList<>();
		List<UTXO> rewardUtxos = new LinkedList<>();
		
		for(Transaction tx : block.getTransactions()) {
			if(tx.getTxis().size() == 0 && tx.getTxos().size() == 1) {
				//this is a miner reward
				TXO txo = tx.getTxos().get(0);
				UTXO utxo = new UTXO(network, tx.getId(), 0, txo.getAddress(), txo.getAmount(), null);
				rewardUtxos.add(utxo);
			} else {
				for(TXI txi : tx.getTxis()) {
					UTXO utxo = uStore.get(UTXO.hash(network, txi.getSourceTxId(), txi.getSourceTxoIndex()));
					consumeUtxos.add(utxo);
				}
				for(int itr = 0; itr< tx.getTxos().size(); itr++) {
					TXO txo = tx.getTxos().get(itr);
					UTXO utxo = new UTXO(network, tx.getId(), itr, txo.getAddress(), txo.getAmount(), txo.getScript());
					generateUtxos.add(utxo);
				}
			}
		}
		Token consumed = consumeUtxos.stream().map(utxo -> utxo.getAmount()).reduce(Token.ZERO, (a, b) -> Token.sum(a, b));
		Token generated = generateUtxos.stream().map(utxo -> utxo.getAmount()).reduce(Token.ZERO, (a, b) -> Token.sum(a, b));
		long netMegas = consumed.getValue() - generated.getValue();
		if(netMegas < 0) {
			//this is not supposed to happen since the transactions should have been validated first
			System.err.println("Invalid transaction set");
		} else {
			if(netMegas > 0) {
				//give the left over money to charity
				Transaction charity = Transaction.rewardOf(version, network.getCharityAddress(), netMegas);
				TXO txo = charity.getTxos().get(0);
				block.getTransactions().add(charity);
				rewardUtxos.add(new UTXO(network, charity.getId(), 0, txo.getAddress(), txo.getAmount(), null));
			}
			//this is what is supposed to happen
			consumeUtxos.forEach(utxo -> uStore.remove(utxo.getUtxoId()));
			generateUtxos.forEach(utxo -> uStore.put(utxo));
			rewardUtxos.forEach(utxo -> uStore.put(utxo));
		}
	}
	
	private void revokeBlockTransactions(LocalBlock block) {
		//the theory is to remove any existing UTXOs associated with this block
		for(Transaction tx : block.getTransactions()) {
			for(TXI txi : tx.getTxis()) {
				uStore.remove(UTXO.hash(network, txi.getSourceTxId(), txi.getSourceTxoIndex()));
			}
		}
	}
	
	private LocalBlock storeAndProcess(Block block) {
		LocalBlock lb = new LocalBlock(block, this);
		bStore.put(lb);
		processBlockTransactions(lb);
		return lb;
	}

	private int getAdjustedDifficulty() {
		LocalBlock prevAdjustmentBlock = null;
		for(int itr = 1; itr != network.getParams().getDifficultyAdjustmentInterval(); itr++) {
			prevAdjustmentBlock = getBlock(getLatestBlock().getPreviousHash());
		}
		int timeExpected = network.getParams().getBlockGenerationInterval() * network.getParams().getDifficultyAdjustmentInterval();
		long timeTaken = getLatestBlock().getTimestamp() - prevAdjustmentBlock.getTimestamp();
		
		if(timeTaken < timeExpected / 2) {
			return prevAdjustmentBlock.getDifficulty() + 1;
		} else if(timeTaken > timeExpected * 2) {
			return prevAdjustmentBlock.getDifficulty() - 1;
		} else {
			return prevAdjustmentBlock.getDifficulty();
		}
	}
	
	private void handleChainBranch(LocalBlock newLatest) throws Exception {
		LocalBlock branchPoint = findBranch(newLatest);
		List<LocalBlock> oldBlocks = getPartialChain(getLatestBlock(), branchPoint);
		List<LocalBlock> newBlocks = getPartialChain(newLatest, branchPoint);
		
		for(LocalBlock b : oldBlocks) {
			revokeBlockTransactions(b);
		}
		for(LocalBlock b : newBlocks) {
			processBlockTransactions(b);
		}
		
		latestBlock = newLatest;
	}
	
	private LocalBlock findBranch(LocalBlock newLatest) throws Exception {
		LocalBlock currentB = getLatestBlock();
		LocalBlock newB = newLatest;
		
		/*
		 * Given two different latest blocks, follow them back until they have a common block
		 * 
		 * Consider the following:
		 *    A(1) -> B(2) -> C(3) -> D(4)
		 *            \--> E(3) -> F(4) -> G(5)
		 * 
		 * findBranch will return block B(2), assuming latest started at D(4) and newLatest started at G(5)
		 */
		
		while(!currentB.equals(newB)) {
			if(currentB.getIndex() > newB.getIndex()) {
				currentB = getBlock(currentB.getPreviousHash());
				if(currentB == null) {
					throw new Exception("invalid chain");
				}
			} else {
				newB = getBlock(newLatest.getPreviousHash());
				if(newB == null) {
					throw new Exception("invalid chain");
				}
			}
		}
		
		return currentB;
	}
	
	private List<LocalBlock> getPartialChain(LocalBlock highest, LocalBlock lowest) throws Exception {
		List<LocalBlock> blocks = new LinkedList<>();
		LocalBlock b = highest;
		while(true) {
			blocks.add(b);
			b = getBlock(b.getPreviousHash());
			if(b == null) {
				throw new Exception("invalid chain");
			} else if(b.equals(lowest)) {
				break;
			}
		}
		return blocks;
	}
	
	private boolean executeScript(Transaction tx, String script) {
		if(script != null) {
			ExecutorService executor = Executors.newSingleThreadExecutor();
			NashornSandbox sandbox = NashornSandboxes.create();
			sandbox.setMaxCPUTime(network.getParams().getMaxCpuTime());
			sandbox.setMaxMemory(network.getParams().getMaxMemory());
			sandbox.setMaxPreparedStatements(network.getParams().getMaxPreparedStatements());
			sandbox.setExecutor(executor);
			try {
				sandbox.eval(script);
				Invocable invocable = sandbox.getSandboxedInvocable();
				Object result = invocable.invokeFunction("handleTx", tx);
				executor.shutdown();
				return (boolean) result;
			} catch (ScriptCPUAbuseException | ScriptException | NoSuchMethodException e) {
				System.err.println(e);
				return false;
			}
		} else {
			return true;
		}
	}
}