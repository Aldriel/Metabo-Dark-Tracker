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

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/** Save dialog of the operating system. */
public final class NativeDocumentSaver implements DocumentSaver {

    private final Window owner;

    /**
     * Creates the saver.
     *
     * @param owner window that owns the dialogs
     */
    public NativeDocumentSaver(Window owner) {
        this.owner = owner;
    }

    @Override
    public void saveText(String suggestedName, String content) {
        write(suggestedName, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public void saveBytes(String suggestedName, byte[] content) {
        write(suggestedName, content);
    }

    private void write(String suggestedName, byte[] content) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(suggestedName);
        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }
        try {
            Files.write(file.toPath(), content);
        } catch (IOException e) {
            // The status line is updated by the controller only after a successful return.
        }
    }
}
