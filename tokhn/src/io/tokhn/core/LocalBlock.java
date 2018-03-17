package io.tokhn.core;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import io.tokhn.util.Hash;

public class LocalBlock extends Block{
	private static final long serialVersionUID = -852427239298408605L;
	private final BigInteger aggregatedDifficulty;
	private final Set<Address> aggregatedUniqueAddresses;
	
	public LocalBlock(Block block, Blockchain chain) {
		super(block.getNetwork(), block.getIndex(), block.getHash(), block.getPreviousHash(), block.getTimestamp(), block.getTransactions(), block.getDifficulty(), block.getNonce());
		aggregatedDifficulty = getAggregateDifficulty(block, chain);
		aggregatedUniqueAddresses = new HashSet<>();
		updateAggregatedUniqueAddresses(chain);
	}

	public BigInteger getAggregatedDifficulty() {
		return aggregatedDifficulty;
	}
	
	public static LocalBlock max(LocalBlock a, LocalBlock b) {
		if(a.getIndex() >= b.getIndex()) {
			return a;
		} else {
			return b;
		}
	}
	
	private BigInteger getAggregateDifficulty(Block block, Blockchain chain) {
		if(block.getPreviousHash().equals(Hash.EMPTY_HASH)) {
			return BigInteger.valueOf(2).pow(block.getDifficulty());
		} else {
			LocalBlock b = chain.getBlock(block.getPreviousHash());
			return BigInteger.valueOf(2).pow(block.getDifficulty()).add(b.aggregatedDifficulty);
		}
	}
	
	private void updateAggregatedUniqueAddresses(Blockchain chain) {
		Block b = this;
		while(!b.getPreviousHash().equals(Hash.EMPTY_HASH)) {
			if(b != null) {
				b.getTransactions().stream().flatMap(t -> t.getAllAddresses().stream()).forEach(a -> aggregatedUniqueAddresses.add(a));
			}
			b = chain.getBlock(b.getPreviousHash());
		}
	}
}