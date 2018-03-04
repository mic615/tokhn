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

import io.tokhn.core.Address;
import io.tokhn.core.Token;

public interface NetworkParams {
	
	public Address getGenesisAddress();
	
	default int getBlockGenerationInterval() {
		return 600; //this is in seconds
	}
	
	default int getDifficultyAdjustmentInterval() {
		return 5; //this is in increments
	}
	
	default int getValidDrift() {
		return 7200; //this is in seconds
	}
	
	default int getMaxCpuTime() {
		return 5000; //this is in milliseconds
	}
	
	default int getMaxMemory() {
		return 1048576; //this is in bytes
	}
	
	default int getMaxPreparedStatements() {
		return 30;
	}
	
	default long getGenesisTime() {
		return 1514764800; //this is Unix time
	}
	
	default Token getGensisBlockReward() {
		return Token.ONE;
	}
	
	default Version getVersion() {
		return Version.ZERO;
	}
}