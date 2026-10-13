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

import com.bioinfo.metabodark.model.AnalysisSettings;
import com.bioinfo.metabodark.model.AnalysisStatus;
import com.bioinfo.metabodark.model.FileFormat;

import java.util.List;

/**
 * Annotation of one raw file.
 *
 * @param fileName file name
 * @param format mzML or mzXML
 * @param simulated true for the built-in demonstration
 * @param status whether any MS/MS spectrum was found
 * @param ms1 number of MS1 scans
 * @param ms2 number of MS/MS spectra
 * @param libraryHits spectra matched to the reference library or to GNPS
 * @param familyHits spectra classified only by a neutral loss
 * @param dark spectra that stayed unclassified
 * @param settings settings that were used
 * @param annotations one row per MS/MS spectrum
 */
public record Analysis(
        String fileName,
        FileFormat format,
        boolean simulated,
        AnalysisStatus status,
        int ms1,
        int ms2,
        int libraryHits,
        int familyHits,
        int dark,
        AnalysisSettings settings,
        List<Annotation> annotations) {

    /** Keeps an immutable copy of the rows. */
    public Analysis {
        fileName = fileName == null ? "" : fileName;
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }
}
