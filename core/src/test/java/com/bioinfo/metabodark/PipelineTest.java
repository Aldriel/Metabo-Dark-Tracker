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

package com.bioinfo.metabodark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.metabodark.catalog.CatalogNote;
import com.bioinfo.metabodark.catalog.HttpCatalog;
import com.bioinfo.metabodark.example.SimulatedStudy;
import com.bioinfo.metabodark.match.FamilyRules;
import com.bioinfo.metabodark.match.FormulaFinder;
import com.bioinfo.metabodark.match.SpectralCosine;
import com.bioinfo.metabodark.model.AnalysisSettings;
import com.bioinfo.metabodark.model.AnalysisStatus;
import com.bioinfo.metabodark.model.AnnotationKind;
import com.bioinfo.metabodark.model.ChemicalFamily;
import com.bioinfo.metabodark.model.FileFormat;
import com.bioinfo.metabodark.model.Language;
import com.bioinfo.metabodark.model.LibraryEntry;
import com.bioinfo.metabodark.model.MsScan;
import com.bioinfo.metabodark.model.Polarity;
import com.bioinfo.metabodark.model.SpectralRun;
import com.bioinfo.metabodark.parse.ParseException;
import com.bioinfo.metabodark.parse.PeakDecoder;
import com.bioinfo.metabodark.parse.SpectrumReader;
import com.bioinfo.metabodark.report.PdfReportWriter;
import com.bioinfo.metabodark.report.ResultTableWriter;
import com.bioinfo.metabodark.service.Analysis;
import com.bioinfo.metabodark.service.Annotation;
import com.bioinfo.metabodark.service.MetaboService;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

class PipelineTest {

    @Test
    void theSimulatedPlasmaIsAnnotated() {
        Analysis analysis =
                new MetaboService()
                        .analyze(
                                SimulatedStudy.bytes(),
                                SimulatedStudy.FILE_NAME,
                                AnalysisSettings.defaults(),
                                true);
        assertEquals(AnalysisStatus.OK, analysis.status());
        assertEquals(FileFormat.MZML, analysis.format());
        assertEquals(1, analysis.ms1());
        assertEquals(4, analysis.ms2());
        assertEquals(2, analysis.libraryHits());
        assertEquals(1, analysis.familyHits());
        assertEquals(1, analysis.dark());
        assertTrue(analysis.simulated());
        Annotation phenylalanine = named(analysis, "Phenylalanine");
        assertEquals(AnnotationKind.LIBRARY, phenylalanine.kind());
        assertEquals(ChemicalFamily.AMINO_ACIDS, phenylalanine.family());
        assertEquals("HMDB0000159", phenylalanine.hmdb());
        assertTrue(phenylalanine.cosine() > 0.99);
        assertEquals(ChemicalFamily.CARBOXYLIC_ACIDS, named(analysis, "Citrate").family());
        assertTrue(
                analysis.annotations().stream()
                        .anyMatch(row -> row.family() == ChemicalFamily.GLYCOSIDES));
        assertTrue(
                analysis.annotations().stream().anyMatch(row -> row.kind() == AnnotationKind.DARK));
        assertTrue(
                new String(ResultTableWriter.write(analysis), StandardCharsets.UTF_8)
                        .contains("Phenylalanine"));
        byte[] pdf = PdfReportWriter.write(analysis, Language.FR, "  ");
        assertEquals('%', (char) pdf[0]);
        assertTrue(PdfReportWriter.write(analysis, Language.EN, "Run").length > 100);
    }

    @Test
    void publicDatabasesCanNameADarkSpectrum() {
        MetaboService service =
                new MetaboService(
                        List.of(),
                        (entry, scan) ->
                                scan.precursorMz() > 400
                                        ? new CatalogNote("T", "C", "Class", "Remote")
                                        : null);
        Analysis analysis =
                service.analyze(
                        SimulatedStudy.bytes(),
                        "x.mzML",
                        new AnalysisSettings(0.02, 0.7, true),
                        false);
        assertTrue(analysis.annotations().stream().anyMatch(row -> "GNPS".equals(row.evidence())));
        assertTrue(
                analysis.annotations().stream().anyMatch(row -> row.evidence().startsWith("LOSS")));
    }

