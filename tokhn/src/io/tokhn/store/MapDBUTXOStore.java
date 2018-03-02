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

import java.util.List;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import io.tokhn.codec.HashSerializer;
import io.tokhn.codec.UTXOSerializer;
import io.tokhn.core.Address;
import io.tokhn.core.UTXO;
import io.tokhn.node.Network;
import io.tokhn.util.Hash;

public class MapDBUTXOStore implements UTXOStore, AutoCloseable {
	private final Network network;
	private final DB db;
	private HTreeMap<Hash, UTXO> utxos;
	private NavigableSet<byte[]> utxoIndex;
	
	public MapDBUTXOStore(Network network) {
		this.network = network;
		db = DBMaker.fileDB("UStore-" + network.toString() + ".db").closeOnJvmShutdown().make();
		utxos = db.hashMap("utxos").keySerializer(new HashSerializer()).valueSerializer(new UTXOSerializer()).createOrOpen();
		utxoIndex = db.treeSet("utxoIndex").serializer(Serializer.BYTE_ARRAY).createOrOpen();
	}
	
	@Override
	public void put(UTXO utxo) {
		utxos.put(utxo.getUtxoId(), utxo);
		utxoIndex.add(utxo.getUtxoId().getBytes());
		db.commit();
	}

	@Override
	public UTXO get(Hash utxoId) {
		return utxos.get(utxoId);
	}
	
	@Override
	public void remove(Hash utxoId) {
		utxos.remove(utxoId);
		utxoIndex.remove(utxoId.getBytes());
		db.commit();
	}

	@Override
	public Network getNetwork() {
		return network;
	}

	@Override
	public void close() throws Exception {
		db.close();
	}

	@Override
	public List<UTXO> getUtxos() {
		return utxoIndex.stream().map(utxoId -> utxos.get(new Hash(utxoId))).collect(Collectors.toList());
	}

	@Override
	public List<UTXO> getUtxosForAddress(Address address) {
		return utxoIndex.stream().map(utxoId -> utxos.get(new Hash(utxoId))).filter(uxto -> uxto.getAddress().equals(address)).collect(Collectors.toList());
	}
}