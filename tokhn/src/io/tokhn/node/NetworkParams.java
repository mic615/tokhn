/*
 * Copyright 2018 Matt Liotta
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.tokhn.node;

import io.tokhn.core.Block;

public interface NetworkParams {
	
	public Block getGenesisBlock();
	
	/**
	 * 
	 * @return Number of seconds between blocks
	 */
	default int getBlockGenerationInterval() {
		return 600;
	}
	
	/**
	 * 
	 * @return Minimum number of blocks between difficulty adjustment
	 */
	default int getDifficultyAdjustmentInterval() {
		return 5;
	}
	
	/**
	 * 
	 * @return Number of seconds plus or minus for timestamps to be considered valid
	 */
	default int getValidDrift() {
		return 7200;
	}
	
	/**
	 * 
	 * @return Number of milliseconds a script can execute
	 */
	default int getMaxCpuTime() {
		return 5000;
	}
	
	/**
	 * 
	 * @return Number of bytes in heap a script can use
	 */
	default int getMaxMemory() {
		return 1048576;
	}
	
	/**
	 * 
	 * @return Number of prepared statements a script can use
	 */
	default int getMaxPreparedStatements() {
		return 30;
	}
	
	/**
	 * 
	 * @return The hostname to connect to for this network
	 */
	default String getHost() {
		return "node.tokhn.io";
	}
	
	/**
	 * 
	 * @return The port to connect to for this network
	 */
	default int getPort() {
		return 1337;
	}
	
	/**
	 * The difficulty of a blockchain starts at 1 and rises based on network rules.
	 * It takes very little resources to mine a block at low difficulty, so it could
	 * be useful for the node to just do the mining at low difficulties. This rule
	 * determines the maximum difficulty the node will engage in its own mining.
	 * Any number less than 1 effectively means don't do internal mining.
	 * 
	 * @return Maximum difficulty or {@code < 1} to turn off internal mining
	 */
	default int getMaxInternalMineDifficulty() {
		return 1;
	}
}