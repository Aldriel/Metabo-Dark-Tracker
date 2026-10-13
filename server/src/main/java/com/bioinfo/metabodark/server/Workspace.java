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

package com.bioinfo.metabodark.server;

import com.bioinfo.metabodark.model.AnalysisSettings;

/** One visitor's file. The bytes stay in memory and disappear with the workspace. */
public final class Workspace {

    private final String id;
    private long touchedMillis;
    private byte[] content;
    private String fileName = "";
    private boolean simulated;
    private AnalysisSettings settings = AnalysisSettings.defaults();

    /**
     * Creates an empty workspace.
     *
     * @param id identifier
     * @param touchedMillis clock time of the creation
     */
    public Workspace(String id, long touchedMillis) {
        this.id = id;
        this.touchedMillis = touchedMillis;
    }

    /** Identifier. */
    public String id() {
        return id;
    }

    /** Last activity, in milliseconds. */
    public long touchedMillis() {
        return touchedMillis;
    }

    /** Refreshes the expiry timer. */
    public void touch(long millis) {
        touchedMillis = millis;
    }

    /** Stored file, or null. */
    public byte[] content() {
        return content;
    }

    /** File name. */
    public String fileName() {
        return fileName;
    }

    /** True for the built-in demonstration. */
    public boolean simulated() {
        return simulated;
    }

    /** Current settings. */
    public AnalysisSettings settings() {
        return settings;
    }

    /** Replaces the settings. */
    public void settings(AnalysisSettings settings) {
        this.settings = settings;
    }

    /**
     * Stores a file.
     *
     * @param content bytes
     * @param fileName name
     * @param simulated demonstration flag
     * @param millis clock time
     */
    public void file(byte[] content, String fileName, boolean simulated, long millis) {
        this.content = content.clone();
        this.fileName = fileName;
        this.simulated = simulated;
        touch(millis);
    }
}
