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

import java.util.List;

/**
 * A raw file reduced to its scans.
 *
 * @param fileName name shown in the interface
 * @param format mzML or mzXML
 * @param scans scans in file order
 */
public record SpectralRun(String fileName, FileFormat format, List<MsScan> scans) {

    /** Keeps an immutable copy of the scans. A blank file name becomes {@code upload.mzML}. */
    public SpectralRun {
        if (fileName == null || fileName.isBlank()) {
            fileName = "upload.mzML";
        }
        if (format == null) {
            throw new IllegalArgumentException("FORMAT");
        }
        scans = scans == null ? List.of() : List.copyOf(scans);
    }
}
