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

package io.tokhn.util;

public class Utils {
	public static String toHexString(byte[] bytes) {
		return toHexString(bytes, Character.MIN_VALUE);
	}
	
	public static String toHexString(byte[] bytes, char delimiter) {
		StringBuilder sb = new StringBuilder();
		for(int itr = 0; itr < bytes.length; itr++) {
			if(itr < bytes.length - 1) {
				sb.append(String.format("%02X%s", bytes[itr], delimiter));
			} else {
				sb.append(String.format("%02X", bytes[itr]));
			}
		}
		return sb.toString().toLowerCase();
	}
}