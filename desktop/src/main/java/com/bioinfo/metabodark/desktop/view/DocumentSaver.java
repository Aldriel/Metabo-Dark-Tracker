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

package com.bioinfo.metabodark.desktop.view;

/** Saves a report or a results table. A test can record the bytes instead of opening a dialog. */
public interface DocumentSaver {

    /**
     * Saves text. A cancel does nothing.
     *
     * @param suggestedName proposed file name
     * @param content the text
     */
    void saveText(String suggestedName, String content);

    /**
     * Saves bytes. A cancel does nothing.
     *
     * @param suggestedName proposed file name
     * @param content the bytes
     */
    void saveBytes(String suggestedName, byte[] content);
}
