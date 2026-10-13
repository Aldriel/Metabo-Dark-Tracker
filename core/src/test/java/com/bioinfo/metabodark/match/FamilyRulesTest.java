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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bioinfo.metabodark.model.ChemicalFamily;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Families suggested by the classical neutral losses of untargeted metabolomics. */
class FamilyRulesTest {

    private static final double PRECURSOR = 600.0;

    // Exact masses of the neutral fragments: phosphocholine C5H14NO4P, phosphoethanolamine
    // C2H8NO4P, anhydroglucuronic acid C6H8O6, anhydrohexose C6H10O5, anhydrodeoxyhexose C6H10O4,
    // HPO3 and CO2.
    @ParameterizedTest(name = "a loss of {0} Da suggests {1}")
    @CsvSource({
        "183.0660, GLYCEROPHOSPHOCHOLINES, LOSS_CHOLINE",
        "141.0191, GLYCEROPHOSPHOETHANOLAMINES, LOSS_PE",
        "176.0321, GLUCURONIDES, LOSS_GLUCURONIDE",
        "162.0528, GLYCOSIDES, LOSS_HEXOSE",
        "146.0579, GLYCOSIDES, LOSS_DEOXYHEXOSE",
        "79.9663, ORGANOPHOSPHATES, LOSS_PHOSPHATE",
        "43.9898, CARBOXYLIC_ACIDS, LOSS_CO2"
    })
    void eachDiagnosticLossNamesItsFamily(double loss, ChemicalFamily family, String evidence) {
        FamilyRules.Loss found =
                FamilyRules.match(
                        PRECURSOR,
                        new double[] {120.0, PRECURSOR - loss},
                        new double[] {100, 50},
                        0.02);
        assertEquals(family, found.family());
        assertEquals(evidence, found.evidence());
    }

    @Test
    void theMostSpecificLossWinsOverCarbonDioxide() {
        FamilyRules.Loss found =
                FamilyRules.match(
                        PRECURSOR,
                        new double[] {PRECURSOR - 43.990, PRECURSOR - 183.066},
                        new double[] {100, 60},
                        0.02);
        assertEquals("LOSS_CHOLINE", found.evidence());
    }

    @Test
    void aFragmentBelowFivePercentOfTheBasePeakIsNoise() {
        double[] mz = {120.0, PRECURSOR - 162.053};
        assertNull(FamilyRules.match(PRECURSOR, mz, new double[] {100, 4.9}, 0.02));
        assertEquals(
                "LOSS_HEXOSE",
                FamilyRules.match(PRECURSOR, mz, new double[] {100, 5}, 0.02).evidence());
    }

    @Test
    void aFragmentOutsideTheWindowIsNotALoss() {
        double[] mz = {PRECURSOR - 162.053 + 0.03};
        assertNull(FamilyRules.match(PRECURSOR, mz, new double[] {100}, 0.02));
    }

    @Test
    void noFragmentOrNoPrecursorMeansNoFamily() {
        assertNull(FamilyRules.match(PRECURSOR, new double[0], new double[0], 0.02));
        assertNull(FamilyRules.match(Double.NaN, new double[] {100}, new double[] {1}, 0.02));
        assertNull(FamilyRules.match(PRECURSOR, new double[] {438}, new double[] {0}, 0.02));
    }
}
