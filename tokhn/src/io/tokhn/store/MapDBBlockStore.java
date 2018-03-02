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

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import io.tokhn.codec.BlockSerializer;
import io.tokhn.codec.HashSerializer;
import io.tokhn.codec.LocalBlockSerializer;
import io.tokhn.core.Block;
import io.tokhn.core.LocalBlock;
import io.tokhn.node.Network;
import io.tokhn.util.Hash;

public class MapDBBlockStore implements BlockStore, AutoCloseable {
	private final Network network;
	private final DB db;
	private HTreeMap<Hash, LocalBlock> blocks;
	private HTreeMap<Hash, Block> orphans;
	
	public MapDBBlockStore(Network network) {
		this.network = network;
		db = DBMaker.fileDB("BStore-" + network.toString() + ".db").closeOnJvmShutdown().make();
		blocks = db.hashMap("blocks").keySerializer(new HashSerializer()).valueSerializer(new LocalBlockSerializer()).createOrOpen();
		orphans = db.hashMap("orphans").keySerializer(new HashSerializer()).valueSerializer(new BlockSerializer()).createOrOpen();
	}
	
	@Override
	public void put(LocalBlock block) {
		blocks.put(block.getHash(), block);
		db.commit();
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
		LocalBlock latest = null;
		for(Object o : blocks.values()) {
			LocalBlock block = (LocalBlock) o;
			if(latest == null || block.getIndex() > latest.getIndex()) {
				latest = block;
			}
		}
		return latest;
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
	public void putOrphan(Block block) {
		orphans.put(block.getHash(), block);
		db.commit();
	}

	@Override
	public Block getOrphan(Hash hash) {
		return orphans.get(hash);
	}

	@Override
	public void removeOrphan(Hash hash) {
		orphans.remove(hash);
		db.commit();
	}
}