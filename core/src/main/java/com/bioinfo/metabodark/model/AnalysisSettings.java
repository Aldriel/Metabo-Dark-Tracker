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
 * Knobs of one annotation. The public databases are optional: the local library and the neutral
 * losses always run.
 *
 * @param mzToleranceDa absolute m/z window for peak and precursor matching
 * @param cosineMinimum cosine above which a library hit is accepted
 * @param queryPublic whether PubChem, ClassyFire and GNPS may be called
 */
public record AnalysisSettings(double mzToleranceDa, double cosineMinimum, boolean queryPublic) {

    /** Default window of 0.02 Da and a cosine of 0.70, without remote calls. */
    public static AnalysisSettings defaults() {
        return new AnalysisSettings(0.02, 0.70, false);
    }

    /** Rejects a tolerance or a cosine outside the ranges the matcher can use. */
    public AnalysisSettings {
        if (mzToleranceDa <= 0 || mzToleranceDa > 1) {
            throw new IllegalArgumentException("TOLERANCE");
        }
        if (cosineMinimum < 0 || cosineMinimum > 1) {
            throw new IllegalArgumentException("COSINE");
        }
    }
}
