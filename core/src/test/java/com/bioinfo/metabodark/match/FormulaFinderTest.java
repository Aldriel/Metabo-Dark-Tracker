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

package com.bioinfo.metabodark.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.metabodark.model.Polarity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

/** Exact-mass formulas checked against published monoisotopic ion masses. */
class FormulaFinderTest {

    @ParameterizedTest(name = "{0} at m/z {1} ({2})")
    @CsvSource({
        "C9H11NO2, 166.0863, POSITIVE",
        "C11H12N2O2, 205.0972, POSITIVE",
        "C8H10N4O2, 195.0877, POSITIVE",
        "C6H12O6, 179.0561, NEGATIVE",
        "C6H8O7, 191.0197, NEGATIVE",
        "C5H5N5, 136.0618, POSITIVE",
        "C3H7NO2S, 122.0270, POSITIVE",
        "C6H13O9P, 259.0224, NEGATIVE"
    })
    void theFormulaOfAKnownMetaboliteIsProposed(String formula, double mz, Polarity polarity) {
        List<FormulaFinder.Guess> guesses = FormulaFinder.find(mz, 1, polarity, 10);
        assertTrue(
                guesses.stream().anyMatch(guess -> guess.formula().equals(formula)),
                () -> formula + " missing from " + guesses);
    }

    @Test
    void aDoublyChargedIonIsBroughtBackToItsNeutralMass() {
        // [M+2H]2+ of phenylalanine, whose neutral monoisotopic mass is 165.0790 Da.
        double mz = (165.078979 + 2 * FormulaFinder.PROTON) / 2;
        assertTrue(
                FormulaFinder.find(mz, 2, Polarity.POSITIVE, 10).stream()
                        .anyMatch(guess -> guess.formula().equals("C9H11NO2")));
    }

    @Test
    void anUnknownModeOrChargeIsReadAsASingleProtonatedIon() {
        assertEquals(
                FormulaFinder.find(166.0863, 1, Polarity.POSITIVE, 10),
                FormulaFinder.find(166.0863, 0, Polarity.UNKNOWN, 10));
    }

    @Test
    void atMostThreeFormulasAreKeptClosestFirst() {
        List<FormulaFinder.Guess> guesses = FormulaFinder.find(522.3554, 1, Polarity.POSITIVE, 10);
        assertTrue(guesses.size() <= 3);
        for (int i = 1; i < guesses.size(); i++) {
            assertTrue(guesses.get(i - 1).ppm() <= guesses.get(i).ppm());
        }
    }

    @Test
    void everyProposalIsInsideThePpmWindow() {
        for (FormulaFinder.Guess guess : FormulaFinder.find(343.1234, 1, Polarity.POSITIVE, 10)) {
            assertTrue(guess.ppm() <= 10, guess::toString);
        }
    }

    @Test
    void formulasAreWrittenInHillOrderWithoutCountsOfOne() {
        List<FormulaFinder.Guess> guesses = FormulaFinder.find(122.0270, 1, Polarity.POSITIVE, 5);
        assertTrue(guesses.stream().anyMatch(guess -> guess.formula().equals("C3H7NO2S")));
    }

    @ParameterizedTest(name = "nothing is proposed for m/z {0} at {1} ppm")
    @CsvSource({"10, 10", "900, 10", "NaN, 10", "200, 0", "200, -5"})
    void massesOutsideTheSearchRangeHaveNoFormula(double mz, double ppm) {
        assertTrue(FormulaFinder.find(mz, 1, Polarity.POSITIVE, ppm).isEmpty());
    }
}
