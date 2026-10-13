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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.metabodark.match.SpectralCosine;
import com.bioinfo.metabodark.model.LibraryEntry;
import com.bioinfo.metabodark.model.Polarity;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** The shipped references, checked against masses computed here from the formulas. */
class ReferenceLibraryTest {

    // IUPAC monoisotopic masses, written independently of the application constants.
    private static final Map<String, Double> ELEMENTS =
            Map.of(
                    "C", 12.0,
                    "H", 1.00782503207,
                    "N", 14.0030740048,
                    "O", 15.99491461956,
                    "P", 30.97376163,
                    "S", 31.97207100,
                    "F", 18.99840322,
                    "Cl", 34.96885268,
                    "Br", 78.9183371,
                    "I", 126.904473);
    private static final double PROTON = 1.007276467;

    static Stream<Named<LibraryEntry>> entries() {
        return ReferenceLibrary.curated().stream().map(entry -> Named.of(entry.name(), entry));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entries")
    void thePrecursorIsTheProtonatedOrDeprotonatedFormula(LibraryEntry entry) {
        double neutral = mass(entry.formula());
        double expected =
                entry.polarity() == Polarity.NEGATIVE ? neutral - PROTON : neutral + PROTON;
        double ppm = Math.abs(entry.precursorMz() - expected) / expected * 1e6;
        assertTrue(ppm < 5, () -> entry.name() + " is " + ppm + " ppm away");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entries")
    void fragmentsAreNoHeavierThanThePrecursor(LibraryEntry entry) {
        assertEquals(entry.mz().length, entry.intensity().length);
        for (double fragment : entry.mz()) {
            assertTrue(fragment <= entry.precursorMz() + 0.001, entry::name);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entries")
    void eachReferenceMatchesItsOwnSpectrum(LibraryEntry entry) {
        SpectralCosine.Score score =
                SpectralCosine.score(
                        entry.mz(), entry.intensity(), entry.mz(), entry.intensity(), 0.02);
        assertEquals(1, score.value(), 1e-9);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entries")
    void identifiersFollowTheirPublicFormats(LibraryEntry entry) {
        assertTrue(
                entry.reference().isEmpty() || entry.reference().matches("HMDB\\d{7}"),
                entry::reference);
        assertTrue(
                entry.inchikey().isEmpty()
                        || entry.inchikey().matches("[A-Z]{14}-[A-Z]{10}-[A-Z]"),
                entry::inchikey);
    }

    @Test
    void namesAreUniqueAndBothModesAreCovered() {
        Set<String> names = new HashSet<>();
        Set<Polarity> modes = new HashSet<>();
        for (LibraryEntry entry : ReferenceLibrary.curated()) {
            assertTrue(names.add(entry.name()), entry::name);
            modes.add(entry.polarity());
        }
        assertTrue(modes.contains(Polarity.POSITIVE));
        assertTrue(modes.contains(Polarity.NEGATIVE));
    }

    @Test
    void theLibraryIsReturnedInAStableOrder() {
        assertEquals(names(), names());
    }

    @Test
    void theFullLibraryIsTheTeachingSpectraThenMassBank() {
        List<LibraryEntry> all = ReferenceLibrary.entries();
        int expected = ReferenceLibrary.curated().size() + ReferenceLibrary.massBank().size();
        assertEquals(expected, all.size());
        assertEquals("Phenylalanine", all.getFirst().name());
    }

    @Test
    void massBankBringsThousandsOfSpectraInBothModes() {
        List<LibraryEntry> massBank = ReferenceLibrary.massBank();
        assertTrue(massBank.size() > 10_000, () -> massBank.size() + " spectra");
        assertTrue(massBank.stream().anyMatch(e -> e.polarity() == Polarity.POSITIVE));
        assertTrue(massBank.stream().anyMatch(e -> e.polarity() == Polarity.NEGATIVE));
    }

    @Test
    void everyMassBankSpectrumIsTraceableAndWellFormed() {
        for (LibraryEntry entry : ReferenceLibrary.massBank()) {
            assertTrue(entry.reference().matches("MassBank MSBNK-\\S+"), entry::reference);
            assertTrue(entry.inchikey().matches("[A-Z]{14}-[A-Z]{10}-[A-Z]"), entry::inchikey);
            assertFalse(entry.name().isBlank(), entry::reference);
            assertTrue(entry.mz().length > 1 && entry.mz().length <= 25, entry::reference);
            for (int i = 0; i < entry.mz().length; i++) {
                assertTrue(entry.mz()[i] > 0 && entry.intensity()[i] > 0, entry::reference);
            }
        }
    }

    @Test
    void massBankPrecursorsAgreeWithTheirFormulas() {
        int checked = 0;
        int agreeing = 0;
        for (LibraryEntry entry : ReferenceLibrary.massBank()) {
            double neutral = mass(entry.formula());
            if (Double.isNaN(neutral)) {
                continue;
            }
            double expected =
                    entry.polarity() == Polarity.NEGATIVE ? neutral - PROTON : neutral + PROTON;
            checked++;
            // 10 ppm in the build script, plus the rounding of the stored m/z to four decimals.
            if (Math.abs(entry.precursorMz() - expected) / expected * 1e6 < 11) {
                agreeing++;
            }
        }
        assertTrue(checked > 10_000, checked + " checked");
        assertEquals(checked, agreeing, "precursors within 10 ppm of their formula");
    }

    @Test
    void wellKnownMoleculesAreInMassBank() {
        Set<String> keys = new HashSet<>();
        ReferenceLibrary.massBank().forEach(entry -> keys.add(entry.inchikey()));
        assertTrue(keys.contains("RYYVLZVUVIJVGH-UHFFFAOYSA-N"), "caffeine");
        assertTrue(keys.contains("RZVAJINKPMORJF-UHFFFAOYSA-N"), "acetaminophen");
        assertTrue(keys.contains("MXWJVTOOROXGIU-UHFFFAOYSA-N"), "atrazine");
    }

    @Test
    void aValidLineIsReadWithItsPolarityAndAccession() throws Exception {
        List<LibraryEntry> parsed =
                parse("MSBNK-X-1\tCaffeine\tC8H10N4O2\tKEY\t-\t193.0731\t110.07:50 138.06:999\n");
        LibraryEntry entry = parsed.getFirst();
        assertEquals("Caffeine", entry.name());
        assertEquals(Polarity.NEGATIVE, entry.polarity());
        assertEquals("MassBank MSBNK-X-1", entry.reference());
        assertEquals(2, entry.mz().length);
        assertEquals(999, entry.intensity()[1]);
        assertEquals(Polarity.POSITIVE, parse("a\tb\tC\tK\t+\t100\t50:1").getFirst().polarity());
    }

    @ParameterizedTest(name = "skipped: {0}")
    @ValueSource(
            strings = {
                "too\tfew\tcolumns",
                "a\tb\tC\tK\t+\tnot-a-number\t50:1",
                "a\tb\tC\tK\t+\t-5\t50:1",
                "a\tb\tC\tK\t+\t100\t ",
                "a\tb\tC\tK\t+\t100\t50",
                "a\tb\tC\tK\t+\t100\t50:x"
            })
    void aMalformedLineIsSkipped(String line) throws Exception {
        assertTrue(parse(line).isEmpty());
    }

    private static List<LibraryEntry> parse(String text) throws Exception {
        return ReferenceLibrary.parse(
                new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static List<String> names() {
        return ReferenceLibrary.entries().stream().map(LibraryEntry::name).toList();
    }

    /** Monoisotopic mass, or NaN when the formula has an element missing from the table. */
    private static double mass(String formula) {
        Matcher part = Pattern.compile("([A-Z][a-z]?)(\\d*)").matcher(formula);
        double total = 0;
        while (part.find()) {
            Double element = ELEMENTS.get(part.group(1));
            if (element == null) {
                return Double.NaN;
            }
            int count = part.group(2).isEmpty() ? 1 : Integer.parseInt(part.group(2));
            total += element * count;
        }
        return total;
    }
}
