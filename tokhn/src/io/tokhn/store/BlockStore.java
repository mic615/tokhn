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

import io.tokhn.core.Block;
import io.tokhn.core.LocalBlock;
import io.tokhn.node.Network;
import io.tokhn.util.Hash;

public interface BlockStore {
	public void put(LocalBlock block);
	public boolean contains(Hash hash);
	public LocalBlock get(Hash hash);
	public LocalBlock getLatestBlock();
	public Network getNetwork();
	public void putOrphan(Block block);
	public Block getOrphan(Hash hash);
	public void removeOrphan(Hash hash);
}