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

import com.bioinfo.metabodark.service.Analysis;
import com.bioinfo.metabodark.service.Annotation;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Tab-separated export of the annotations. */
public final class ResultTableWriter {

    private ResultTableWriter() {}

    /**
     * Writes the table, with a header in English. Numbers use a dot.
     *
     * @param analysis the result, which may contain no row
     * @return UTF-8 bytes
     */
    public static byte[] write(Analysis analysis) {
        StringBuilder text = new StringBuilder();
        text.append("scan\tretention_s\tprecursor_mz\tcharge\tpolarity\tkind\tname\tfamily\t");
        text.append("formula\tcosine\tevidence\treference\tcatalog\n");
        for (Annotation row : analysis.annotations()) {
            text.append(row.scan()).append('\t');
            text.append(format(row.retentionSeconds())).append('\t');
            text.append(format(row.precursorMz())).append('\t');
            text.append(row.charge()).append('\t');
            text.append(row.polarity()).append('\t');
            text.append(row.kind()).append('\t');
            text.append(clean(row.name())).append('\t');
            text.append(row.family()).append('\t');
            text.append(clean(row.formula())).append('\t');
            text.append(format(row.cosine())).append('\t');
            text.append(clean(row.evidence())).append('\t');
            text.append(clean(row.reference())).append('\t');
            text.append(clean(row.catalog())).append('\n');
        }
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String format(double value) {
        if (!Double.isFinite(value)) {
            return "";
        }
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static String clean(String value) {
        return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
