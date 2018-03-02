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

import java.io.IOException;
import java.io.Serializable;

import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import io.tokhn.util.Hash;

public class HashSerializer implements Serializer<Hash>, Serializable {
	private static final long serialVersionUID = 8570611108733467577L;
	
	@Override
    public void serialize(DataOutput2 out, Hash value) throws IOException {
        out.packInt(value.getBytes().length);
        out.write(value.getBytes());
    }

    @Override
    public Hash deserialize(DataInput2 in, int available) throws IOException {
        int size = in.unpackInt();
        byte[] ret = new byte[size];
        in.readFully(ret);
        return new Hash(ret);
    }
}