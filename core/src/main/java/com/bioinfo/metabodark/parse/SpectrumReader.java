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

import com.bioinfo.metabodark.model.FileFormat;
import com.bioinfo.metabodark.model.MsScan;
import com.bioinfo.metabodark.model.Polarity;
import com.bioinfo.metabodark.model.SpectralRun;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Reads mzML and mzXML into scans. Binary peak arrays may be 32- or 64-bit, little- or big-endian,
 * and zlib-compressed. External XML entities are refused.
 */
public final class SpectrumReader {

    /** Largest accepted upload, in bytes, before decompression. */
    public static final int MAX_BYTES = 40 * 1024 * 1024;

    /** Largest accepted XML after gzip decompression. */
    public static final int MAX_UNCOMPRESSED = 80_000_000;

    private static final String MS_LEVEL = "MS:1000511";
    private static final String RT = "MS:1000016";
    private static final String NEGATIVE = "MS:1000129";
    private static final String POSITIVE = "MS:1000130";
    private static final String SELECTED = "MS:1000744";
    private static final String CHARGE = "MS:1000041";
    private static final String FLOAT64 = "MS:1000523";
    private static final String FLOAT32 = "MS:1000521";
    private static final String ZLIB = "MS:1000574";
    private static final String MZ_ARRAY = "MS:1000514";
    private static final String INTENSITY = "MS:1000515";
    private static final String MINUTES = "UO:0000010";

    private SpectrumReader() {}

    /**
     * Reads a file. A gzip wrapper is accepted.
     *
     * @param content file bytes
     * @param fileName name to keep on the run
     * @return the scans
     */
    public static SpectralRun read(byte[] content, String fileName) {
        if (content == null || content.length == 0) {
            throw new ParseException(ParseException.Code.EMPTY, "The file is empty.");
        }
        if (content.length > MAX_BYTES) {
            throw new ParseException(ParseException.Code.TOO_LARGE, "The file is too large.");
        }
        byte[] xml = gunzip(content);
        String head = new String(xml, 0, Math.min(xml.length, 800), StandardCharsets.UTF_8);
        if (head.contains("<mzXML") || head.contains("<msRun")) {
            return new SpectralRun(fileName, FileFormat.MZXML, mzXml(xml));
        }
        if (head.contains("<mzML") || head.contains("<indexedmzML")) {
            return new SpectralRun(fileName, FileFormat.MZML, mzMl(xml));
        }
        throw new ParseException(ParseException.Code.NOT_XML, "The file is not mzML or mzXML.");
    }

