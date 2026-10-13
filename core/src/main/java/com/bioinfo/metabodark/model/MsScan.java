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

package com.bioinfo.metabodark.model;

/**
 * One scan. Fragment arrays are parallel and may be empty. Level 2 is an MS/MS spectrum.
 *
 * @param index position in the file, starting at 1
 * @param level MS level (1 is a full scan, 2 is a fragment spectrum)
 * @param retentionSeconds retention time in seconds
 * @param precursorMz selected precursor, or NaN when the scan has none
 * @param charge precursor charge, or 0 when it was not written
 * @param polarity ion mode
 * @param mz fragment or profile m/z values
 * @param intensity intensities paired with {@code mz}
 */
public record MsScan(
        int index,
        int level,
        double retentionSeconds,
        double precursorMz,
        int charge,
        Polarity polarity,
        double[] mz,
        double[] intensity) {

    /** Copies the peak arrays so a caller cannot change a stored scan. */
    public MsScan {
        polarity = polarity == null ? Polarity.UNKNOWN : polarity;
        mz = mz == null ? new double[0] : mz.clone();
        intensity = intensity == null ? new double[0] : intensity.clone();
        int peaks = Math.min(mz.length, intensity.length);
        if (peaks != mz.length) {
            mz = java.util.Arrays.copyOf(mz, peaks);
        }
        if (peaks != intensity.length) {
            intensity = java.util.Arrays.copyOf(intensity, peaks);
        }
    }

    /** Fragment spectra are the ones the pipeline annotates. */
    public boolean ms2() {
        return level >= 2 && Double.isFinite(precursorMz) && precursorMz > 0;
    }
}
