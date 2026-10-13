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

package com.bioinfo.metabodark.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.metabodark.example.SimulatedStudy;
import com.bioinfo.metabodark.model.AnalysisSettings;
import com.bioinfo.metabodark.model.Language;
import com.bioinfo.metabodark.service.Analysis;
import com.bioinfo.metabodark.service.MetaboService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** The tab-separated table and the PDF report of the simulated plasma. */
class ReportWritersTest {

    private static final Analysis DEMO =
            new MetaboService()
                    .analyze(
                            SimulatedStudy.bytes(),
                            SimulatedStudy.FILE_NAME,
                            AnalysisSettings.defaults(),
                            true);

    @Test
    void theTableHasOneHeaderAndOneLinePerFragmentSpectrum() {
        String[] lines = table().split("\n");
        assertEquals(1 + DEMO.ms2(), lines.length);
        assertTrue(lines[0].startsWith("scan\tretention_s\tprecursor_mz"));
        int columns = lines[0].split("\t", -1).length;
        for (String line : lines) {
            assertEquals(columns, line.split("\t", -1).length, line);
        }
    }

    @Test
    void numbersInTheTableUseADotWhateverTheLocale() {
        java.util.Locale previous = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.CANADA_FRENCH);
            assertTrue(table().contains("166.0863"));
            assertTrue(!table().contains("166,0863"));
        } finally {
            java.util.Locale.setDefault(previous);
        }
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void thePdfIsAValidDocumentInEachLanguage(Language language) {
        byte[] pdf = PdfReportWriter.write(DEMO, language, "Plasma run");
        assertTrue(new String(pdf, 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF-"));
        String tail = new String(pdf, pdf.length - 8, 8, StandardCharsets.US_ASCII);
        assertTrue(tail.contains("%%EOF"));
    }

    @Test
    void theReportStatesThatTheExampleIsSimulated() {
        assertTrue(PdfReportWriter.lines(DEMO, Language.EN, "").get(1).contains("simulated"));
        assertTrue(PdfReportWriter.lines(DEMO, Language.FR, "").get(1).contains("simulees"));
    }

    @Test
    void aBlankTitleBecomesTheApplicationName() {
        String fallback = PdfReportWriter.lines(DEMO, Language.EN, " ").getFirst();
        assertEquals("Metabo-Dark Tracker", fallback);
        assertEquals("Run 7", PdfReportWriter.lines(DEMO, Language.EN, "  Run 7 ").getFirst());
    }

    @Test
    void theReportCountsEachKindOfAnnotation() {
        List<String> lines = PdfReportWriter.lines(DEMO, Language.EN, "");
        assertTrue(lines.contains("MS/MS spectra: " + DEMO.ms2()));
        assertTrue(lines.contains("Library: " + DEMO.libraryHits()));
        assertTrue(lines.contains("Families: " + DEMO.familyHits()));
        assertTrue(lines.contains("Unclassified: " + DEMO.dark()));
    }

    @Test
    void charactersHelveticaCannotDrawAreReplaced() {
        assertEquals("a?b c", PdfReportWriter.encodable("a\u2603b\tc"));
        assertEquals("", PdfReportWriter.encodable(null));
    }

    private static String table() {
        return new String(ResultTableWriter.write(DEMO), StandardCharsets.UTF_8);
    }
}
