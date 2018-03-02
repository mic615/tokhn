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

package io.tokhn.node.message;

import java.util.List;

import io.tokhn.core.UTXO;
import io.tokhn.node.Network;

public class UtxoMessage extends AbstractMessage {
	private static final long serialVersionUID = -3704999753091126950L;
	public final List<UTXO> utxos;
	
	public UtxoMessage(Network network, List<UTXO> utxos) {
		super(network);
		this.utxos = utxos;
	}
	
	public String toString() {
		String relay = getRelayHosts().stream().reduce("", (a, b) -> a + "->" + b);
		return String.format("%s (NET:%s, VER:%s, UTS:%s) [%s]", getClass().getSimpleName(), getNetwork(), getVersion(), utxos, relay);
	}
}