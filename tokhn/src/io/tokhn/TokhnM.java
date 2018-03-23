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

package io.tokhn;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.LongStream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.tokhn.core.Block;
import io.tokhn.core.Transaction;
import io.tokhn.core.Wallet;
import io.tokhn.grpc.BlockModel;
import io.tokhn.grpc.TokhnServiceGrpc;
import io.tokhn.grpc.TokhnServiceGrpc.TokhnServiceStub;
import io.tokhn.grpc.TransactionModel;
import io.tokhn.grpc.WelcomeModel;
import io.tokhn.grpc.WelcomeRequest;
import io.tokhn.grpc.WelcomeRequest.PeerType;
import io.tokhn.grpc.WelcomeResponse;
import io.tokhn.node.Network;
import io.tokhn.store.MapDBWalletStore;
import io.tokhn.util.GRPC;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "Tokhn Miner", version = { "Tokhn 0.0.1", "(c) 2018 Matt Liotta" }, showDefaultValues = true)
public class TokhnM extends Thread {
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	private Map<Network, Block> tails = new HashMap<>();
	private Map<Network, Integer> difficulties = new HashMap<>();
	private Map<Network, Integer> rewards = new HashMap<>();
	/*
	 * the below data structure contains all the pending transactions for all
	 * the networks it is going to keep them in the natural of the networks, so
	 * TKHN will be first it could be supplied a different Comperator if a
	 * differen't order is desired
	 */
	private ConcurrentSkipListMap<Network, List<Transaction>> pendingTxs = new ConcurrentSkipListMap<>(new PhenoFirst());
	private Wallet wallet = null;
	private StreamObserver<BlockModel> blockObserver = null;
	private Future<?> mineJob = null;
	
	@Option(names = { "-h", "--help" }, usageHelp = true, description = "displays this help message and exit")
	private boolean helpRequested = false;
	
	@Option(names = { "-H", "--host" }, required = false, description = "the remote host")
	private String HOST = "localhost";
	
