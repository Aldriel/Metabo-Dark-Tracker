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

/** An HTTP error with a stable code. */
public final class ApiException extends RuntimeException {

    private final int status;
    private final String code;

    private ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /** Unknown route or workspace. */
    public static ApiException notFound(String message) {
        return new ApiException(404, "NOT_FOUND", message);
    }

    /** A workspace that was forgotten after 30 minutes. */
    public static ApiException expired(String message) {
        return new ApiException(410, "EXPIRED", message);
    }

    /** A request the route does not accept. */
    public static ApiException method(String message) {
        return new ApiException(405, "METHOD", message);
    }

    /** A body or a query the pipeline refused. */
    public static ApiException invalid(String code, String message) {
        return new ApiException(400, code, message);
    }

    /** HTTP status. */
    public int status() {
        return status;
    }

    /** Stable code. */
    public String code() {
        return code;
    }
}
