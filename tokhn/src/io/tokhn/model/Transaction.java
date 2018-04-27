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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

public class Transaction {
	private final StringProperty id;
	private final ObjectProperty<LocalDateTime> timestamp;
	private final StringProperty type;
	private final ListProperty<TXI> txis;
	private final ListProperty<TXO> txos;
	
	public Transaction(String id, long timestamp, String type, List<TXI> txis, List<TXO> txos) {
		this.id = new SimpleStringProperty(id);
		this.timestamp = new SimpleObjectProperty<LocalDateTime>(LocalDateTime.ofEpochSecond(timestamp, 0 , ZoneOffset.UTC));
		this.type = new SimpleStringProperty(type);
		this.txis = new SimpleListProperty<TXI>(FXCollections.observableArrayList(txis));
		this.txos = new SimpleListProperty<TXO>(FXCollections.observableArrayList(txos));
	}

	public StringProperty getId() {
		return id;
	}

	public ObjectProperty<LocalDateTime> getTimestamp() {
		return timestamp;
	}

	public StringProperty getType() {
		return type;
	}

	public ListProperty<TXI> getTxis() {
		return txis;
	}

	public ListProperty<TXO> getTxos() {
		return txos;
	}
}