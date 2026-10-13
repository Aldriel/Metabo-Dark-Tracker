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

import com.bioinfo.metabodark.model.AnalysisSettings;
import com.bioinfo.metabodark.model.AnalysisStatus;
import com.bioinfo.metabodark.model.FileFormat;
import com.bioinfo.metabodark.model.Language;
import com.bioinfo.metabodark.service.Analysis;

import org.junit.jupiter.api.Test;

class ReportDetailsTest {

    @Test
    void helpers() {
        assertEquals("", PdfReportWriter.encodable(null));
        assertEquals("a?b", PdfReportWriter.encodable("a\u2603b"));
        assertEquals("a b", PdfReportWriter.encodable("a\nb"));
        Analysis analysis =
                new Analysis(
                        "f",
                        FileFormat.MZML,
                        false,
                        AnalysisStatus.NO_MS2,
                        1,
                        0,
                        0,
                        0,
                        0,
                        AnalysisSettings.defaults(),
                        null);
        assertTrue(PdfReportWriter.lines(analysis, Language.FR, "  ").get(0).contains("Metabo"));
    }
}
