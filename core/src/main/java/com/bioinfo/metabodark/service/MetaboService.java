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

import com.bioinfo.metabodark.catalog.Catalog;
import com.bioinfo.metabodark.catalog.CatalogNote;
import com.bioinfo.metabodark.match.FamilyRules;
import com.bioinfo.metabodark.match.FormulaFinder;
import com.bioinfo.metabodark.match.SpectralCosine;
import com.bioinfo.metabodark.model.AnalysisSettings;
import com.bioinfo.metabodark.model.AnalysisStatus;
import com.bioinfo.metabodark.model.AnnotationKind;
import com.bioinfo.metabodark.model.ChemicalFamily;
import com.bioinfo.metabodark.model.LibraryEntry;
import com.bioinfo.metabodark.model.MsScan;
import com.bioinfo.metabodark.model.SpectralRun;
import com.bioinfo.metabodark.parse.SpectrumReader;

import java.util.ArrayList;
import java.util.List;

/**
 * Facade of the pipeline. It reads the raw file, matches the built-in library, classifies the
 * remaining spectra by neutral loss, proposes exact-mass formulas, and optionally asks the public
 * databases.
 */
public final class MetaboService {

    private final List<LibraryEntry> library;
    private final Catalog catalog;

    /**
     * The service used by the applications: the shipped library, and no network of its own. The
     * server passes a catalog when the visitor asks for the public databases.
     */
    public MetaboService() {
        this(
                com.bioinfo.metabodark.library.ReferenceLibrary.entries(),
                (entry, scan) -> CatalogNote.empty());
    }

    /**
     * Creates a service with an explicit library and catalog, so tests can substitute both.
     *
     * @param library reference spectra
     * @param catalog public databases, which may return an empty note
     */
    public MetaboService(List<LibraryEntry> library, Catalog catalog) {
        this.library = List.copyOf(library);
        this.catalog = catalog;
    }

    /**
     * Annotates one file.
     *
     * @param content mzML or mzXML bytes
     * @param fileName name to show
     * @param settings tolerances and the public-database switch
     * @param simulated true for the built-in demonstration
     * @return the annotations
     */
    public Analysis analyze(
            byte[] content, String fileName, AnalysisSettings settings, boolean simulated) {
        SpectralRun run = SpectrumReader.read(content, fileName);
        int ms1 = 0;
        List<Annotation> rows = new ArrayList<>();
        for (MsScan scan : run.scans()) {
            if (!scan.ms2()) {
                ms1++;
                continue;
            }
            rows.add(annotate(scan, settings));
        }
        int libraryHits = 0;
        int familyHits = 0;
        int dark = 0;
        for (Annotation row : rows) {
            if (row.kind() == AnnotationKind.LIBRARY) {
                libraryHits++;
            } else if (row.kind() == AnnotationKind.FAMILY) {
                familyHits++;
            } else {
                dark++;
            }
        }
        AnalysisStatus status = rows.isEmpty() ? AnalysisStatus.NO_MS2 : AnalysisStatus.OK;
        return new Analysis(
                run.fileName(),
                run.format(),
                simulated,
                status,
                ms1,
                rows.size(),
                libraryHits,
                familyHits,
                dark,
                settings,
                rows);
    }

    private Annotation annotate(MsScan scan, AnalysisSettings settings) {
        LibraryEntry bestEntry = null;
        SpectralCosine.Score best = new SpectralCosine.Score(0, 0);
        for (LibraryEntry entry : library) {
            if (!scan.polarity().compatibleWith(entry.polarity())) {
                continue;
            }
            if (Math.abs(entry.precursorMz() - scan.precursorMz()) > settings.mzToleranceDa()) {
                continue;
            }
            SpectralCosine.Score score =
                    SpectralCosine.score(
                            scan.mz(),
                            scan.intensity(),
                            entry.mz(),
                            entry.intensity(),
                            settings.mzToleranceDa());
            if (score.value() > best.value()) {
                best = score;
                bestEntry = entry;
            }
        }
        CatalogNote note = CatalogNote.empty();
        if (settings.queryPublic()) {
            note = catalog.describe(bestEntry, scan);
            if (note == null) {
                note = CatalogNote.empty();
            }
        }
        List<String> formulas = formulasOf(scan);
        if (bestEntry != null && best.value() >= settings.cosineMinimum()) {
            return libraryRow(scan, bestEntry, best, note, formulas);
        }
        if (!note.gnpsName().isBlank()) {
            return gnpsRow(scan, note, formulas);
        }
        FamilyRules.Loss loss =
                FamilyRules.match(
                        scan.precursorMz(), scan.mz(), scan.intensity(), settings.mzToleranceDa());
        if (loss != null) {
            return familyRow(scan, loss, best, note, formulas);
        }
        return darkRow(scan, best, note, formulas);
    }

    private static Annotation libraryRow(
            MsScan scan,
            LibraryEntry entry,
            SpectralCosine.Score score,
            CatalogNote note,
            List<String> formulas) {
        String formula = note.formula().isBlank() ? entry.formula() : note.formula();
        return row(
                scan,
                AnnotationKind.LIBRARY,
                entry.name(),
                formula,
                entry.family(),
                "MATCH",
                score.value(),
                score.matched(),
                entry.inchikey(),
                entry.hmdb(),
                note.summary(),
                formulas);
    }

    private static Annotation gnpsRow(MsScan scan, CatalogNote note, List<String> formulas) {
        return row(
                scan,
                AnnotationKind.LIBRARY,
                note.gnpsName(),
                note.formula(),
                ChemicalFamily.UNCLASSIFIED,
                "GNPS",
                0,
                0,
                "",
                "",
                note.summary(),
                formulas);
    }

    private static Annotation familyRow(
            MsScan scan,
            FamilyRules.Loss loss,
            SpectralCosine.Score score,
            CatalogNote note,
            List<String> formulas) {
        String formula = formulas.isEmpty() ? "" : formulas.getFirst();
        return row(
                scan,
                AnnotationKind.FAMILY,
                "",
                formula,
                loss.family(),
                loss.evidence(),
                score.value(),
                score.matched(),
                "",
                "",
                note.summary(),
                formulas);
    }

    private static Annotation darkRow(
            MsScan scan, SpectralCosine.Score score, CatalogNote note, List<String> formulas) {
        String formula = formulas.isEmpty() ? "" : formulas.getFirst();
        return row(
                scan,
                AnnotationKind.DARK,
                "",
                formula,
                ChemicalFamily.UNCLASSIFIED,
                "DARK",
                score.value(),
                score.matched(),
                "",
                "",
                note.summary(),
                formulas);
    }

    private static List<String> formulasOf(MsScan scan) {
        List<FormulaFinder.Guess> guesses =
                FormulaFinder.find(scan.precursorMz(), scan.charge(), scan.polarity(), 10);
        List<String> formulas = new ArrayList<>();
        for (FormulaFinder.Guess guess : guesses) {
            formulas.add(guess.formula());
        }
        return formulas;
    }

    private static Annotation row(
            MsScan scan,
            AnnotationKind kind,
            String name,
            String formula,
            ChemicalFamily family,
            String evidence,
            double cosine,
            int matched,
            String inchikey,
            String hmdb,
            String catalog,
            List<String> formulas) {
        return new Annotation(
                scan.index(),
                scan.retentionSeconds(),
                scan.precursorMz(),
                scan.charge(),
                scan.polarity(),
                kind,
                name,
                formula,
                family,
                evidence,
                cosine,
                matched,
                inchikey,
                hmdb,
                catalog,
                formulas);
    }
}
