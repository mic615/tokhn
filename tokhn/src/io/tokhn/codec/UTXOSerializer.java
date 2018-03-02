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

package io.tokhn.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import io.tokhn.core.UTXO;

public class UTXOSerializer implements Serializer<UTXO>, Serializable {
	private static final long serialVersionUID = -6289735310161145771L;

	@Override
	public UTXO deserialize(DataInput2 in2, int arg1) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(in2.internalByteArray());
		ObjectInputStream ois = new ObjectInputStream(in);
		UTXO tmp = null;
		try {
			tmp = (UTXO) ois.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return tmp;
	}

	@Override
	public void serialize(DataOutput2 out2, UTXO utxo) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(utxo);
		out2.write(out.toByteArray());
	}
}