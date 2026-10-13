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

package com.bioinfo.metabodark.library;

import com.bioinfo.metabodark.model.ChemicalFamily;
import com.bioinfo.metabodark.model.LibraryEntry;
import com.bioinfo.metabodark.model.Polarity;

import java.util.List;

/**
 * Small reference library shipped with the application. The fragment masses are the well-known ones
 * of each molecule, written here for the demonstration. They are not a copy of GNPS, MoNA or HMDB
 * spectra.
 */
public final class ReferenceLibrary {

    private ReferenceLibrary() {}

    /** The references, in a stable order. */
    public static List<LibraryEntry> entries() {
        return List.of(
                entry(
                        "Phenylalanine",
                        "C9H11NO2",
                        "COLNVLDHVKWLRT-QMMMGPOBSA-N",
                        "HMDB0000159",
                        166.0863,
                        Polarity.POSITIVE,
                        ChemicalFamily.AMINO_ACIDS,
                        new double[] {103.0542, 120.0808, 166.0863},
                        new double[] {40, 100, 15}),
                entry(
                        "Tryptophan",
                        "C11H12N2O2",
                        "QIVBCDIJIAJPQS-VIFPVBQESA-N",
                        "HMDB0000929",
                        205.0972,
                        Polarity.POSITIVE,
                        ChemicalFamily.AMINO_ACIDS,
                        new double[] {146.0600, 188.0706, 205.0972},
                        new double[] {35, 100, 20}),
                entry(
                        "Caffeine",
                        "C8H10N4O2",
                        "RYYVLZVUVIJVGH-UHFFFAOYSA-N",
                        "HMDB0001847",
                        195.0877,
                        Polarity.POSITIVE,
                        ChemicalFamily.ALKALOIDS,
                        new double[] {110.0713, 138.0662, 195.0877},
                        new double[] {40, 100, 25}),
                entry(
                        "Glucose",
                        "C6H12O6",
                        "WQZGKKKJIJFFOK-GASJEMHNSA-N",
                        "HMDB0000122",
                        179.0561,
                        Polarity.NEGATIVE,
                        ChemicalFamily.CARBOHYDRATES,
                        new double[] {59.0139, 89.0244, 179.0561},
                        new double[] {30, 55, 100}),
                entry(
                        "Citrate",
                        "C6H8O7",
                        "KRKNYBCHXYNGOX-UHFFFAOYSA-N",
                        "HMDB0000094",
                        191.0197,
                        Polarity.NEGATIVE,
                        ChemicalFamily.CARBOXYLIC_ACIDS,
                        new double[] {87.0088, 111.0088, 191.0197},
                        new double[] {40, 100, 30}),
                entry(
                        "LPC 18:1",
                        "C26H52NO7P",
                        "",
                        "",
                        522.3554,
                        Polarity.POSITIVE,
                        ChemicalFamily.GLYCEROPHOSPHOCHOLINES,
                        new double[] {104.1070, 184.0733, 522.3554},
                        new double[] {30, 100, 20}));
    }

    private static LibraryEntry entry(
            String name,
            String formula,
            String inchikey,
            String hmdb,
            double precursor,
            Polarity polarity,
            ChemicalFamily family,
            double[] mz,
            double[] intensity) {
        return new LibraryEntry(
                name, formula, inchikey, hmdb, precursor, polarity, family, mz, intensity);
    }
}
