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

package com.bioinfo.metabodark.desktop.prefs;

import com.bioinfo.metabodark.model.Language;

import java.util.prefs.Preferences;

/** Language and text size remembered between sessions. */
public final class UserPreferences {

    /** Smallest text size, in pixels. */
    public static final int MIN_FONT = 10;

    /** Largest text size, in pixels. */
    public static final int MAX_FONT = 24;

    /** Size used the first time, and by the reset command. */
    public static final int DEFAULT_FONT = 13;

    private static final String LANGUAGE = "language";
    private static final String FONT = "fontSize";

    private final Preferences node;

    /** Preferences of the real user. */
    public UserPreferences() {
        this(Preferences.userRoot().node("com/bioinfo/metabodark/desktop"));
    }

    /**
     * Preferences stored in the given node, so tests do not touch the user's settings.
     *
     * @param node preference node
     */
    public UserPreferences(Preferences node) {
        this.node = node;
    }

    /** Remembered language. An unknown value becomes English. */
    public Language language() {
        return Language.parse(node.get(LANGUAGE, Language.EN.name()));
    }

    /** Remembers the language. */
    public void language(Language language) {
        node.put(LANGUAGE, (language == null ? Language.EN : language).name());
    }

    /** Remembered text size, limited to the allowed range. */
    public int fontSize() {
        return clamp(node.getInt(FONT, DEFAULT_FONT));
    }

    /** Remembers the text size. */
    public void fontSize(int size) {
        node.putInt(FONT, clamp(size));
    }

    /**
     * Limits a size to [{@value #MIN_FONT}, {@value #MAX_FONT}].
     *
     * @param size requested size
     * @return the size that will actually be used
     */
    public static int clamp(int size) {
        return Math.clamp(size, MIN_FONT, MAX_FONT);
    }
}
