
package io.tokhn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.tokhn.model.Address;
import io.tokhn.node.Network;
import io.tokhn.core.Token;
import io.tokhn.core.Wallet;
import io.tokhn.grpc.BlockModel;
import io.tokhn.grpc.NetworkModel;
import io.tokhn.grpc.TokhnServiceGrpc;
import io.tokhn.grpc.TokhnServiceGrpc.TokhnServiceStub;
import io.tokhn.grpc.TransactionModel;
import io.tokhn.grpc.UtxoRequest;
import io.tokhn.grpc.UtxoResponse;
import io.tokhn.grpc.WelcomeRequest;
import io.tokhn.grpc.WelcomeRequest.PeerType;
import io.tokhn.grpc.WelcomeResponse;
import io.tokhn.store.MapDBWalletStore;
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
		List<Address> addresses= getAddresses();
		showBalances();
		String Luv = getAddress(addresses, "LUV");
		ByteString LuvAddress= ByteString.copyFromUtf8(Luv);
		NetworkModel NM= NetworkModel.LUV;
		UtxoRequest request =UtxoRequest.newBuilder().setNetwork(NM).setAddress(LuvAddress).build();
		 UtxoResponse UTXOResponse = stub.getUtxos(request);
		 System.out.println(UTXOResponse);

		
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

	public String getAddress(List<Address> addresses,String network) {
		String addressStr = null;
	
		for (Address address: addresses) {
			System.out.print(address.getNetwork() + " ");
			System.out.println(address.getAddress());
			String walletNetwork =address.getNetwork().getValue();
			if(walletNetwork==network) {
				addressStr=address.getAddress().getValue();
			}
		}
		return addressStr;

}

	public void showBalances() {
		Map<Network, Token> balances = wallet.getBalances();
		System.out.println(balances);
		// for (Address address: balances) {
		// System.out.print(address.getNetwork() + " ");
		// System.out.println(address.getAddress());
		// }
	}

}