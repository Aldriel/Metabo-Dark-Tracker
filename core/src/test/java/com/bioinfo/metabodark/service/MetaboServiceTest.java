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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.metabodark.catalog.Catalog;
import com.bioinfo.metabodark.catalog.CatalogNote;
import com.bioinfo.metabodark.example.SimulatedStudy;
import com.bioinfo.metabodark.library.ReferenceLibrary;
import com.bioinfo.metabodark.model.AnalysisSettings;
import com.bioinfo.metabodark.model.AnalysisStatus;
import com.bioinfo.metabodark.model.AnnotationKind;
import com.bioinfo.metabodark.model.ChemicalFamily;
import com.bioinfo.metabodark.model.LibraryEntry;
import com.bioinfo.metabodark.model.Polarity;
import com.bioinfo.metabodark.parse.PeakDecoder;

import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** The facade: library first, then GNPS, then a neutral loss, else the dark metabolome. */
class MetaboServiceTest {

    private static final double[] PHE_MZ = {103.0542, 120.0808, 166.0863};
    private static final double[] PHE_INTENSITY = {40, 100, 15};
    private static final AnalysisSettings DEFAULTS = AnalysisSettings.defaults();
    private static final AnalysisSettings PUBLIC = new AnalysisSettings(0.02, 0.70, true);
    private static final MetaboService TEACHING =
            new MetaboService(ReferenceLibrary.curated(), (entry, scan) -> CatalogNote.empty());

    @Test
    void aMassBankSpectrumIsNamedByTheDefaultLibrary() {
        LibraryEntry reference =
                ReferenceLibrary.massBank().stream()
                        .filter(e -> e.polarity() == Polarity.POSITIVE && e.mz().length >= 5)
                        .findFirst()
                        .orElseThrow();
        byte[] file =
                spectrum(
                        "positive", reference.precursorMz(), reference.mz(), reference.intensity());
        Annotation row =
                new MetaboService().analyze(file, "x", DEFAULTS, false).annotations().getFirst();
        assertEquals(AnnotationKind.LIBRARY, row.kind());
        assertEquals(reference.inchikey(), row.inchikey());
        assertTrue(row.reference().startsWith("MassBank MSBNK-"));
        assertTrue(row.cosine() > 0.99);
    }

    @Test
    void twoSharedPeaksAreNotEnoughForALibraryName() {
        double[] mz = {103.0542, 120.0808};
        Annotation row = only(spectrum("positive", 166.0863, mz, new double[] {40, 100}), DEFAULTS);
        assertFalse(row.kind() == AnnotationKind.LIBRARY);
        assertTrue(row.cosine() == 0);
    }

    @Test
    void aReferenceWithFewPeaksMustMatchAllOfThem() {
        LibraryEntry twoPeaks =
                new LibraryEntry(
                        "Two", "C2H4O2", "", "", 61.0284, Polarity.POSITIVE, null,
                        new double[] {43.0178, 61.0284}, new double[] {100, 50});
        MetaboService service = new MetaboService(List.of(twoPeaks), (entry, scan) -> null);
        double[] both = {43.0178, 61.0284};
        double[] one = {43.0178, 80.0};
        double[] intensities = {100, 50};
        byte[] all = spectrum("positive", 61.0284, both, intensities);
        byte[] partial = spectrum("positive", 61.0284, one, intensities);
        assertEquals(AnnotationKind.LIBRARY, kindOf(service, all));
        assertFalse(kindOf(service, partial) == AnnotationKind.LIBRARY);
    }

    private static AnnotationKind kindOf(MetaboService service, byte[] file) {
        return service.analyze(file, "x", DEFAULTS, false).annotations().getFirst().kind();
    }

    @Test
    void aSpectrumLikeTheReferenceIsNamedFromTheLibrary() {
        Annotation row = only(spectrum("positive", 166.0863, PHE_MZ, PHE_INTENSITY), DEFAULTS);
        assertEquals(AnnotationKind.LIBRARY, row.kind());
        assertEquals("Phenylalanine", row.name());
        assertEquals("C9H11NO2", row.formula());
        assertEquals(ChemicalFamily.AMINO_ACIDS, row.family());
        assertEquals("HMDB0000159", row.reference());
        assertEquals("MATCH", row.evidence());
        assertEquals(3, row.matchedPeaks());
        assertEquals(1, row.cosine(), 1e-6);
    }

    @Test
    void theWrongIonModeNeverMatchesTheLibrary() {
        Annotation row = only(spectrum("negative", 166.0863, PHE_MZ, PHE_INTENSITY), DEFAULTS);
        assertFalse(row.kind() == AnnotationKind.LIBRARY);
    }

    @Test
    void aPrecursorOutsideTheWindowIsNotCompared() {
        Annotation row = only(spectrum("positive", 166.2, PHE_MZ, PHE_INTENSITY), DEFAULTS);
        assertEquals(AnnotationKind.DARK, row.kind());
        assertEquals(0, row.cosine());
    }

    @Test
    void aCosineBelowTheThresholdStaysUnnamed() {
        double[] intensity = {100, 1, 1};
        byte[] file = spectrum("positive", 166.0863, PHE_MZ, intensity);
        Annotation strict = only(file, new AnalysisSettings(0.02, 0.99, false));
        assertFalse(strict.kind() == AnnotationKind.LIBRARY);
        assertTrue(strict.cosine() > 0 && strict.cosine() < 0.99);
        Annotation lenient = only(file, new AnalysisSettings(0.02, 0.5, false));
        assertEquals(AnnotationKind.LIBRARY, lenient.kind());
    }

