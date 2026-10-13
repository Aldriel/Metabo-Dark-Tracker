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

package com.bioinfo.metabodark.model;

/**
 * One reference spectrum of the built-in library. Peaks are teaching values, not a dump of a public
 * spectral library.
 *
 * @param name common name
 * @param formula molecular formula of the neutral molecule
 * @param inchikey InChIKey, or blank when none is shipped
 * @param hmdb HMDB accession, or blank
 * @param precursorMz precursor m/z of this reference
 * @param polarity ion mode
 * @param family ClassyFire-style family
 * @param mz fragment m/z
 * @param intensity fragment intensity
 */
public record LibraryEntry(
        String name,
        String formula,
        String inchikey,
        String hmdb,
        double precursorMz,
        Polarity polarity,
        ChemicalFamily family,
        double[] mz,
        double[] intensity) {

    /** Copies the peak arrays and replaces null texts. */
    public LibraryEntry {
        name = name == null ? "" : name;
        formula = formula == null ? "" : formula;
        inchikey = inchikey == null ? "" : inchikey;
        hmdb = hmdb == null ? "" : hmdb;
        polarity = polarity == null ? Polarity.UNKNOWN : polarity;
        family = family == null ? ChemicalFamily.UNCLASSIFIED : family;
        mz = mz == null ? new double[0] : mz.clone();
        intensity = intensity == null ? new double[0] : intensity.clone();
    }
}
