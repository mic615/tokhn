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
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import io.tokhn.node.InvalidNetworkException;
import io.tokhn.node.Network;
import io.tokhn.node.Version;
import io.tokhn.store.MapDBWalletStore;
import io.tokhn.store.WalletStore;

public class Wallet implements Serializable {
	private static final long serialVersionUID = 4678179293993780295L;
	private final Network network;
	private final Version version;
	private final WalletStore store;
	
	public Wallet(Network network, Version version, WalletStore store) {
		this.network = network;
		this.version = version;
		this.store = store;
	}
	
	public static Wallet build(Network network, Version version) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeySpecException, InvalidNetworkException {
		ECGenParameterSpec ecGenSpec = new ECGenParameterSpec("prime192v1");
		KeyPairGenerator g = KeyPairGenerator.getInstance("ECDSA", "BC");
		g.initialize(ecGenSpec, new SecureRandom());
		KeyPair  pair = g.generateKeyPair();
		KeyFactory fact = KeyFactory.getInstance("ECDSA", "BC");
		PublicKey publicKey = fact.generatePublic(new X509EncodedKeySpec(pair.getPublic().getEncoded()));
		PrivateKey privateKey = fact.generatePrivate(new PKCS8EncodedKeySpec(pair.getPrivate().getEncoded()));
		Address address = new Address(publicKey, network);
		
		Wallet wallet = new Wallet(network, version, new MapDBWalletStore(network));
		wallet.setKeys(privateKey, publicKey, address);
		return wallet;
	}
	
	public void addUtxos(List<UTXO> utxos) {
		utxos.forEach(utxo -> store.putUtxo(utxo));
	}
	
	public Token getBalance() {
		long megas = store.getUtxos().stream().mapToLong(utxo -> utxo.getAmount().getValue()).sum();
		return Token.valueOfInMegas(megas);
	}
	
	public Transaction sign(Transaction tx) {
		tx.getTxis().stream().forEach(txi -> txi.sign(tx, store.getPrivateKey()));
		return tx;
	}

	public Transaction newTx(Address to, Token amount) throws Exception {
		List<TXI> txis = new LinkedList<>();
		List<TXO> txos = new LinkedList<>();
		
		List<UTXO> utxos = findUtxosForAmount(amount);
		long leftOver = amount.getValue();
		for(UTXO utxo : utxos) {
			txis.add(convertToTxi(utxo));
			leftOver -= utxo.getAmount().getValue();
		}
		if(leftOver > 0) {
			txos.add(new TXO(store.getAddress(), Token.valueOfInMegas(leftOver)));
		}
		txos.add(new TXO(to, amount));
		
		return new Transaction(version, Instant.now().getEpochSecond(), txis, txos);
	}
	
	public void setKeys(PrivateKey privateKey, PublicKey publicKey, Address address) {
		store.putKeys(privateKey, publicKey, address);
	}
	
	public Network getNetwork() {
		return network;
	}
	
	public Version getVersion() {
		return version;
	}
	
	public PrivateKey getPrivateKey() {
		return store.getPrivateKey();
	}
	
	public PublicKey getPublicKey() {
		return store.getPublicKey();
	}

	public Address getAddress() {
		return store.getAddress();
	}
	
	private TXI convertToTxi(UTXO utxo) {
		return new TXI(utxo.getSourceTxoId(), utxo.getSourceTxoIndex());
	}
	
	private List<UTXO> findUtxosForAmount(Token amount) throws Exception {
		List<UTXO> found = new LinkedList<>();
		long megasFound = 0;
		for(UTXO utxo : store.getUtxos()) {
			found.add(utxo);
			megasFound += utxo.getAmount().getValue();
			if(megasFound >= amount.getValue()) {
				return found;
			}
		}
		throw new Exception("Balance too small");
	}
}