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

package io.tokhn.store;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import io.tokhn.codec.HashSerializer;
import io.tokhn.codec.UTXOSerializer;
import io.tokhn.core.UTXO;
import io.tokhn.node.Network;
import io.tokhn.util.Hash;

public class MapDBWalletStore implements WalletStore, AutoCloseable {
	private final DB db;
	private HTreeMap<Hash, UTXO> utxos;
	private NavigableSet<byte[]> utxoIndex;
	private HTreeMap<String, byte[]> params;
	private PrivateKey privateKey;
	private PublicKey publicKey;
	
	public MapDBWalletStore() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		db = DBMaker.fileDB("WStore.db").closeOnJvmShutdown().make();
		utxos = db.hashMap("utxos").keySerializer(new HashSerializer()).valueSerializer(new UTXOSerializer()).createOrOpen();
		utxoIndex = db.treeSet("utxoIndex").serializer(Serializer.BYTE_ARRAY).createOrOpen();
		params = db.hashMap("params").keySerializer(Serializer.STRING).valueSerializer(Serializer.BYTE_ARRAY).createOrOpen();
		
		if(params.containsKey("PUBLICKEY") && params.containsKey("PRIVATEKEY")) {
			KeyFactory fact = KeyFactory.getInstance("ECDSA", "BC");
			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(params.get("PRIVATEKEY"));
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(params.get("PUBLICKEY"));
			
			privateKey = fact.generatePrivate(privateKeySpec);
			publicKey = fact.generatePublic(publicKeySpec);
		}
	}

	@Override
	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	@Override
	public PublicKey getPublicKey() {
		return publicKey;
	}

	@Override
	public List<UTXO> getUtxos(Network network) {
		return utxoIndex.stream().map(utxoId -> utxos.get(new Hash(utxoId))).filter(utxo -> utxo.getNetwork() == network).collect(Collectors.toList());
	}
	@Override
	public UTXO getUtxo(Hash utxoId) {
		return utxos.get(utxoId);
	}

	@Override
	public void removeUtxo(Hash utxoId) {
		utxos.remove(utxoId);
		utxoIndex.remove(utxoId.getBytes());
		db.commit();
	}

	@Override
	public void putUtxo(UTXO utxo) {
		utxos.put(utxo.getUtxoId(), utxo);
		utxoIndex.add(utxo.getUtxoId().getBytes());
		db.commit();
	}

	@Override
	public void close() throws Exception {
		db.close();
	}

	@Override
	public void putKeys(PrivateKey privateKey, PublicKey publicKey) {
		this.privateKey = privateKey;
		this.publicKey = publicKey;
		
		params.put("PRIVATEKEY", privateKey.getEncoded());
		params.put("PUBLICKEY", publicKey.getEncoded());
		db.commit();
	}
}