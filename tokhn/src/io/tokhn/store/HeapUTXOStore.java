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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.tokhn.core.Address;
import io.tokhn.core.UTXO;
import io.tokhn.node.Network;
import io.tokhn.util.Hash;

public class HeapUTXOStore implements UTXOStore {
	private Map<Hash, UTXO> utxos = new LinkedHashMap<>();
	private Network network = null;
	
	public HeapUTXOStore(Network network) {
		this.network = network;
	}

	@Override
	public void put(UTXO utxo) {
		utxos.put(utxo.getUtxoId(), utxo);
	}

	@Override
	public UTXO get(Hash utxoId) {
		return utxos.get(utxoId);
	}
	
	@Override
	public void remove(Hash utxoId) {
		utxos.remove(utxoId);
	}

	@Override
	public Network getNetwork() {
		return network;
	}

	@Override
	public List<UTXO> getUtxos() {
		return utxos.values().stream().collect(Collectors.toList());
	}

	@Override
	public List<UTXO> getUtxosForAddress(Address address) {
		return utxos.values().stream().filter(utxo -> utxo.getAddress().equals(address)).collect(Collectors.toList());
	}
}