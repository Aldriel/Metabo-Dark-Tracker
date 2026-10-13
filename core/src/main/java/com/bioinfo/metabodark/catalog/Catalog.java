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

package com.bioinfo.metabodark.catalog;

import com.bioinfo.metabodark.model.LibraryEntry;
import com.bioinfo.metabodark.model.MsScan;

/**
 * Optional questions to PubChem, ClassyFire and GNPS. The local annotation does not depend on an
 * answer: a failure leaves the note empty.
 */
@FunctionalInterface
public interface Catalog {

    /**
     * Asks the public databases about one spectrum.
     *
     * @param entry library hit, or null when the spectrum did not match the local library
     * @param scan the measured spectrum
     * @return whatever came back; never null
     */
    CatalogNote describe(LibraryEntry entry, MsScan scan);
}
