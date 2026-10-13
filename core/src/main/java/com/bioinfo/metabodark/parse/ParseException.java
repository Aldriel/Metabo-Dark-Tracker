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

package com.bioinfo.metabodark.parse;

/** A file that is not a readable mzML or mzXML spectrum file. */
public final class ParseException extends RuntimeException {

    /** Stable code the interfaces translate. */
    public enum Code {
        EMPTY,
        TOO_LARGE,
        NOT_XML,
        NO_SPECTRUM,
        BAD_BINARY
    }

    private final Code code;

    /**
     * Creates the failure.
     *
     * @param code stable code
     * @param message short English explanation for the log
     */
    public ParseException(Code code, String message) {
        super(message);
        this.code = code == null ? Code.NOT_XML : code;
    }

    /** Stable code. */
    public Code code() {
        return code;
    }
}
