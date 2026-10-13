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

package com.bioinfo.metabodark.desktop.i18n;

import com.bioinfo.metabodark.model.Language;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

/** Texts of one language. An unknown key is returned as {@code !key!} so a gap is visible. */
public final class Messages {

    private static final String BUNDLE = "com.bioinfo.metabodark.desktop.i18n.messages";

    private final Language language;
    private final ResourceBundle bundle;

    private Messages(Language language) {
        this.language = language;
        Locale locale = language == Language.FR ? Locale.FRENCH : Locale.ENGLISH;
        this.bundle =
                ResourceBundle.getBundle(
                        BUNDLE,
                        locale,
                        ResourceBundle.Control.getNoFallbackControl(
                                ResourceBundle.Control.FORMAT_PROPERTIES));
    }

    /**
     * Loads the bundle of a language.
     *
     * @param language French or English
     * @return the texts
     */
    public static Messages of(Language language) {
        return new Messages(language == null ? Language.EN : language);
    }

    /** Language of this bundle. */
    public Language language() {
        return language;
    }

    /**
     * Returns a text.
     *
     * @param key message key
     * @return the text, or {@code !key!} when the key is missing
     */
    public String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
    }

    /** Every key of this bundle. */
    public Set<String> keys() {
        return bundle.keySet();
    }

    /**
     * Loads the user guide of this language.
     *
     * @return the guide, or a short fallback when the file is missing
     */
    public String guide() {
        String name = language == Language.FR ? "guide_fr.txt" : "guide_en.txt";
        return read(name);
    }

    /**
     * Reads one guide file from the classpath.
     *
     * @param name file name inside the help folder
     * @return the text, or the help title when the file is missing
     */
    public String read(String name) {
        String path = "/com/bioinfo/metabodark/desktop/help/" + name;
        try (var in = Messages.class.getResourceAsStream(path)) {
            if (in == null) {
                return get("help.title");
            }
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            return get("help.title");
        }
    }
}
