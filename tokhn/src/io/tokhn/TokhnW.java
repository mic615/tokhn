
package io.tokhn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.Security;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.sun.tools.corba.se.idl.toJavaPortable.Stub;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.tokhn.model.Address;
import io.tokhn.node.InvalidNetworkException;
import io.tokhn.node.Network;
import io.tokhn.core.TXI;
import io.tokhn.core.TXO;
import io.tokhn.core.Token;
import io.tokhn.core.Transaction;
import io.tokhn.core.UTXO;
import io.tokhn.core.Wallet;
import io.tokhn.grpc.BlockModel;
import io.tokhn.grpc.NetworkModel;
import io.tokhn.grpc.TokhnServiceGrpc;
import io.tokhn.grpc.TokhnServiceGrpc.TokhnServiceStub;
import io.tokhn.grpc.TransactionModel;
import io.tokhn.grpc.TxiModel;
import io.tokhn.grpc.TxoModel;
import io.tokhn.grpc.UtxoModel;
import io.tokhn.grpc.UtxoRequest;
import io.tokhn.grpc.UtxoResponse;
import io.tokhn.grpc.WelcomeRequest;
import io.tokhn.grpc.WelcomeRequest.PeerType;
import io.tokhn.grpc.WelcomeResponse;
import io.tokhn.store.MapDBWalletStore;
import io.tokhn.util.Hash;
import javafx.beans.property.StringProperty;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class TokhnW extends Thread {

	private Wallet wallet = null;
	// private Stage primaryStage;
	// private BorderPane rootLayout;
	// private BooleanProperty tokhnConnected;
	private TokhnServiceStub tokhnStub = null;
	private StreamObserver<TransactionModel> txObserver = null;

	@Option(names = { "-h", "--help" }, usageHelp = true, description = "displays this help message and exit")
	private boolean helpRequested = false;

	@Option(names = { "-H", "--host" }, required = false, description = "the remote host")
	private String HOST = "localhost";

	@Option(names = { "-P", "--port" }, required = false, description = "the remote port")
	private int PORT = 1337;

	@Option(names = { "-A", "--address" }, required = false, description = "address for transaction")
	private String transactionAddress = null;

	@Option(names = { "-a", "--amount" }, required = false, description = "amount for transaction")
	private int transactionAmount = 10;

	@Option(names = { "-w", "--withdraw" }, description = "send to ")
	private boolean withdrawRequested;

	@Option(names = { "-n", "--network" }, required = false, description = "the network")
	private String transactionNetwork ="LUV";

	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		CommandLine.run(new TokhnW(), System.out, args);
	}

	@Override
	public void run() {

		ManagedChannel channel = ManagedChannelBuilder.forAddress(HOST, PORT).usePlaintext(true).build();
		TokhnServiceGrpc.TokhnServiceBlockingStub stub = TokhnServiceGrpc.newBlockingStub(channel).withWaitForReady();
		TokhnServiceStub async = TokhnServiceGrpc.newStub(channel).withWaitForReady();

		WelcomeResponse welcomeResponse = stub
				.getWelcome(WelcomeRequest.newBuilder().setPeerType(PeerType.CLIENT).build());
		System.out.println(welcomeResponse);

		init();
		showAddresses();
		List<Address> addresses = getAddresses();
		showDBBalances();
		String Luv = getAddress(addresses, "LUV");
		io.tokhn.core.Address Luvaddr = null;
		try {
			Luvaddr = new io.tokhn.core.Address(Luv);
		} catch (InvalidNetworkException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ByteString LuvBS = ByteString.copyFrom(Luvaddr.getBytes());
		NetworkModel NM = NetworkModel.LUV;
		UtxoRequest request = UtxoRequest.newBuilder().setNetwork(NM).setAddress(LuvBS).build();
		System.out.println("request data  Network val:" + request.getNetworkValue() + " network :"
				+ request.getNetwork() + "address" + request.getAddress());

		UtxoResponse UTXOResponse = stub.getUtxos(request);
		System.out.println(UTXOResponse);
		if(UTXOResponse.getUtxosCount()>0) {
			updateDBBalance(Network.LUV, UTXOResponse);
		}
		showDBBalances();
		System.out.println("Done");
//		if(withdrawRequested) {
//			Transaction tx = null;
//		io.tokhn.core.Address toaddr = null;
//		try {
//			toaddr = new io.tokhn.core.Address(transactionAddress);
//		} catch (InvalidNetworkException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		Network net = Network.valueOf(transactionNetwork);
//		Token toAmount = Token.valueOfInOnes(transactionAmount);
//			try {
//			 tx = wallet.newTx(net,toaddr , toAmount);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//				
//			}
//			 String typeStr;
//			int type =0;
//			typeStr=tx.getType().toString();
//			if(typeStr=="FEE") {
//				type = 0;
//			}else if (typeStr=="REGULAR") {
//				type = 1;
//			}else if (typeStr=="REWARD") {
//				type = 2;
//			}
//
//			List<TXI> txis = tx.getTxis();
//			List<TxiModel> txiModels= new LinkedList<>();
//			for(TXI txi : txis) {
//				ByteString sig =null;
//				if (txi.getSignature()!=null){
//				 sig =ByteString.copyFrom(txi.getSignature());
//				
//				TxiModel model = TxiModel.newBuilder().setSourceTxId(txi.getSourceTxId().toString()).setSignature(sig).setSourceTxoIndex(txi.getSourceTxoIndex()).setScript(txi.getScript()).build();
//				txiModels.add(model);
//				}
//			}
//			List<TXO> txos = tx.getTxos();
//			List<TxoModel> txoModels= new LinkedList<>();
//			for(TXO txo : txos) {
//				
//				TxoModel model = TxoModel.newBuilder().setAddress(txo.getAddress().toString()).setAmount(txo.getAmount().getValue()).setScript(txo.getScript()).build();
//				txoModels.add(model);
//			}
//			TransactionModel TM  = TransactionModel.newBuilder().addAllTxos(txoModels).addAllTxis(txiModels).setTypeValue(type).setNetwork(NM).setId(tx.getId().toString()).setTimestamp(tx.getTimestamp()).build();
//			
//			//txObserver.onNext(TM);
//		
//		} 
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

	public Wallet getWallet() {
		return wallet;
	}

	public StreamObserver<TransactionModel> getTxObserver() {
		return txObserver;
	}

	public TokhnServiceStub getTokhnStub() {
		return tokhnStub;
	}

	public void init() {
		File file = getWalletFilePath();
		if (file != null) {
			loadWalletFile(file);
		}
	}

	public File getWalletFilePath() {
		Preferences prefs = Preferences.userNodeForPackage(TokhnW.class);
		String Path = Paths.get(".").toAbsolutePath().normalize().toString();
		// "/home/mike/DU/tokhn/tokhn/WStore.db";
		String filePath = Path + "/WStore.db";
		// prefs.get("filePath", null);
		if (filePath != null) {
			return new File(filePath);
		} else {
			return null;
		}
	}

	public void setWalletFilePath(File file) {
		Preferences prefs = Preferences.userNodeForPackage(TokhnW.class);
		if (file != null) {
			prefs.put("filePath", file.getPath());
		} else {
			prefs.remove("filePath");
		}
	}

	public void loadWalletFile(File file) {
		try {
			wallet = new Wallet(new MapDBWalletStore(file));
			setWalletFilePath(file);
		} catch (Exception e) { // catches ANY exception
			System.err.println(e);
		}
	}

	public void saveWalletFile(File file) {
		try {
			// TODO: actually do something here
			wallet = null;
		} catch (Exception e) { // catches ANY exception
			System.err.println(e);
		}
	}

	public void showAddresses() {

		List<Address> addresses = wallet.getAddresses().values().stream()
				.map(a -> new Address(a.getNetwork().toString(), a.toString())).collect(Collectors.toList());
		for (Address address : addresses) {
			System.out.print(address.getNetwork() + " ");
			System.out.println(address.getAddress());
		}

	}

	public List<Address> getAddresses() {

		List<Address> addresses = wallet.getAddresses().values().stream()
				.map(a -> new Address(a.getNetwork().toString(), a.toString())).collect(Collectors.toList());
		for (Address address : addresses) {
			System.out.print(address.getNetwork() + " ");
			System.out.println(address.getAddress());
		}
		return addresses;

	}

	public String getAddress(List<Address> addresses, String network) {
		String addressStr = null;

		for (Address address : addresses) {
			System.out.print(address.getNetwork() + " ");
			System.out.println(address.getAddress());
			String walletNetwork = address.getNetwork().getValue();
			if (walletNetwork == network) {
				addressStr = address.getAddress().getValue();
			}
		}
		return addressStr;

	}

	public void showDBBalances() {
		Map<Network, Token> balances = wallet.getBalances();
		System.out.println(balances);
		// for (Address address: balances) {
		// System.out.print(address.getNetwork() + " ");
		// System.out.println(address.getAddress());
		// }
	}

	public long getUtxoAmount(UtxoModel lastUtxo) {
		long amount;

		System.out.println(lastUtxo.getAmount());
		amount = lastUtxo.getAmount();
		return amount;

	}

	public void updateDBBalance(Network network, UtxoResponse UTXOResponse) {
		UtxoModel lastUtxo = UTXOResponse.getUtxos(UTXOResponse.getUtxosCount() - 1);
		Hash txi = new Hash(lastUtxo.getSourceTxId());
		io.tokhn.core.Address addr = null;
		try {
			addr = new io.tokhn.core.Address(lastUtxo.getAddress());
		} catch (InvalidNetworkException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Token amount = Token.valueOfInMegas(lastUtxo.getAmount());
		int txoIdx = lastUtxo.getSourceTxoIndex();

		UTXO utxo = new UTXO(network, txi, txoIdx, addr, amount);
		if (utxo != null) {
			wallet.setBalance(utxo);
		}
	}

}