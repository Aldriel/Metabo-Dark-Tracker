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

import com.bioinfo.metabodark.model.Language;
import com.bioinfo.metabodark.service.Analysis;
import com.bioinfo.metabodark.service.Annotation;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** One-page PDF report. Helvetica only encodes WinAnsi, so other characters become "?". */
public final class PdfReportWriter {

    private PdfReportWriter() {}

    /**
     * Builds the report.
     *
     * @param analysis the result
     * @param language language of the sentences
     * @param title title typed by the visitor; blank becomes the application name
     * @return PDF bytes
     */
    public static byte[] write(Analysis analysis, Language language, String title) {
        List<String> lines = lines(analysis, language == null ? Language.EN : language, title);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(font, 11);
                stream.setLeading(14);
                stream.newLineAtOffset(48, 800);
                int written = 0;
                for (String line : lines) {
                    if (written == 48) {
                        break;
                    }
                    stream.showText(encodable(line));
                    stream.newLine();
                    written++;
                }
                stream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("The PDF could not be written.", e);
        }
    }

    static List<String> lines(Analysis analysis, Language language, String title) {
        boolean french = language == Language.FR;
        List<String> lines = new ArrayList<>();
        String shown = title == null || title.isBlank() ? "Metabo-Dark Tracker" : title.trim();
        lines.add(shown);
        lines.add(
                french
                        ? "Annotation du metabolome sombre. Donnees d'exemple simulees, pas un"
                                + " dosage clinique."
                        : "Dark metabolome annotation. The example is simulated, not a clinical"
                                + " assay.");
        lines.add(
                (french ? "Fichier : " : "File: ")
                        + analysis.fileName()
                        + " ("
                        + analysis.format()
                        + ")");
        lines.add((french ? "Spectres MS/MS : " : "MS/MS spectra: ") + analysis.ms2());
        lines.add((french ? "Bibliotheque : " : "Library: ") + analysis.libraryHits());
        lines.add((french ? "Familles : " : "Families: ") + analysis.familyHits());
        lines.add((french ? "Non classes : " : "Unclassified: ") + analysis.dark());
        lines.add(
                french
                        ? "Correspondance cosinus, pertes neutres, formules par masse exacte."
                        : "Cosine match, neutral losses, exact-mass formulas.");
        int shownRows = 0;
        for (Annotation row : analysis.annotations()) {
            if (shownRows == 30) {
                lines.add(french ? "Lignes suivantes omises." : "Further rows omitted.");
                break;
            }
            String name = row.name().isBlank() ? row.family().name() : row.name();
            lines.add(row.scan() + "  " + name + "  " + row.evidence() + "  " + row.formula());
            shownRows++;
        }
        lines.add("Maxime Ethier - contact@maximeethier.com - https://www.maximeethier.com");
        lines.add("https://ko-fi.com/K1S228CL7A");
        return lines;
    }

    /** Drops characters Helvetica cannot draw. */
    static String encodable(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 32 && c <= 126) {
                out.append(c);
            } else if (c == '\n' || c == '\r' || c == '\t') {
                out.append(' ');
            } else {
                out.append('?');
            }
        }
        return out.toString();
    }
}