    @Test
    void onlyAnMs1FileHasNothingToAnnotate() {
        byte[] xml =
                ("<?xml version=\"1.0\"?><mzML><spectrum>"
                                + "<cvParam accession=\"MS:1000511\" value=\"1\"/>"
                                + "</spectrum></mzML>")
                        .getBytes(StandardCharsets.UTF_8);
        Analysis analysis =
                new MetaboService().analyze(xml, " ", AnalysisSettings.defaults(), false);
        assertEquals(AnalysisStatus.NO_MS2, analysis.status());
        assertEquals("upload.mzML", analysis.fileName());
        assertTrue(PdfReportWriter.write(analysis, null, "Title").length > 100);
        assertTrue(PdfReportWriter.write(analysis, Language.EN, null).length > 100);
    }

    @Test
    void readerRejectsBadFilesAndReadsMzXml() throws Exception {
        assertEquals(ParseException.Code.EMPTY, fail(null));
        assertEquals(ParseException.Code.EMPTY, fail(new byte[0]));
        assertEquals(ParseException.Code.TOO_LARGE, fail(new byte[SpectrumReader.MAX_BYTES + 1]));
        assertEquals(ParseException.Code.NOT_XML, fail("hello".getBytes(StandardCharsets.UTF_8)));
        assertEquals(
                ParseException.Code.NO_SPECTRUM,
                fail("<mzML></mzML>".getBytes(StandardCharsets.UTF_8)));
        assertEquals(
                ParseException.Code.BAD_BINARY,
                fail(
                        ("<mzML><spectrum><binaryDataArray><cvParam"
                             + " accession=\"MS:1000521\"/><cvParam accession=\"MS:1000514\"/>"
                             + "<binary>AQ==</binary></binaryDataArray></spectrum></mzML>")
                                .getBytes(StandardCharsets.UTF_8)));
        assertEquals(
                ParseException.Code.BAD_BINARY,
                fail(
                        ("<mzML><spectrum><cvParam accession=\"MS:1000511\""
                                        + " value=\"nope\"/></spectrum></mzML>")
                                .getBytes(StandardCharsets.UTF_8)));
        ByteArrayOutputStream gzipped = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(gzipped)) {
            gzip.write(SimulatedStudy.bytes());
        }
        SpectralRun run = SpectrumReader.read(gzipped.toByteArray(), "demo.mzML");
        assertEquals(5, run.scans().size());
        assertEquals(30, run.scans().getFirst().retentionSeconds(), 0.01);
        assertThrows(
                ParseException.class,
                () -> SpectrumReader.read(new byte[] {0x1f, (byte) 0x8b, 1, 2}, "bad.gz"));
        String peaks =
                PeakDecoder.encode(new double[] {10, 5, 11, 7}, false, ByteOrder.BIG_ENDIAN, true);
        String mzxml =
                "<?xml version=\"1.0\"?><mzXML><msRun>"
                        + "<scan msLevel=\"2\" retentionTime=\"PT1M30.5S\" polarity=\"+\">"
                        + "<precursorMz precursorCharge=\"2\">166.0863</precursorMz>"
                        + "<peaks precision=\"32\" compressionType=\"zlib\" byteOrder=\"network\">"
                        + peaks
                        + "</peaks></scan><scan msLevel=\"1\" polarity=\"-\""
                        + " retentionTime=\"12\"></scan><scan msLevel=\"2\""
                        + " retentionTime=\"PT1H2M3S\" polarity=\"weird\"></scan></msRun></mzXML>";
        SpectralRun xml = SpectrumReader.read(mzxml.getBytes(StandardCharsets.UTF_8), "a.mzXML");
        assertEquals(FileFormat.MZXML, xml.format());
        assertEquals(3, xml.scans().size());
        assertEquals(90.5, xml.scans().getFirst().retentionSeconds(), 0.01);
        assertEquals(2, xml.scans().getFirst().charge());
        assertEquals(10, xml.scans().getFirst().mz()[0], 0.01);
        assertEquals(Polarity.NEGATIVE, xml.scans().get(1).polarity());
        assertEquals(3723, xml.scans().get(2).retentionSeconds(), 0.01);
        assertEquals(
                ParseException.Code.NOT_XML,
                fail("<mzML><spectrum".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void scoresLossesFormulasAndNotes() {
        SpectralCosine.Score same =
                SpectralCosine.score(
                        new double[] {10, 20},
                        new double[] {1, 3},
                        new double[] {20, 10},
                        new double[] {3, 1},
                        0.02);
        assertEquals(1, same.value(), 1e-9);
        assertEquals(2, same.matched());
        assertEquals(
                0,
                SpectralCosine.score(
                                new double[0],
                                new double[0],
                                new double[] {1},
                                new double[] {1},
                                0.1)
                        .value());
        assertEquals(
                0,
                SpectralCosine.score(
                                new double[] {1},
                                new double[] {0},
                                new double[] {1},
                                new double[] {1},
                                0.1)
                        .value());
        assertEquals(
                0,
                SpectralCosine.score(
                                new double[] {1},
                                new double[] {1},
                                new double[] {50},
                                new double[] {1},
                                0.01)
                        .matched());
        for (FamilyRules.Loss loss :
                new FamilyRules.Loss[] {
                    new FamilyRules.Loss(
                            183.066, ChemicalFamily.GLYCEROPHOSPHOCHOLINES, "LOSS_CHOLINE"),
                    new FamilyRules.Loss(
                            141.019, ChemicalFamily.GLYCEROPHOSPHOETHANOLAMINES, "LOSS_PE"),
                    new FamilyRules.Loss(176.032, ChemicalFamily.GLUCURONIDES, "LOSS_GLUCURONIDE"),
                    new FamilyRules.Loss(162.053, ChemicalFamily.GLYCOSIDES, "LOSS_HEXOSE"),
                    new FamilyRules.Loss(146.058, ChemicalFamily.GLYCOSIDES, "LOSS_DEOXYHEXOSE"),
                    new FamilyRules.Loss(79.966, ChemicalFamily.ORGANOPHOSPHATES, "LOSS_PHOSPHATE"),
                    new FamilyRules.Loss(43.990, ChemicalFamily.CARBOXYLIC_ACIDS, "LOSS_CO2")
                }) {
            FamilyRules.Loss found =
                    FamilyRules.match(
                            500, new double[] {500 - loss.daltons()}, new double[] {10}, 0.02);
            assertEquals(loss.evidence(), found.evidence());
        }
        assertEquals(null, FamilyRules.match(Double.NaN, new double[] {1}, new double[] {1}, 0.02));
        assertEquals(null, FamilyRules.match(100, new double[] {1}, new double[] {0}, 0.02));
        assertEquals(null, FamilyRules.match(100, new double[0], new double[0], 0.02));
        List<FormulaFinder.Guess> phenylalanine =
                FormulaFinder.find(166.0863, 0, Polarity.UNKNOWN, 10);
        assertTrue(phenylalanine.stream().anyMatch(guess -> guess.formula().equals("C9H11NO2")));
        assertTrue(
                FormulaFinder.find(191.0197, 1, Polarity.NEGATIVE, 10).stream()
                        .anyMatch(guess -> guess.formula().equals("C6H8O7")));
        assertTrue(FormulaFinder.find(10, 1, Polarity.POSITIVE, 10).isEmpty());
        assertTrue(FormulaFinder.find(Double.NaN, 1, Polarity.POSITIVE, 10).isEmpty());
        assertTrue(FormulaFinder.find(200, 1, Polarity.POSITIVE, 0).isEmpty());
        assertTrue(FormulaFinder.find(2000, 1, Polarity.POSITIVE, 10).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new AnalysisSettings(0, 0.5, false));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisSettings(0.1, 2, false));
        assertEquals(Language.FR, Language.parse("fr-CA"));
        assertEquals(Language.EN, Language.parse(null));
        assertTrue(Polarity.UNKNOWN.compatibleWith(Polarity.NEGATIVE));
        assertFalse(Polarity.POSITIVE.compatibleWith(Polarity.NEGATIVE));
        CatalogNote note = new CatalogNote(null, "C6", null, "X");
        assertEquals("C6; GNPS X", note.summary());
        assertTrue(CatalogNote.empty().blank());
        assertFalse(note.blank());
        assertEquals("", HttpCatalog.jsonString(null, "a"));
        assertEquals("", HttpCatalog.jsonString("{}", null));
        assertEquals("", HttpCatalog.jsonString("{\"a\"", "a"));
        assertEquals("", HttpCatalog.jsonString("{\"a\":1}", "a"));
        assertEquals("Z", HttpCatalog.jsonString("{\"a\": \"Z\"}", "a"));
        double[] mz = {1, 2};
        MsScan scan = new MsScan(1, 2, 1, 10, 1, null, mz, new double[] {3, 4});
        mz[0] = 9;
        assertEquals(1, scan.mz()[0], 0);
        assertEquals(
                1,
                new MsScan(
                                1,
                                2,
                                1,
                                10,
                                1,
                                Polarity.POSITIVE,
                                new double[] {1, 2, 3},
                                new double[] {4})
                        .mz()
                        .length);
        assertTrue(scan.ms2());
        assertFalse(
                new MsScan(
                                1,
                                1,
                                0,
                                Double.NaN,
                                0,
                                Polarity.POSITIVE,
                                new double[] {1},
                                new double[] {1, 2})
                        .ms2());
        new LibraryEntry(null, null, null, null, 1, null, null, null, new double[] {1});
        assertThrows(IllegalArgumentException.class, () -> new SpectralRun(" ", null, null));
    }

    @Test
    void theHttpCatalogReadsALocalServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/pubchem",
                exchange -> respond(exchange, "{\"Title\":\"Phe\",\"MolecularFormula\":\"C9\"}"));
        server.createContext(
                "/classy/queries.json",
                exchange -> respond(exchange, "{\"name\":\"Amino acids\"}"));
        server.createContext(
                "/gnps/search", exchange -> respond(exchange, "{\"name\":\"From GNPS\"}"));
        server.createContext(
                "/bad",
                exchange -> {
                    byte[] body = "no".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(500, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        server.start();
        try {
            int port = server.getAddress().getPort();
            String root = "http://127.0.0.1:" + port;
            HttpClient client =
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpCatalog catalog =
                    new HttpCatalog(root + "/pubchem/", root + "/classy", root + "/gnps", client);
            LibraryEntry entry =
                    new LibraryEntry(
                            "Phenylalanine",
                            "C9",
                            "COLNVLDHVKWLRT-QMMMGPOBSA-N",
                            "",
                            166,
                            Polarity.POSITIVE,
                            ChemicalFamily.AMINO_ACIDS,
                            new double[] {1},
                            new double[] {1});
            MsScan scan =
                    new MsScan(
                            1,
                            2,
                            1,
                            166,
                            1,
                            Polarity.POSITIVE,
                            new double[] {1, 2},
                            new double[] {3, 4});
            CatalogNote note = catalog.describe(entry, scan);
            assertEquals("Phe", note.title());
            assertEquals("C9", note.formula());
            assertEquals("Amino acids", note.chemicalClass());
            assertEquals("From GNPS", note.gnpsName());
            HttpCatalog quiet = new HttpCatalog(root + "/bad", "", null, client);
            assertTrue(quiet.describe(entry, null).title().isBlank());
            assertTrue(
                    quiet.describe(
                                    new LibraryEntry(
                                            "n",
                                            "",
                                            "",
                                            "",
                                            1,
                                            Polarity.POSITIVE,
                                            ChemicalFamily.UNCLASSIFIED,
                                            new double[0],
                                            new double[0]),
                                    scan)
                            .blank());
            HttpCatalog closed =
                    new HttpCatalog(
                            "http://127.0.0.1:1",
                            "http://127.0.0.1:1",
                            "http://127.0.0.1:1",
                            client);
            assertTrue(closed.describe(entry, scan).blank());
            HttpCatalog.defaults();
        } finally {
            server.stop(0);
        }
        List<Annotation> many = new ArrayList<>();
        for (int i = 0; i < 31; i++) {
            many.add(
                    new Annotation(
                            i,
                            1,
                            2,
                            1,
                            Polarity.POSITIVE,
                            AnnotationKind.DARK,
                            "n\tm",
                            "C\n",
                            ChemicalFamily.UNCLASSIFIED,
                            "DARK",
                            Double.NaN,
                            0,
                            "",
                            "",
                            "c\r",
                            List.of()));
        }
        Analysis wide =
                new Analysis(
                        "f",
                        FileFormat.MZML,
                        false,
                        AnalysisStatus.OK,
                        0,
                        31,
                        0,
                        0,
                        31,
                        AnalysisSettings.defaults(),
                        many);
        assertTrue(
                new String(ResultTableWriter.write(wide), StandardCharsets.UTF_8).contains("n m"));
        assertTrue(PdfReportWriter.write(wide, Language.FR, "a\u2603b").length > 100);
    }

    @Test
    void remainingBranches() throws Exception {
        new Annotation(
                1, 0, 1, 0, null, null, null, null, null, null, 0, 0, null, null, null, null);
        Analysis empty =
                new Analysis(
                        null,
                        FileFormat.MZML,
                        false,
                        AnalysisStatus.OK,
                        0,
                        0,
                        0,
                        0,
                        0,
                        AnalysisSettings.defaults(),
                        null);
        assertEquals("", empty.fileName());
        assertTrue(empty.annotations().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new AnalysisSettings(2, 0.5, false));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisSettings(0.1, -0.1, false));
        assertEquals(Language.EN, Language.parse("en"));
        assertTrue(Polarity.POSITIVE.compatibleWith(Polarity.UNKNOWN));
        assertTrue(Polarity.POSITIVE.compatibleWith(Polarity.POSITIVE));
        SpectralRun blank = new SpectralRun(null, FileFormat.MZML, null);
        assertEquals("upload.mzML", blank.fileName());
        assertFalse(
                new MsScan(1, 2, 0, Double.NaN, 0, Polarity.POSITIVE, new double[0], new double[0])
                        .ms2());
        assertFalse(
                new MsScan(1, 2, 0, 0, 0, Polarity.POSITIVE, new double[0], new double[0]).ms2());
        assertEquals(0, PeakDecoder.decode(null, true, ByteOrder.LITTLE_ENDIAN, false).length);
        assertEquals(0, PeakDecoder.decode("  ", false, ByteOrder.BIG_ENDIAN, false).length);
        assertEquals(
                null,
                FamilyRules.match(500, new double[] {10, 337.947}, new double[] {100, 1}, 0.02));
        assertEquals(
                0,
                SpectralCosine.score(
                                new double[] {1},
                                new double[] {1},
                                new double[0],
                                new double[0],
                                0.1)
                        .value());
        assertTrue(
                SpectralCosine.score(
                                        new double[] {5, 50},
                                        new double[] {1, 1},
                                        new double[] {1, 50},
                                        new double[] {1, 1},
                                        0.2)
                                .matched()
                        >= 1);
        assertTrue(new CatalogNote("T", "", "C", "").summary().contains("T"));
        assertEquals(ParseException.Code.NOT_XML, new ParseException(null, "x").code());
        String low =
                "<?xml version=\"1.0\"?><mzML><spectrum>"
                        + "<cvParam accession=\"MS:1000511\" value=\"2\"/>"
                        + "<cvParam accession=\"MS:1000130\"/>"
                        + "<precursor><selectedIon>"
                        + "<cvParam accession=\"MS:1000744\" value=\"15\"/>"
                        + "</selectedIon></precursor></spectrum></mzML>";
        Analysis dark =
                new MetaboService()
                        .analyze(
                                low.getBytes(StandardCharsets.UTF_8),
                                "low.mzML",
                                AnalysisSettings.defaults(),
                                false);
        assertEquals(AnnotationKind.DARK, dark.annotations().getFirst().kind());
        assertEquals("", dark.annotations().getFirst().formula());
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/gnps/search", exchange -> respond(exchange, "{\"name\":\"Only\"}"));
        server.start();
        try {
            String root = "http://127.0.0.1:" + server.getAddress().getPort();
            HttpCatalog catalog =
                    new HttpCatalog(null, null, root + "/gnps", HttpClient.newHttpClient());
            MsScan scan =
                    new MsScan(
                            1, 2, 1, 20, 1, Polarity.POSITIVE, new double[] {1}, new double[] {1});
            assertEquals("Only", catalog.describe(null, scan).gnpsName());
            assertEquals("", HttpCatalog.jsonString("{\"a\": \"Z", "a"));
            assertEquals("", HttpCatalog.jsonString("{\"a\" 1}", "a"));
        } finally {
            server.stop(0);
        }
    }

    private static ParseException.Code fail(byte[] content) {
        return assertThrows(ParseException.class, () -> SpectrumReader.read(content, "f")).code();
    }

    private static Annotation named(Analysis analysis, String name) {
        return analysis.annotations().stream()
                .filter(row -> name.equals(row.name()))
                .findFirst()
                .orElseThrow();
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String json)
            throws java.io.IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