    @Test
    void aHexoseLossClassifiesAnUnknownGlycoside() {
        Annotation row =
                only(
                        spectrum("positive", 343.1234, new double[] {181.0704}, new double[] {100}),
                        DEFAULTS);
        assertEquals(AnnotationKind.FAMILY, row.kind());
        assertEquals(ChemicalFamily.GLYCOSIDES, row.family());
        assertEquals("LOSS_HEXOSE", row.evidence());
    }

    @Test
    void aSpectrumWithoutAnyClueStaysInTheDarkMetabolomeWithFormulas() {
        Annotation row =
                only(spectrum("positive", 166.0863, new double[] {50}, new double[] {1}), DEFAULTS);
        assertEquals(AnnotationKind.DARK, row.kind());
        assertEquals("DARK", row.evidence());
        assertTrue(row.formulas().contains("C9H11NO2"));
        assertEquals(row.formulas().getFirst(), row.formula());
    }

    @Test
    void publicDatabasesAreNotCalledUnlessTheVisitorAsks() {
        AtomicInteger calls = new AtomicInteger();
        Catalog counting =
                (entry, scan) -> {
                    calls.incrementAndGet();
                    return CatalogNote.empty();
                };
        MetaboService service = new MetaboService(ReferenceLibrary.curated(), counting);
        service.analyze(SimulatedStudy.bytes(), "demo.mzML", DEFAULTS, true);
        assertEquals(0, calls.get());
        Analysis analysis = service.analyze(SimulatedStudy.bytes(), "demo.mzML", PUBLIC, true);
        assertEquals(analysis.ms2(), calls.get());
    }

    @Test
    void aGnpsNameRescuesASpectrumTheLibraryDoesNotKnow() {
        Catalog gnps = (entry, scan) -> new CatalogNote("", "C10H13N5O4", "", "Adenosine");
        MetaboService service = new MetaboService(ReferenceLibrary.curated(), gnps);
        byte[] file = spectrum("positive", 268.1040, new double[] {136.0618}, new double[] {1});
        Annotation row = service.analyze(file, "x.mzML", PUBLIC, false).annotations().getFirst();
        assertEquals(AnnotationKind.LIBRARY, row.kind());
        assertEquals("Adenosine", row.name());
        assertEquals("GNPS", row.evidence());
        assertEquals("C10H13N5O4; GNPS Adenosine", row.catalog());
    }

    @Test
    void aPubChemFormulaReplacesTheLibraryOne() {
        Catalog pubchem = (entry, scan) -> new CatalogNote("L-Phenylalanine", "C9H11NO2", "", "");
        MetaboService service = new MetaboService(ReferenceLibrary.curated(), pubchem);
        byte[] file = spectrum("positive", 166.0863, PHE_MZ, PHE_INTENSITY);
        Annotation row = service.analyze(file, "x", PUBLIC, false).annotations().getFirst();
        assertEquals("Phenylalanine", row.name());
        assertTrue(row.catalog().startsWith("L-Phenylalanine"));
    }

    @Test
    void aCatalogThatReturnsNothingIsHarmless() {
        Catalog silent = (entry, scan) -> null;
        MetaboService service = new MetaboService(ReferenceLibrary.curated(), silent);
        Analysis analysis = service.analyze(SimulatedStudy.bytes(), "demo.mzML", PUBLIC, true);
        assertEquals(AnalysisStatus.OK, analysis.status());
        assertTrue(analysis.annotations().stream().allMatch(row -> row.catalog().isEmpty()));
    }

    @Test
    void theCountsAddUpToTheNumberOfFragmentSpectra() {
        Analysis analysis =
                new MetaboService().analyze(SimulatedStudy.bytes(), "demo.mzML", DEFAULTS, true);
        assertEquals(
                analysis.ms2(), analysis.libraryHits() + analysis.familyHits() + analysis.dark());
        assertEquals(analysis.ms2(), analysis.annotations().size());
        assertTrue(analysis.simulated());
    }

    @Test
    void aFileWithOnlyFullScansHasNothingToAnnotate() {
        String xml =
                "<mzML><spectrum><cvParam accession=\"MS:1000511\" value=\"1\"/></spectrum></mzML>";
        Analysis analysis =
                new MetaboService()
                        .analyze(xml.getBytes(StandardCharsets.UTF_8), "ms1.mzML", DEFAULTS, false);
        assertEquals(AnalysisStatus.NO_MS2, analysis.status());
        assertEquals(1, analysis.ms1());
        assertTrue(analysis.annotations().isEmpty());
    }

    private static Annotation only(byte[] file, AnalysisSettings settings) {
        Analysis analysis = TEACHING.analyze(file, "one.mzML", settings, false);
        assertEquals(1, analysis.annotations().size());
        return analysis.annotations().getFirst();
    }

    private static byte[] spectrum(String mode, double precursor, double[] mz, double[] intensity) {
        String polarity = "positive".equals(mode) ? "MS:1000130" : "MS:1000129";
        String xml =
                "<mzML><spectrum>"
                        + "<cvParam accession=\"MS:1000511\" value=\"2\"/>"
                        + "<cvParam accession=\""
                        + polarity
                        + "\"/>"
                        + "<precursor><selectedIon>"
                        + "<cvParam accession=\"MS:1000744\" value=\""
                        + precursor
                        + "\"/><cvParam accession=\"MS:1000041\" value=\"1\"/>"
                        + "</selectedIon></precursor><binaryDataArrayList>"
                        + array("MS:1000514", mz)
                        + array("MS:1000515", intensity)
                        + "</binaryDataArrayList></spectrum></mzML>";
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    private static String array(String accession, double[] values) {
        return "<binaryDataArray><cvParam accession=\"MS:1000523\"/><cvParam accession=\""
                + accession
                + "\"/><binary>"
                + PeakDecoder.encode(values, true, ByteOrder.LITTLE_ENDIAN, false)
                + "</binary></binaryDataArray>";
    }
}
