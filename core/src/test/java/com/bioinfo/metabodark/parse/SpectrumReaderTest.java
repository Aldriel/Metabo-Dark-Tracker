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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.metabodark.example.SimulatedStudy;
import com.bioinfo.metabodark.model.FileFormat;
import com.bioinfo.metabodark.model.MsScan;
import com.bioinfo.metabodark.model.Polarity;
import com.bioinfo.metabodark.model.SpectralRun;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayOutputStream;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

/** Reading of mzML and mzXML files, their limits and their safety. */
class SpectrumReaderTest {

    @Test
    void theSimulatedPlasmaIsReadScanByScan() {
        SpectralRun run = SpectrumReader.read(SimulatedStudy.bytes(), SimulatedStudy.FILE_NAME);
        assertEquals(FileFormat.MZML, run.format());
        assertEquals(5, run.scans().size());
        MsScan full = run.scans().get(0);
        assertEquals(1, full.level());
        assertFalse(full.ms2());
        MsScan phenylalanine = run.scans().get(1);
        assertTrue(phenylalanine.ms2());
        assertEquals(166.0863, phenylalanine.precursorMz(), 1e-6);
        assertEquals(1, phenylalanine.charge());
        assertEquals(Polarity.POSITIVE, phenylalanine.polarity());
        assertEquals(3, phenylalanine.mz().length);
        assertEquals(120.0808, phenylalanine.mz()[1], 1e-6);
        assertEquals(Polarity.NEGATIVE, run.scans().get(2).polarity());
    }

    @Test
    void retentionTimesInMinutesAreConvertedToSeconds() {
        SpectralRun run = SpectrumReader.read(SimulatedStudy.bytes(), "demo.mzML");
        assertEquals(30, run.scans().get(0).retentionSeconds(), 1e-9);
        assertEquals(12.5, run.scans().get(1).retentionSeconds(), 1e-9);
    }

    @Test
    void aGzippedFileIsReadLikeThePlainOne() throws Exception {
        ByteArrayOutputStream gzipped = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(gzipped)) {
            gzip.write(SimulatedStudy.bytes());
        }
        SpectralRun run = SpectrumReader.read(gzipped.toByteArray(), "demo.mzML.gz");
        assertEquals(5, run.scans().size());
    }

    @Test
    void anMzXmlScanIsRead() {
        String peaks =
                PeakDecoder.encode(
                        new double[] {120.0808, 100, 166.0863, 20},
                        false,
                        ByteOrder.BIG_ENDIAN,
                        false);
        String xml =
                "<?xml version=\"1.0\"?><mzXML><msRun>"
                        + "<scan msLevel=\"2\" retentionTime=\"PT1M30.5S\" polarity=\"+\">"
                        + "<precursorMz precursorCharge=\"1\">166.0863</precursorMz>"
                        + "<peaks precision=\"32\" byteOrder=\"network\">"
                        + peaks
                        + "</peaks></scan></msRun></mzXML>";
        SpectralRun run = SpectrumReader.read(xml.getBytes(StandardCharsets.UTF_8), "a.mzXML");
        MsScan scan = run.scans().getFirst();
        assertEquals(FileFormat.MZXML, run.format());
        assertEquals(90.5, scan.retentionSeconds(), 1e-9);
        assertEquals(166.0863, scan.precursorMz(), 1e-6);
        assertEquals(2, scan.mz().length);
        assertEquals(120.0808, scan.mz()[0], 1e-4);
        assertEquals(100, scan.intensity()[0], 1e-4);
    }

    @ParameterizedTest(name = "\"{0}\" is {1} s")
    @CsvSource({
        "PT1M30.5S, 90.5",
        "PT45S, 45",
        "PT2M, 120",
        "PT1H2M3S, 3723",
        "12.25, 12.25",
        "'', 0"
    })
    void mzXmlRetentionTimesAreReadAsSeconds(String text, double seconds) {
        assertEquals(seconds, SpectrumReader.retentionSeconds(text), 1e-9);
    }

    @ParameterizedTest(name = "\"{0}\" is {1}")
    @CsvSource({
        "+, POSITIVE",
        "positive, POSITIVE",
        "-, NEGATIVE",
        "Negative, NEGATIVE",
        "any, UNKNOWN",
        "'', UNKNOWN"
    })
    void mzXmlPolaritiesAreRecognized(String text, Polarity polarity) {
        assertEquals(polarity, SpectrumReader.polarity(text));
    }

    @ParameterizedTest(name = "{1}")
    @CsvSource({
        "'', EMPTY",
        "hello, NOT_XML",
        "<mzML></mzML>, NO_SPECTRUM",
        "<mzXML><msRun></msRun></mzXML>, NO_SPECTRUM",
        "<mzML><spectrum, NOT_XML"
    })
    void badFilesAreRejectedWithAReason(String content, ParseException.Code code) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        assertEquals(
                code,
                assertThrows(ParseException.class, () -> SpectrumReader.read(bytes, "f")).code());
    }

    @Test
    void aFileAboveTheLimitIsRefusedBeforeBeingRead() {
        byte[] huge = new byte[SpectrumReader.MAX_BYTES + 1];
        assertEquals(
                ParseException.Code.TOO_LARGE,
                assertThrows(ParseException.class, () -> SpectrumReader.read(huge, "f")).code());
    }

    @Test
    void aGzipBombIsStoppedWhileInflating() throws Exception {
        ByteArrayOutputStream gzipped = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(gzipped)) {
            byte[] zeros = new byte[1 << 20];
            for (int i = 0; i * zeros.length <= SpectrumReader.MAX_UNCOMPRESSED; i++) {
                gzip.write(zeros);
            }
        }
        byte[] bomb = gzipped.toByteArray();
        assertTrue(bomb.length < SpectrumReader.MAX_BYTES);
        assertEquals(
                ParseException.Code.TOO_LARGE,
                assertThrows(ParseException.class, () -> SpectrumReader.read(bomb, "f")).code());
    }

    @Test
    void externalEntitiesAreNeverResolved(@TempDir Path folder) throws Exception {
        Path secret = folder.resolve("secret.txt");
        Files.writeString(secret, "TOP-SECRET");
        String xml =
                "<?xml version=\"1.0\"?><!DOCTYPE mzML [<!ENTITY leak SYSTEM \""
                        + secret.toUri()
                        + "\">]><mzML><spectrum>"
                        + "<cvParam accession=\"MS:1000511\" value=\"&leak;\"/>"
                        + "</spectrum></mzML>";
        byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
        ParseException error =
                assertThrows(ParseException.class, () -> SpectrumReader.read(bytes, "x.mzML"));
        assertFalse(error.getMessage().contains("TOP-SECRET"));
    }
}
