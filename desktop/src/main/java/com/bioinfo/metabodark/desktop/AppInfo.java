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

package com.bioinfo.metabodark.desktop;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/** Name, version and the addresses printed in the footer. */
public final class AppInfo {

    /** Application name. */
    public static final String NAME = "Metabo-Dark Tracker";

    /** Contact address. */
    public static final String CONTACT_EMAIL = "contact@maximeethier.com";

    /** Personal site, French pages. */
    public static final String WEBSITE_FR = "https://www.maximeethier.com";

    /** Personal site, English pages. */
    public static final String WEBSITE_EN = "https://www.maximeethier.com/en";

    /** Ko-fi page. The button does not load a script from Ko-fi. */
    public static final String KOFI = "https://ko-fi.com/K1S228CL7A";

    private static final String VERSION = readVersion();

    private AppInfo() {}

    /** Version written by Maven, or {@code dev} when the file is missing. */
    public static String version() {
        return VERSION;
    }

    private static String readVersion() {
        Properties properties = new Properties();
        try (InputStream in = AppInfo.class.getResourceAsStream("app.properties")) {
            if (in == null) {
                return "dev";
            }
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties.getProperty("version", "dev");
    }
}