    private static byte[] gunzip(byte[] content) {
        if (content.length < 2 || (content[0] & 0xff) != 0x1f || (content[1] & 0xff) != 0x8b) {
            return content;
        }
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(content))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) >= 0) {
                if (out.size() + read > MAX_UNCOMPRESSED) {
                    throw new ParseException(
                            ParseException.Code.TOO_LARGE, "The file is too large.");
                }
                out.write(chunk, 0, read);
            }
            return out.toByteArray();
        } catch (ParseException e) {
            throw e;
        } catch (IOException e) {
            throw new ParseException(
                    ParseException.Code.NOT_XML, "The gzip wrapper could not be read.");
        }
    }

    private static List<MsScan> mzMl(byte[] xml) {
        List<MsScan> scans = new ArrayList<>();
        MlState state = new MlState();
        XMLStreamReader reader = open(xml);
        try {
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    startMl(reader, state);
                } else if (event == XMLStreamConstants.CHARACTERS && state.inBinary) {
                    state.base64.append(reader.getText());
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    endMl(reader, state, scans);
                }
            }
        } catch (XMLStreamException e) {
            throw new ParseException(ParseException.Code.NOT_XML, "The mzML could not be read.");
        } finally {
            close(reader);
        }
        if (scans.isEmpty()) {
            throw new ParseException(ParseException.Code.NO_SPECTRUM, "The file has no spectrum.");
        }
        return scans;
    }

    private static void startMl(XMLStreamReader reader, MlState state) {
        String name = reader.getLocalName();
        if ("spectrum".equals(name)) {
            state.resetSpectrum();
        } else if ("selectedIon".equals(name)) {
            state.inSelected = true;
        } else if ("binaryDataArray".equals(name)) {
            state.resetArray();
        } else if ("cvParam".equals(name)) {
            applyMlParam(reader, state);
        } else if ("binary".equals(name)) {
            state.inBinary = true;
            state.base64.setLength(0);
        }
    }

    private static void applyMlParam(XMLStreamReader reader, MlState state) {
        String accession = attribute(reader, "accession");
        String value = attribute(reader, "value");
        if (state.inArray) {
            applyArrayParam(accession, state);
            return;
        }
        if (state.inSelected) {
            if (SELECTED.equals(accession)) {
                state.precursor = parseDouble(value);
            } else if (CHARGE.equals(accession)) {
                state.charge = (int) parseDouble(value);
            }
            return;
        }
        if (MS_LEVEL.equals(accession)) {
            state.level = (int) parseDouble(value);
        } else if (RT.equals(accession)) {
            double time = parseDouble(value);
            state.retention = MINUTES.equals(attribute(reader, "unitAccession")) ? time * 60 : time;
        } else if (NEGATIVE.equals(accession)) {
            state.polarity = Polarity.NEGATIVE;
        } else if (POSITIVE.equals(accession)) {
            state.polarity = Polarity.POSITIVE;
        }
    }

    private static void applyArrayParam(String accession, MlState state) {
        if (FLOAT64.equals(accession)) {
            state.bits64 = true;
        } else if (FLOAT32.equals(accession)) {
            state.bits64 = false;
        } else if (ZLIB.equals(accession)) {
            state.zlib = true;
        } else if (MZ_ARRAY.equals(accession)) {
            state.mzArray = true;
            state.intensityArray = false;
        } else if (INTENSITY.equals(accession)) {
            state.intensityArray = true;
            state.mzArray = false;
        }
    }

    private static void endMl(XMLStreamReader reader, MlState state, List<MsScan> scans) {
        String name = reader.getLocalName();
        if ("binary".equals(name)) {
            state.inBinary = false;
        } else if ("binaryDataArray".equals(name)) {
            double[] values =
                    PeakDecoder.decode(
                            state.base64.toString(),
                            state.bits64,
                            ByteOrder.LITTLE_ENDIAN,
                            state.zlib);
            if (state.mzArray) {
                state.mz = values;
            } else if (state.intensityArray) {
                state.intensity = values;
            }
            state.inArray = false;
        } else if ("selectedIon".equals(name)) {
            state.inSelected = false;
        } else if ("spectrum".equals(name)) {
            scans.add(state.toScan());
        }
    }

    private static List<MsScan> mzXml(byte[] xml) {
        List<MsScan> scans = new ArrayList<>();
        XmlState state = new XmlState();
        XMLStreamReader reader = open(xml);
        try {
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    startXml(reader, state);
                } else if (event == XMLStreamConstants.CHARACTERS) {
                    if (state.inPeaks || state.inPrecursor) {
                        state.text.append(reader.getText());
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    endXml(reader, state, scans);
                }
            }
        } catch (XMLStreamException e) {
            throw new ParseException(ParseException.Code.NOT_XML, "The mzXML could not be read.");
        } finally {
            close(reader);
        }
        if (scans.isEmpty()) {
            throw new ParseException(ParseException.Code.NO_SPECTRUM, "The file has no scan.");
        }
        return scans;
    }

    private static void startXml(XMLStreamReader reader, XmlState state) {
        String name = reader.getLocalName();
        if ("scan".equals(name)) {
            state.reset();
            state.level = (int) parseDouble(attribute(reader, "msLevel"));
            state.retention = retentionSeconds(attribute(reader, "retentionTime"));
            state.polarity = polarity(attribute(reader, "polarity"));
        } else if ("precursorMz".equals(name)) {
            state.inPrecursor = true;
            state.text.setLength(0);
            state.charge = (int) parseDouble(attribute(reader, "precursorCharge"));
        } else if ("peaks".equals(name)) {
            state.inPeaks = true;
            state.text.setLength(0);
            state.bits64 = "64".equals(attribute(reader, "precision"));
            state.zlib = "zlib".equalsIgnoreCase(attribute(reader, "compressionType"));
            state.little = "little".equalsIgnoreCase(attribute(reader, "byteOrder"));
        }
    }

    private static void endXml(XMLStreamReader reader, XmlState state, List<MsScan> scans) {
        String name = reader.getLocalName();
        if ("precursorMz".equals(name)) {
            state.precursor = parseDouble(state.text.toString());
            state.inPrecursor = false;
        } else if ("peaks".equals(name)) {
            ByteOrder order = state.little ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
            double[] paired =
                    PeakDecoder.decode(state.text.toString(), state.bits64, order, state.zlib);
            splitPairs(paired, state);
            state.inPeaks = false;
        } else if ("scan".equals(name) && state.level > 0) {
            scans.add(state.toScan());
        }
    }

    /** mzXML stores m/z and intensity one after the other. */
    private static void splitPairs(double[] paired, XmlState state) {
        int peaks = paired.length / 2;
        state.mz = new double[peaks];
        state.intensity = new double[peaks];
        for (int i = 0; i < peaks; i++) {
            state.mz[i] = paired[i * 2];
            state.intensity[i] = paired[i * 2 + 1];
        }
    }

    /** ISO-8601 duration used by mzXML, or a plain number of seconds. */
    static double retentionSeconds(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String text = value.trim();
        if (!text.startsWith("PT")) {
            return parseDouble(text);
        }
        double hours = 0;
        double minutes = 0;
        double seconds = 0;
        StringBuilder number = new StringBuilder();
        for (int i = 2; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= '0' && c <= '9') || c == '.') {
                number.append(c);
            } else if (c == 'H') {
                hours = parseDouble(number.toString());
                number.setLength(0);
            } else if (c == 'M') {
                minutes = parseDouble(number.toString());
                number.setLength(0);
            } else if (c == 'S') {
                seconds = parseDouble(number.toString());
                number.setLength(0);
            }
        }
        return hours * 3600 + minutes * 60 + seconds;
    }

    static Polarity polarity(String value) {
        if (value == null || value.isBlank()) {
            return Polarity.UNKNOWN;
        }
        String text = value.trim().toLowerCase();
        if (text.startsWith("+") || text.startsWith("pos")) {
            return Polarity.POSITIVE;
        }
        if (text.startsWith("-") || text.startsWith("neg")) {
            return Polarity.NEGATIVE;
        }
        return Polarity.UNKNOWN;
    }

    private static double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new ParseException(ParseException.Code.BAD_BINARY, "A number could not be read.");
        }
    }

    private static String attribute(XMLStreamReader reader, String name) {
        String value = reader.getAttributeValue(null, name);
        return value == null ? "" : value;
    }

    private static XMLStreamReader open(byte[] xml) {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        try {
            return factory.createXMLStreamReader(new ByteArrayInputStream(xml));
        } catch (XMLStreamException e) {
            throw new ParseException(ParseException.Code.NOT_XML, "The XML could not be opened.");
        }
    }

    private static void close(XMLStreamReader reader) {
        try {
            reader.close();
        } catch (XMLStreamException e) {
            throw new ParseException(ParseException.Code.NOT_XML, "The XML could not be closed.");
        }
    }

    /** Mutable state while one mzML spectrum is read. */
    private static final class MlState {
        private int index;
        private int level;
        private double retention;
        private double precursor = Double.NaN;
        private int charge;
        private Polarity polarity = Polarity.UNKNOWN;
        private boolean inSelected;
        private boolean inArray;
        private boolean inBinary;
        private boolean bits64 = true;
        private boolean zlib;
        private boolean mzArray;
        private boolean intensityArray;
        private final StringBuilder base64 = new StringBuilder();
        private double[] mz = new double[0];
        private double[] intensity = new double[0];

        private void resetSpectrum() {
            index++;
            level = 1;
            retention = 0;
            precursor = Double.NaN;
            charge = 0;
            polarity = Polarity.UNKNOWN;
            inSelected = false;
            mz = new double[0];
            intensity = new double[0];
        }

        private void resetArray() {
            inArray = true;
            bits64 = true;
            zlib = false;
            mzArray = false;
            intensityArray = false;
            base64.setLength(0);
        }

        private MsScan toScan() {
            return new MsScan(index, level, retention, precursor, charge, polarity, mz, intensity);
        }
    }

    /** Mutable state while one mzXML scan is read. */
    private static final class XmlState {
        private int index;
        private int level;
        private double retention;
        private double precursor = Double.NaN;
        private int charge;
        private Polarity polarity = Polarity.UNKNOWN;
        private boolean inPrecursor;
        private boolean inPeaks;
        private boolean bits64;
        private boolean zlib;
        private boolean little;
        private final StringBuilder text = new StringBuilder();
        private double[] mz = new double[0];
        private double[] intensity = new double[0];

        private void reset() {
            index++;
            level = 0;
            retention = 0;
            precursor = Double.NaN;
            charge = 0;
            polarity = Polarity.UNKNOWN;
            mz = new double[0];
            intensity = new double[0];
        }

        private MsScan toScan() {
            return new MsScan(index, level, retention, precursor, charge, polarity, mz, intensity);
        }
    }
}
