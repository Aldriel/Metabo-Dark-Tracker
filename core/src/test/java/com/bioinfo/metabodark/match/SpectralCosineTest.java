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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The GNPS-style cosine on square-rooted intensities. */
class SpectralCosineTest {

    private static final double[] MZ = {103.0542, 120.0808, 166.0863};
    private static final double[] INTENSITY = {40, 100, 15};

    @Test
    void aSpectrumMatchesItselfPerfectly() {
        SpectralCosine.Score score = SpectralCosine.score(MZ, INTENSITY, MZ, INTENSITY, 0.02);
        assertEquals(1, score.value(), 1e-12);
        assertEquals(3, score.matched());
    }

    @Test
    void theOrderOfThePeaksDoesNotMatter() {
        double[] mz = {166.0863, 103.0542, 120.0808};
        double[] intensity = {15, 40, 100};
        assertEquals(1, SpectralCosine.score(mz, intensity, MZ, INTENSITY, 0.02).value(), 1e-12);
    }

    @Test
    void theScoreDoesNotDependOnTheIntensityScale() {
        double[] scaled = {4000, 10000, 1500};
        assertEquals(1, SpectralCosine.score(MZ, scaled, MZ, INTENSITY, 0.02).value(), 1e-12);
    }

    @Test
    void theScoreIsSymmetric() {
        double[] mz = {103.0542, 120.0808, 150.0};
        double[] intensity = {10, 100, 60};
        assertEquals(
                SpectralCosine.score(MZ, INTENSITY, mz, intensity, 0.02).value(),
                SpectralCosine.score(mz, intensity, MZ, INTENSITY, 0.02).value(),
                1e-12);
    }

    @Test
    void intensitiesAreSquareRootedBeforeTheComparison() {
        // sqrt gives (2, 1) against (1, 2): 4 / (sqrt(5) * sqrt(5)) = 0.8.
        double[] mz = {100, 200};
        SpectralCosine.Score score =
                SpectralCosine.score(mz, new double[] {4, 1}, mz, new double[] {1, 4}, 0.01);
        assertEquals(0.8, score.value(), 1e-12);
    }

    @Test
    void spectraWithoutCommonPeaksScoreZero() {
        double[] elsewhere = {50, 60};
        SpectralCosine.Score score =
                SpectralCosine.score(MZ, INTENSITY, elsewhere, new double[] {1, 1}, 0.02);
        assertEquals(0, score.value());
        assertEquals(0, score.matched());
    }

    @ParameterizedTest(name = "a peak {0} Da away, window {1} Da, matches: {2}")
    @CsvSource({"0.015, 0.02, true", "0.02, 0.02, true", "0.015, 0.01, false", "0.5, 0.02, false"})
    void peaksMatchOnlyInsideTheWindow(double shift, double window, boolean matches) {
        SpectralCosine.Score score =
                SpectralCosine.score(
                        new double[] {100 + shift},
                        new double[] {1},
                        new double[] {100},
                        new double[] {1},
                        window);
        assertEquals(matches ? 1 : 0, score.matched());
    }

    @Test
    void aLibraryPeakPairsWithOnlyOneQueryPeak() {
        SpectralCosine.Score score =
                SpectralCosine.score(
                        new double[] {100.000, 100.010},
                        new double[] {1, 1},
                        new double[] {100.005},
                        new double[] {1},
                        0.02);
        assertEquals(1, score.matched());
        assertTrue(score.value() < 1);
    }

    @Test
    void anEmptySpectrumScoresZero() {
        double[] none = new double[0];
        assertEquals(0, SpectralCosine.score(none, none, MZ, INTENSITY, 1).value());
        assertEquals(0, SpectralCosine.score(MZ, INTENSITY, none, none, 1).value());
    }

    @Test
    void aSpectrumWithoutIntensityScoresZero() {
        assertEquals(0, SpectralCosine.score(MZ, new double[3], MZ, INTENSITY, 0.02).value());
        assertEquals(0, SpectralCosine.score(MZ, INTENSITY, MZ, new double[3], 0.02).value());
    }

    @Test
    void unpairedTrailingValuesAreIgnored() {
        double[] longer = {103.0542, 120.0808, 166.0863, 999};
        double value = SpectralCosine.score(longer, INTENSITY, MZ, INTENSITY, 0.02).value();
        assertEquals(1, value, 1e-12);
    }

    @Test
    void theScoreNeverExceedsOne() {
        double[] mz = {100, 100.001, 100.002};
        double[] intensity = {1, 1, 1};
        assertTrue(SpectralCosine.score(mz, intensity, mz, intensity, 0.5).value() <= 1);
    }
}
