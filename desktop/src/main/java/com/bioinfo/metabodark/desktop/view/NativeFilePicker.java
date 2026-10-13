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

/** File dialog of the operating system. */
public final class NativeFilePicker implements FilePicker {

    private final Window owner;

    /**
     * Creates the picker.
     *
     * @param owner window that owns the dialog
     */
    public NativeFilePicker(Window owner) {
        this.owner = owner;
    }

    @Override
    public PickedFile open() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Metabo-Dark Tracker");
        chooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("mzML or mzXML", "*.mzML", "*.mzXML", "*.gz"));
        File file = chooser.showOpenDialog(owner);
        if (file == null) {
            return null;
        }
        try {
            return new PickedFile(file.getName(), Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            return null;
        }
    }
}
