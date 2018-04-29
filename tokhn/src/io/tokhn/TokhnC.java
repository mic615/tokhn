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

import java.util.concurrent.CountDownLatch;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.tokhn.grpc.BlockModel;
import io.tokhn.grpc.TokhnServiceGrpc;
import io.tokhn.grpc.TokhnServiceGrpc.TokhnServiceStub;
import io.tokhn.grpc.TransactionModel;
import io.tokhn.grpc.WelcomeRequest;
import io.tokhn.grpc.WelcomeRequest.PeerType;
import io.tokhn.grpc.WelcomeResponse;
import io.tokhn.node.Network;

public class TokhnC {

	public static void main(String[] args) {
		ManagedChannel channel = ManagedChannelBuilder.forAddress(Network.TKHN.getParams().getHost(), Network.TKHN.getParams().getPort()).usePlaintext(true).build();

		TokhnServiceGrpc.TokhnServiceBlockingStub stub = TokhnServiceGrpc.newBlockingStub(channel).withWaitForReady();
		TokhnServiceStub async = TokhnServiceGrpc.newStub(channel).withWaitForReady();
		
		WelcomeResponse welcomeResponse = stub.getWelcome(WelcomeRequest.newBuilder().setPeerType(PeerType.CLIENT).build());
		System.out.println(welcomeResponse);
		
		final CountDownLatch blockLatch = new CountDownLatch(1);
		StreamObserver<BlockModel> blockObserver = async.streamBlocks(new StreamObserver<BlockModel>() {
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
				System.out.println(blockModel);
			}
		});
		
		final CountDownLatch txLatch = new CountDownLatch(1);
		StreamObserver<TransactionModel> txObserver = async.streamTransactions(new StreamObserver<TransactionModel>() {
			@Override
			public void onCompleted() {
				txLatch.countDown();
			}

			@Override
			public void onError(Throwable t) {
				System.err.println(t);
			}

			@Override
			public void onNext(TransactionModel transactionModel) {
				System.out.println(transactionModel);
			}
		});
		
		try {
			blockLatch.await();
			txLatch.await();
		} catch (InterruptedException e) {
			System.err.println(e);
		} finally {
			channel.shutdown();
		}
	}
}