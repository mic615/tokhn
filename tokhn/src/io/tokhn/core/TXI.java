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

package io.tokhn.core;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.security.spec.InvalidKeySpecException;

import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.util.Arrays;

import io.tokhn.util.Hash;

public class TXI implements Serializable {
	private static final long serialVersionUID = -2192154375566006776L;
	private final Hash sourceTxId;
	private final int sourceTxoIndex;
	private final String script;
	private byte[] signature;
	
	public TXI(Hash sourceTxId, int sourceTxoIndex) {
		this(sourceTxId, sourceTxoIndex, "", null);
	}

	public TXI(Hash sourceTxId, int sourceTxoIndex, String script) {
		this(sourceTxId, sourceTxoIndex, script, null);
	}
	
	public TXI(Hash sourceTxId, int sourceTxoIndex, String script, byte[] signature) {
		this.sourceTxId = sourceTxId;
		this.sourceTxoIndex = sourceTxoIndex;
		this.script = script;
		this.signature = signature;
	}

	public void sign(PrivateKey privateKey) {
		try {
			Signature ecdsaSign = Signature.getInstance("SHA256withECDSA", "BC");
			ecdsaSign.initSign(privateKey);
			ecdsaSign.update(getData());
			signature = ecdsaSign.sign();
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (InvalidKeyException | SignatureException e) {
			System.err.println(e);
		}
	}

	public boolean verify() {
		try {
			byte[] dataBytes = getData();
			byte[] sigBytes = getSignature();

			ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec("prime192v1");
			KeyFactory fact = KeyFactory.getInstance("ECDSA", "BC");
			ECCurve curve = params.getCurve();
			EllipticCurve ellipticCurve = EC5Util.convertCurve(curve, params.getSeed());
			ECPoint point = ECPointUtil.decodePoint(ellipticCurve, sigBytes);
			ECParameterSpec params2 = EC5Util.convertSpec(ellipticCurve, params);
			ECPublicKeySpec keySpec = new ECPublicKeySpec(point, params2);
			PublicKey publicKey = fact.generatePublic(keySpec);

			Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA", "BC");
			ecdsaVerify.initVerify(publicKey);
			ecdsaVerify.update(dataBytes);
			
			return ecdsaVerify.verify(sigBytes);
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (InvalidKeyException | SignatureException | InvalidKeySpecException e) {
			System.err.println(e);
		}
		
		return false;
	}

	public Hash getSourceTxId() {
		return sourceTxId;
	}

	public int getSourceTxoIndex() {
		return sourceTxoIndex;
	}
	
	public String getScript() {
		return script;
	}

	public byte[] getSignature() {
		return signature;
	}
	
	private byte[] getData() {
		return Arrays.concatenate(getSourceTxId().getBytes(), ByteBuffer.allocate(Integer.BYTES).putInt(getSourceTxoIndex()).array(), script.getBytes(StandardCharsets.UTF_8));
	}
}