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
import java.math.BigDecimal;

public final class Token implements Comparable<Token>, Serializable {
	private static final long serialVersionUID = 853323352792314991L;
	private static final long TOKEN_VALUE = 1000000; //10^6
	private final long value;
	public static final int SMALLEST_UNIT_EXPONENT = 6;
	public static final Token ZERO = Token.valueOfInOnes(0);
	public static final Token ONE = Token.valueOfInOnes(1);
	
	private Token(final long megas) {
        this.value = megas;
    }
	
	public static Token sum(Token a, Token b) {
		return Token.valueOfInMegas(a.getValue() + b.getValue());
	}
	
	public static Token valueOfInOnes(final int ones) {
		return new Token(ones * TOKEN_VALUE);
	}
	
	public static Token valueOfInMegas(final long megas) {
		return new Token(megas);
	}
	
	public static Token parseToken(final String str) {
        try {
            long megas = new BigDecimal(str).movePointRight(SMALLEST_UNIT_EXPONENT).longValue();
            return Token.valueOfInMegas(megas);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(e);
        }
    }
	
	public String toString() {
		return "‽" + new BigDecimal(value).movePointLeft(SMALLEST_UNIT_EXPONENT);
    }
	
	public long getValue() {
        return value;
    }
	
	public int smallestUnitExponent() {
        return SMALLEST_UNIT_EXPONENT;
    }
	
	@Override
	public int compareTo(Token o) {
		return Long.compare(this.getValue(), o.getValue());
	}
}