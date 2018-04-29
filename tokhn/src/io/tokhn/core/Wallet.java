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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.tokhn.node.Network;
import io.tokhn.store.MapDBWalletStore;
import io.tokhn.store.WalletStore;

public class Wallet implements Serializable {
	private static final long serialVersionUID = 4678179293993780295L;
	private final Map<Network, Address> addresses = new HashMap<>();
	private final WalletStore store;
	
	public Wallet(WalletStore store) {
		this.store = store;
		
		PublicKey publicKey = store.getPublicKey();
		if(publicKey != null) {
			Arrays.stream(Network.values()).forEach(n -> addresses.put(n, new Address(publicKey, n)));
		}
	}
	
	public static Wallet build() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeySpecException {
		ECGenParameterSpec ecGenSpec = new ECGenParameterSpec("prime192v1");
		KeyPairGenerator g = KeyPairGenerator.getInstance("ECDSA", "BC");
		g.initialize(ecGenSpec, new SecureRandom());
		KeyPair  pair = g.generateKeyPair();
		KeyFactory fact = KeyFactory.getInstance("ECDSA", "BC");
		PublicKey publicKey = fact.generatePublic(new X509EncodedKeySpec(pair.getPublic().getEncoded()));
		PrivateKey privateKey = fact.generatePrivate(new PKCS8EncodedKeySpec(pair.getPrivate().getEncoded()));
		
		Wallet wallet = new Wallet(new MapDBWalletStore());
		wallet.setKeys(privateKey, publicKey);
		return wallet;
	}
	
	public void addUtxos(List<UTXO> utxos) {
		utxos.forEach(utxo -> store.putUtxo(utxo));
	}
	public void setBalance(UTXO utxo) {
		store.putUtxo(utxo);
		
	}
	public Token getBalance(Network network) {
		long megas = store.getUtxos(network).stream().mapToLong(utxo -> utxo.getAmount().getValue()).sum();
		return Token.valueOfInMegas(megas);
	}
	
	public Map<Network, Token> getBalances() {
		Map<Network, Token> balances = new HashMap<>();
		for(Network n : Network.values()) {
			balances.put(n, getBalance(n));
		}
		return balances;
	}
	
	public Transaction sign(Transaction tx) {
		tx.getTxis().stream().forEach(txi -> txi.sign(store.getPrivateKey()));
		return tx;
	}

	public Transaction newTx(Network network, Address to, Token amount) throws Exception {
		List<TXI> txis = new LinkedList<>();
		List<TXO> txos = new LinkedList<>();
		
		List<UTXO> utxos = findUtxosForAmount(network, amount);
		long leftOver = amount.getValue();
		for(UTXO utxo : utxos) {
			txis.add(convertToTxi(utxo));
			leftOver -= utxo.getAmount().getValue();
		}
		if(leftOver > 0) {
			txos.add(new TXO(addresses.get(network), Token.valueOfInMegas(leftOver)));
		}
		txos.add(new TXO(to, amount));
		
		return new Transaction(Instant.now().getEpochSecond(), txis, txos);
	}
	
	public Map<Network, Address> getAddresses() {
		return addresses;
	}
	
	public PrivateKey getPrivateKey() {
		return store.getPrivateKey();
	}
	
	public PublicKey getPublicKey() {
		return store.getPublicKey();
	}

	public Address getAddress(Network network) {
		return addresses.get(network);
	}
	
	private void setKeys(PrivateKey privateKey, PublicKey publicKey) {
		store.putKeys(privateKey, publicKey);
		Arrays.stream(Network.values()).forEach(n -> addresses.put(n, new Address(publicKey, n)));
	}
	
	private TXI convertToTxi(UTXO utxo) {
		return new TXI(utxo.getSourceTxoId(), utxo.getSourceTxoIndex(), utxo.getScript());
	}
	
	private List<UTXO> findUtxosForAmount(Network network, Token amount) throws Exception {
		List<UTXO> found = new LinkedList<>();
		long megasFound = 0;
		for(UTXO utxo : store.getUtxos(network)) {
			found.add(utxo);
			megasFound += utxo.getAmount().getValue();
			if(megasFound >= amount.getValue()) {
				return found;
			}
		}
		throw new Exception("Balance too small");
	}
}