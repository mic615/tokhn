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

package io.tokhn.node.network;

import io.tokhn.core.Address;
import io.tokhn.node.Network;
import io.tokhn.node.NetworkParams;

public class Test implements NetworkParams {
	
	public Address getGenesisAddress() {
		return Network.TEST.getCharityAddress();
	}
	
	public int getBlockGenerationInterval() {
		return 1; //this is in seconds
	}
	
	public int getDifficultyAdjustmentInterval() {
		return 1000; //this is in increments
	}
}