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

package com.bioinfo.metabodark.service;

import com.bioinfo.metabodark.model.AnnotationKind;
import com.bioinfo.metabodark.model.ChemicalFamily;
import com.bioinfo.metabodark.model.Polarity;

import java.util.List;

/**
 * Annotation of one MS/MS spectrum.
 *
 * @param scan scan index in the file
 * @param retentionSeconds retention time in seconds
 * @param precursorMz precursor m/z
 * @param charge precursor charge, or 0
 * @param polarity ion mode
 * @param kind library, family or still dark
 * @param name compound name, or blank
 * @param formula formula of a library hit, or the closest exact-mass formula
 * @param family chemical family
 * @param evidence stable code (MATCH, a LOSS_ code, DARK or GNPS)
 * @param cosine library cosine, or 0
 * @param matchedPeaks peaks shared with the library hit
 * @param inchikey InChIKey, or blank
 * @param hmdb HMDB accession, or blank
 * @param catalog extra text from a public database, or blank
 * @param formulas up to three exact-mass formulas
 */
public record Annotation(
        int scan,
        double retentionSeconds,
        double precursorMz,
        int charge,
        Polarity polarity,
        AnnotationKind kind,
        String name,
        String formula,
        ChemicalFamily family,
        String evidence,
        double cosine,
        int matchedPeaks,
        String inchikey,
        String hmdb,
        String catalog,
        List<String> formulas) {

    /** Replaces null texts and keeps the formula list immutable. */
    public Annotation {
        polarity = polarity == null ? Polarity.UNKNOWN : polarity;
        kind = kind == null ? AnnotationKind.DARK : kind;
        name = name == null ? "" : name;
        formula = formula == null ? "" : formula;
        family = family == null ? ChemicalFamily.UNCLASSIFIED : family;
        evidence = evidence == null ? "" : evidence;
        inchikey = inchikey == null ? "" : inchikey;
        hmdb = hmdb == null ? "" : hmdb;
        catalog = catalog == null ? "" : catalog;
        formulas = formulas == null ? List.of() : List.copyOf(formulas);
    }
}
