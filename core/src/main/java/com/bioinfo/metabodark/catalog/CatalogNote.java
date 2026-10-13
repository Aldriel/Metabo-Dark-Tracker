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

package com.bioinfo.metabodark.catalog;

/**
 * What a public database added to a local annotation. Blank fields mean that the service did not
 * answer.
 *
 * @param title name returned by PubChem
 * @param formula formula returned by PubChem
 * @param chemicalClass class name returned by ClassyFire
 * @param gnpsName name returned by a GNPS library search
 */
public record CatalogNote(String title, String formula, String chemicalClass, String gnpsName) {

    /** A note with nothing in it. */
    public static CatalogNote empty() {
        return new CatalogNote("", "", "", "");
    }

    /** Replaces null fields. */
    public CatalogNote {
        title = title == null ? "" : title;
        formula = formula == null ? "" : formula;
        chemicalClass = chemicalClass == null ? "" : chemicalClass;
        gnpsName = gnpsName == null ? "" : gnpsName;
    }

    /** True when every field is blank. */
    public boolean blank() {
        return title.isBlank()
                && formula.isBlank()
                && chemicalClass.isBlank()
                && gnpsName.isBlank();
    }

    /** One line for the report. */
    public String summary() {
        StringBuilder text = new StringBuilder();
        append(text, title);
        append(text, formula);
        append(text, chemicalClass);
        append(text, gnpsName.isBlank() ? "" : "GNPS " + gnpsName);
        return text.toString();
    }

    private static void append(StringBuilder text, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!text.isEmpty()) {
            text.append("; ");
        }
        text.append(value);
    }
}
