/*
 * Copyright 2026 Maxime Ethier - Consultant en Bioinformatique/Biocomputing Consultant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bioinfo.metabodark.parse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.ByteOrder;

/** The base64 peak arrays of mzML and mzXML. */
class PeakDecoderTest {

    private static final double[] PEAKS = {103.0542, 120.0808, 166.0863, 1.5e6};

    @ParameterizedTest(name = "64 bits: {0}, little endian: {1}, zlib: {2}")
    @CsvSource({
        "true, true, false",
        "true, true, true",
        "true, false, false",
        "true, false, true",
        "false, true, false",
        "false, true, true",
        "false, false, false",
        "false, false, true"
    })
    void everyEncodingReadsBackItsNumbers(boolean bits64, boolean little, boolean zlib) {
        ByteOrder order = little ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        String text = PeakDecoder.encode(PEAKS, bits64, order, zlib);
        double[] decoded = PeakDecoder.decode(text, bits64, order, zlib);
        assertEquals(PEAKS.length, decoded.length);
        for (int i = 0; i < PEAKS.length; i++) {
            // A 32-bit float keeps about seven significant digits.
            double relative = bits64 ? 1e-15 : 1e-7;
            assertEquals(PEAKS[i], decoded[i], Math.abs(PEAKS[i]) * relative);
        }
    }

    @Test
    void lineBreaksInsideTheBinaryAreIgnored() {
        String text = PeakDecoder.encode(PEAKS, true, ByteOrder.LITTLE_ENDIAN, false);
        String wrapped = text.substring(0, 10) + "\n  " + text.substring(10) + "\r\n";
        assertArrayEquals(
                PEAKS, PeakDecoder.decode(wrapped, true, ByteOrder.LITTLE_ENDIAN, false), 1e-12);
    }

    @Test
    void anEmptyBinaryHasNoPeak() {
        assertEquals(0, PeakDecoder.decode(null, true, ByteOrder.LITTLE_ENDIAN, false).length);
        assertEquals(0, PeakDecoder.decode(" \n ", true, ByteOrder.LITTLE_ENDIAN, false).length);
        assertEquals(
                "", PeakDecoder.encode(new double[0], true, ByteOrder.BIG_ENDIAN, false));
    }

    @Test
    void aTruncatedArrayIsRejected() {
        ParseException error =
                assertThrows(
                        ParseException.class,
                        () -> PeakDecoder.decode("QUJD", true, ByteOrder.LITTLE_ENDIAN, false));
        assertEquals(ParseException.Code.BAD_BINARY, error.code());
    }

    @Test
    void aBrokenZlibStreamIsRejected() {
        String plain = PeakDecoder.encode(PEAKS, true, ByteOrder.LITTLE_ENDIAN, false);
        assertThrows(
                ParseException.class,
                () -> PeakDecoder.decode(plain, true, ByteOrder.LITTLE_ENDIAN, true));
    }
}
