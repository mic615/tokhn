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

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

import io.tokhn.core.Address;
import io.tokhn.core.UTXO;
import io.tokhn.node.Network;
import io.tokhn.util.Hash;

public interface WalletStore {
	public Network getNetwork();
	public PrivateKey getPrivateKey();
	public PublicKey getPublicKey();
	public Address getAddress();
	public void putKeys(PrivateKey privateKey, PublicKey publicKey, Address address);
	public List<UTXO> getUtxos();
	public UTXO getUtxo(Hash utxoId);
	public void putUtxo(UTXO utxo);
	public void removeUtxo(Hash utxoId);
}