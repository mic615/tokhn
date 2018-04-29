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

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TXI {
	private final StringProperty sourceTxId;
	private final IntegerProperty sourceTxoIndex;
	private final StringProperty script;
	private final ObjectProperty<byte[]> signature;
	
	public TXI(String sourceTxId, int sourceTxoIndex, String script, byte[] signature) {
		this.sourceTxId = new SimpleStringProperty(sourceTxId);
		this.sourceTxoIndex = new SimpleIntegerProperty(sourceTxoIndex);
		this.script = new SimpleStringProperty(script);
		this.signature = new SimpleObjectProperty<byte[]>(signature);
	}

	public StringProperty getSourceTxId() {
		return sourceTxId;
	}

	public IntegerProperty getSourceTxoIndex() {
		return sourceTxoIndex;
	}

	public StringProperty getScript() {
		return script;
	}

	public ObjectProperty<byte[]> getSignature() {
		return signature;
	}
}