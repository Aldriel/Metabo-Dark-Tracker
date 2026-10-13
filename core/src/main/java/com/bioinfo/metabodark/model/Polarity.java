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

/** Ion mode of a scan. Unknown matches a reference of either mode. */
public enum Polarity {
    POSITIVE,
    NEGATIVE,
    UNKNOWN;

    /**
     * Tells whether a query scan may be compared with a library entry.
     *
     * @param library polarity stored on the reference spectrum
     * @return false when the two modes are known and different
     */
    public boolean compatibleWith(Polarity library) {
        if (this == UNKNOWN || library == UNKNOWN) {
            return true;
        }
        return this == library;
    }
}
