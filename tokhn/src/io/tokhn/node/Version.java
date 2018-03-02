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

package io.tokhn.node;

import java.io.Serializable;

public enum Version implements Serializable {
	ZERO((byte) 0x00), ONE((byte) 0x01), TWO((byte) 0x02), THREE((byte) 0x03), FOUR((byte) 0x04), FIVE((byte) 0x05);
	
	private final byte id;
	
	private Version(byte id) {
		this.id = id;
	}

	public byte getId() {
		return id;
	}
}