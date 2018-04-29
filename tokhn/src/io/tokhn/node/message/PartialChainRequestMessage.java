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

import io.tokhn.node.Network;

public class PartialChainRequestMessage extends AbstractMessage {
	private static final long serialVersionUID = -4531721093094708903L;
	public final int startIndex;
	public final int endIndex;
	
	public PartialChainRequestMessage(Network network, int startIndex, int endIndex) {
		super(network);
		this.startIndex = startIndex;
		this.endIndex = endIndex;
	}
	
	public String toString() {
		String relay = getRelayHosts().stream().reduce("", (a, b) -> a + "->" + b);
		return String.format("%s (NET:%s, SRT:%d END:%d) [%s]", getClass().getSimpleName(), getNetwork(), startIndex, endIndex, relay);
	}
}