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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.metabodark.match.SpectralCosine;
import com.bioinfo.metabodark.model.LibraryEntry;
import com.bioinfo.metabodark.model.Polarity;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
                    "S", 31.97207100);
    private static final double PROTON = 1.007276467;

    static Stream<Named<LibraryEntry>> entries() {
        return ReferenceLibrary.entries().stream().map(entry -> Named.of(entry.name(), entry));
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
        assertTrue(entry.hmdb().isEmpty() || entry.hmdb().matches("HMDB\\d{7}"), entry::hmdb);
        assertTrue(
                entry.inchikey().isEmpty()
                        || entry.inchikey().matches("[A-Z]{14}-[A-Z]{10}-[A-Z]"),
                entry::inchikey);
    }

    @Test
    void namesAreUniqueAndBothModesAreCovered() {
        Set<String> names = new HashSet<>();
        Set<Polarity> modes = new HashSet<>();
        for (LibraryEntry entry : ReferenceLibrary.entries()) {
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

    private static List<String> names() {
        return ReferenceLibrary.entries().stream().map(LibraryEntry::name).toList();
    }

    private static double mass(String formula) {
        Matcher part = Pattern.compile("([A-Z][a-z]?)(\\d*)").matcher(formula);
        double total = 0;
        while (part.find()) {
            int count = part.group(2).isEmpty() ? 1 : Integer.parseInt(part.group(2));
            total += ELEMENTS.get(part.group(1)) * count;
        }
        return total;
    }
}
