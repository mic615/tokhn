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

public class LUV implements NetworkParams {
	
	public Block getGenesisBlock() {
		Address LuvStorage = null;
		try {// 
			LuvStorage = new Address("zBVoei18ao3xwoBRuRbmhWNWZm6vprK4N");//"1LZVY6JK9xxvJ3eAQAaPGfVYVP4ZgxB2Pv");
		} catch(InvalidNetworkException e) {
			System.err.println(e);
		}
		long genesisTime = 1521555882;
		List<Transaction> transactions = new LinkedList<>();
		List<TXO> txos = new LinkedList<>();
		txos.add(new TXO(LuvStorage, Token.valueOfInOnes(100000)));
		transactions.add(new Transaction(genesisTime, new LinkedList<>(), txos));
		return new Block(Network.LUV, 0, Hash.EMPTY_HASH, genesisTime, transactions, 1, 0);
	}
	
	@Override
	public int getBlockGenerationInterval() {
		return 900;
	}
	
	@Override
	public int getDifficultyAdjustmentInterval() {
		return 50;
	}
	
//	@Override
//	public String getHost() {
//		return "node.luv.ag";
//	}
}