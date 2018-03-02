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
import java.util.Map;
import java.util.Optional;

import io.tokhn.core.Block;
import io.tokhn.core.LocalBlock;
import io.tokhn.node.Network;
import io.tokhn.util.Hash;

public class HeapBlockStore implements BlockStore {
	private Map<Hash, LocalBlock> blocks = new LinkedHashMap<>();
	private Map<Hash, Block> orphans = new LinkedHashMap<>();
	private Network network = null;
	
	public HeapBlockStore(Network network) {
		this.network = network;
	}
	
	@Override
	public void put(LocalBlock block) {
		blocks.put(block.getHash(), block);
	}

	@Override
	public LocalBlock get(Hash hash) {
		return blocks.get(hash);
	}
	
	@Override
	public boolean contains(Hash hash) {
		return blocks.containsKey(hash);
	}

	@Override
	public LocalBlock getLatestBlock() {
		Optional<LocalBlock> block = blocks.values().stream().reduce(LocalBlock::max);
		if(block.isPresent()) {
			return block.get();
		} else {
			return null;
		}
	}

	@Override
	public Network getNetwork() {
		return network;
	}

	@Override
	public void putOrphan(Block block) {
		orphans.put(block.getHash(), block);
	}

	@Override
	public Block getOrphan(Hash hash) {
		return orphans.get(hash);
	}

	@Override
	public void removeOrphan(Hash hash) {
		orphans.remove(hash);
	}
}