	@Option(names = { "-P", "--port" }, required = false, description = "the remote port")
	private int PORT = 1337;
	
	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		CommandLine.run(new TokhnM(), System.out, args);
	}
	
	@Override
	public void run() {
		try {
			wallet = new Wallet(new MapDBWalletStore());
			if(wallet.getPrivateKey() == null || wallet.getPublicKey() == null) {
				System.err.println("Wallet needs to be generated before running Miner.");
				System.exit(-1);
			}
		} catch(NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
			System.err.println(e);
			System.exit(-1);
		}
		
		ManagedChannel channel = ManagedChannelBuilder.forAddress(HOST, PORT).usePlaintext(true).build();
		TokhnServiceGrpc.TokhnServiceBlockingStub blockingStub = TokhnServiceGrpc.newBlockingStub(channel);
		TokhnServiceStub asyncStub = TokhnServiceGrpc.newStub(channel).withWaitForReady();
		
		WelcomeResponse welcomeResponse = blockingStub.getWelcome(WelcomeRequest.newBuilder().setPeerType(PeerType.MINER).build());
		for(WelcomeModel welcome : welcomeResponse.getWelcomesList()) {
			Network network = Network.valueOf(welcome.getNetwork().name());
			tails.put(network, GRPC.transform(welcome.getLatestBlock()));
			difficulties.put(network, welcome.getDifficulty());
			rewards.put(network, welcome.getReward());
		}
		
		final CountDownLatch blockLatch = new CountDownLatch(1);
		blockObserver = asyncStub.streamBlocks(new StreamObserver<BlockModel>() {
			@Override
			public void onCompleted() {
				blockLatch.countDown();
			}
			
			@Override
			public void onError(Throwable t) {
				System.err.println(t);
			}
			
			@Override
			public void onNext(BlockModel blockModel) {
				/*
				 * we are ignoring what was sent because we are going to assume
				 * a new block means we need to start over
				 */
				if(mineJob != null) {
					//existing mining operation needs to be canceled
					mineJob.cancel(true);
				}
				
				WelcomeResponse welcomeResponse = blockingStub.getWelcome(WelcomeRequest.newBuilder().setPeerType(PeerType.MINER).build());
				for(WelcomeModel welcome : welcomeResponse.getWelcomesList()) {
					Network network = Network.valueOf(welcome.getNetwork().name());
					tails.put(network, GRPC.transform(welcome.getLatestBlock()));
					difficulties.put(network, welcome.getDifficulty());
					rewards.put(network, welcome.getReward());
				}
				
				mineJob = executor.submit(new Runnable() {
					
					@Override
					public void run() {
						mine();
					}
				});
			}
		});
		
		final CountDownLatch txLatch = new CountDownLatch(1);
		asyncStub.streamTransactions(new StreamObserver<TransactionModel>() {
			@Override
			public void onCompleted() {
				txLatch.countDown();
			}
			
			@Override
			public void onError(Throwable t) {
				t.printStackTrace();
				System.err.println(t);
			}
			
			@Override
			public void onNext(TransactionModel transactionModel) {
				Network network = Network.valueOf(transactionModel.getNetwork().name());
				List<Transaction> txs = pendingTxs.get(network);
				if(txs == null) {
					txs = new LinkedList<Transaction>();
				}
				txs.add(GRPC.transform(transactionModel));
			}
		});
		
		mineJob = executor.submit(new Runnable() {
			
			@Override
			public void run() {
				mine();
			}
		});
		
		try {
			blockLatch.await();
			txLatch.await();
			
			executor.shutdown();
		} catch(InterruptedException e) {
			System.err.println(e);
		} finally {
			channel.shutdown();
		}
	}
	
	private void mine() {
		if(pendingTxs.isEmpty()) {
			//no pending transactions, so mine TKHN
			mineBlock(Network.TKHN, null);
		} else {
			Entry<Network, List<Transaction>> firstEntry = pendingTxs.firstEntry();
			mineBlock(firstEntry.getKey(), firstEntry.getValue());
		}
	}
	
	private void mineBlock(Network network, List<Transaction> txs) {
		List<Transaction> transactions = new LinkedList<>();
		//add a reward for ourselves first
		int difficulty = difficulties.get(network);
		int reward = rewards.get(network);
		Block tail = tails.get(network);
		transactions.add(Transaction.rewardOf(wallet.getAddress(network), reward));
		//add all the pending transactions if any
		if(txs != null) {
			txs.forEach(tx -> transactions.add(tx));
		}
		
		Metric metric = new Metric();
		metric.start();
		Block block = LongStream.iterate(0, i -> i + 1).parallel().peek(metric::handleLong).mapToObj(i -> new Block(network, tail.getIndex() + 1, tail.getHash(), Instant.now().getEpochSecond(), transactions, difficulty, i)).filter(b -> Block.hashMatchesDifficulty(b.getHash(), difficulty)).findAny().orElse(null);
		metric.end();
		
		blockObserver.onNext(GRPC.transform(block));
		
		System.out.printf("Mined new block with difficulty of %d at a hash rate of %,.2f Mh/s\n", difficulty, metric.getRate());
	}
	
	private class Metric {
		Instant start;
		Instant end;
		long count = 0;
		
		public void start() {
			start = Instant.now();
		}
		
		public void end() {
			end = Instant.now();
		}
		
		public void handleLong(long l) {
			synchronized(this) {
				count++;
			}
		}
		
		public long getSeconds() {
			return Duration.between(start, end).getSeconds();
		}
		
		public double getRate() {
			return (double) count / getSeconds() / 1000000;
		}
	}
	
	private class PhenoFirst implements Comparator<Network> {
		
		@Override
		public int compare(Network o1, Network o2) {
			if(o1 == Network.PHNO && o2 == Network.PHNO) {
				return 0;
			} else if(o1 == Network.PHNO && o2 != Network.PHNO) {
				return 1;
			} else if(o1 != Network.PHNO && o2 == Network.PHNO) {
				return -1;
			} else {
				return o1.compareTo(o2);
			}
		}
	}
}