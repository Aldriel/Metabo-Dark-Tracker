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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/** Base64 and zlib peak arrays of mzML and mzXML. Numbers are IEEE floats. */
public final class PeakDecoder {

    private PeakDecoder() {}

    /**
     * Decodes a base64 text, optionally compressed with zlib, into doubles.
     *
     * @param base64 text of the binary element, whitespace ignored
     * @param bits64 true for 64-bit floats, false for 32-bit
     * @param order byte order of the decoded buffer
     * @param zlib true when the bytes are a zlib stream
     * @return the numbers, which may be empty
     */
    public static double[] decode(String base64, boolean bits64, ByteOrder order, boolean zlib) {
        if (base64 == null || base64.isBlank()) {
            return new double[0];
        }
        byte[] raw;
        try {
            raw = Base64.getMimeDecoder().decode(base64.replaceAll("\\s", ""));
        } catch (IllegalArgumentException e) {
            throw new ParseException(
                    ParseException.Code.BAD_BINARY, "The peak array is not base64.");
        }
        if (zlib) {
            raw = inflate(raw);
        }
        return doubles(raw, bits64, order);
    }

    /**
     * Encodes numbers the way mzML writes an uncompressed little-endian array. Used by the
     * simulated example and the tests.
     *
     * @param values numbers
     * @param bits64 true for 64-bit floats
     * @param order byte order
     * @param zlib true to wrap the bytes in zlib
     * @return base64 text
     */
    public static String encode(double[] values, boolean bits64, ByteOrder order, boolean zlib) {
        int width = bits64 ? Double.BYTES : Float.BYTES;
        ByteBuffer buffer = ByteBuffer.allocate(values.length * width).order(order);
        for (double value : values) {
            if (bits64) {
                buffer.putDouble(value);
            } else {
                buffer.putFloat((float) value);
            }
        }
        byte[] raw = buffer.array();
        if (zlib) {
            raw = deflate(raw);
        }
        return Base64.getEncoder().encodeToString(raw);
    }

    private static byte[] inflate(byte[] compressed) {
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        try {
            while (!inflater.finished()) {
                int read = inflater.inflate(chunk);
                if (read == 0 && inflater.needsInput()) {
                    break;
                }
                out.write(chunk, 0, read);
            }
        } catch (DataFormatException e) {
            throw new ParseException(ParseException.Code.BAD_BINARY, "The peak array is not zlib.");
        } finally {
            inflater.end();
        }
        return out.toByteArray();
    }

    private static byte[] deflate(byte[] raw) {
        Deflater deflater = new Deflater();
        deflater.setInput(raw);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        while (!deflater.finished()) {
            int read = deflater.deflate(chunk);
            out.write(chunk, 0, read);
        }
        deflater.end();
        return out.toByteArray();
    }

    private static double[] doubles(byte[] raw, boolean bits64, ByteOrder order) {
        int width = bits64 ? Double.BYTES : Float.BYTES;
        if (raw.length % width != 0) {
            throw new ParseException(
                    ParseException.Code.BAD_BINARY, "The peak array length is odd.");
        }
        ByteBuffer buffer = ByteBuffer.wrap(raw).order(order);
        double[] values = new double[raw.length / width];
        for (int i = 0; i < values.length; i++) {
            values[i] = bits64 ? buffer.getDouble() : buffer.getFloat();
        }
        return values;
    }
}
