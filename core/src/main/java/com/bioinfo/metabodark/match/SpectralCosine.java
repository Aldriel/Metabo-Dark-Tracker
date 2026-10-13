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

import java.util.Arrays;

/**
 * Cosine similarity of two fragment spectra, the score used by GNPS. Intensities are square-rooted
 * before the comparison, and a peak may match only one peak of the other spectrum.
 */
public final class SpectralCosine {

    private SpectralCosine() {}

    /**
     * The score and how many peaks were paired.
     *
     * @param value cosine from 0 to 1
     * @param matched number of paired peaks
     */
    public record Score(double value, int matched) {}

    /**
     * Compares two spectra. Peaks further apart than {@code toleranceDa} do not match.
     *
     * @param queryMz query m/z
     * @param queryIntensity query intensity
     * @param libraryMz library m/z
     * @param libraryIntensity library intensity
     * @param toleranceDa absolute window
     * @return the score; empty spectra score 0
     */
    public static Score score(
            double[] queryMz,
            double[] queryIntensity,
            double[] libraryMz,
            double[] libraryIntensity,
            double toleranceDa) {
        int queryPeaks = Math.min(queryMz.length, queryIntensity.length);
        int libraryPeaks = Math.min(libraryMz.length, libraryIntensity.length);
        if (queryPeaks == 0 || libraryPeaks == 0) {
            return new Score(0, 0);
        }
        int[] queryOrder = order(queryMz, queryPeaks);
        int[] libraryOrder = order(libraryMz, libraryPeaks);
        double queryNorm = 0;
        double libraryNorm = 0;
        for (int i = 0; i < queryPeaks; i++) {
            queryNorm += queryIntensity[i];
        }
        for (int i = 0; i < libraryPeaks; i++) {
            libraryNorm += libraryIntensity[i];
        }
        if (queryNorm <= 0 || libraryNorm <= 0) {
            return new Score(0, 0);
        }
        double dot = 0;
        int matched = 0;
        int libraryCursor = 0;
        for (int q = 0; q < queryPeaks; q++) {
            int queryIndex = queryOrder[q];
            double mz = queryMz[queryIndex];
            while (libraryCursor < libraryPeaks
                    && libraryMz[libraryOrder[libraryCursor]] < mz - toleranceDa) {
                libraryCursor++;
            }
            if (libraryCursor < libraryPeaks
                    && Math.abs(libraryMz[libraryOrder[libraryCursor]] - mz) <= toleranceDa) {
                int libraryIndex = libraryOrder[libraryCursor];
                dot +=
                        Math.sqrt(queryIntensity[queryIndex])
                                * Math.sqrt(libraryIntensity[libraryIndex]);
                matched++;
                libraryCursor++;
            }
        }
        double value = dot / Math.sqrt(queryNorm * libraryNorm);
        if (value > 1) {
            value = 1;
        }
        return new Score(value, matched);
    }

    private static int[] order(double[] mz, int peaks) {
        Integer[] boxed = new Integer[peaks];
        for (int i = 0; i < peaks; i++) {
            boxed[i] = i;
        }
        Arrays.sort(boxed, (left, right) -> Double.compare(mz[left], mz[right]));
        int[] indexes = new int[peaks];
        for (int i = 0; i < peaks; i++) {
            indexes[i] = boxed[i];
        }
        return indexes;
    }
}
