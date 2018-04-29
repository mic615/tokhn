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

package io.tokhn.model;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TXO {
	private final StringProperty address;
	private final LongProperty amount;
	private final StringProperty script;
	
	public TXO(String address, long amount, String script) {
		this.address = new SimpleStringProperty(address);
		this.amount = new SimpleLongProperty(amount);
		this.script = new SimpleStringProperty(script);
	}

	public StringProperty getAddress() {
		return address;
	}

	public LongProperty getAmount() {
		return amount;
	}

	public StringProperty getScript() {
		return script;
	}
}