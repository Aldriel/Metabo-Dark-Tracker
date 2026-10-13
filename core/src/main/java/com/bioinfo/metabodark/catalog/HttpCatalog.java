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

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Calls PubChem, ClassyFire and a GNPS-style search. Each call is independent: one service that
 * does not answer does not cancel the others. A null address skips that service.
 */
public final class HttpCatalog implements Catalog {

    private final String pubchem;
    private final String classyFire;
    private final String gnps;
    private final HttpClient client;

    /**
     * Creates the client. Addresses have no trailing slash.
     *
     * @param pubchem PubChem PUG root, or null
     * @param classyFire ClassyFire root, or null
     * @param gnps GNPS search root, or null
     * @param client HTTP client, already configured with a short timeout
     */
    public HttpCatalog(String pubchem, String classyFire, String gnps, HttpClient client) {
        this.pubchem = blankToNull(pubchem);
        this.classyFire = blankToNull(classyFire);
        this.gnps = blankToNull(gnps);
        this.client = client;
    }

    /** A client that gives each public service three seconds. */
    public static HttpCatalog defaults() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        return new HttpCatalog(
                "https://pubchem.ncbi.nlm.nih.gov/rest/pug",
                "http://classyfire.wishartlab.com",
                "https://gnps-external.ucsd.edu/gnpslibrary",
                client);
    }

    @Override
    public CatalogNote describe(LibraryEntry entry, MsScan scan) {
        String title = "";
        String formula = "";
        String chemicalClass = "";
        String gnpsName = "";
        if (entry != null && pubchem != null && !entry.inchikey().isBlank()) {
            String key = URLEncoder.encode(entry.inchikey(), StandardCharsets.UTF_8);
            String path = "/compound/inchikey/" + key + "/property/Title,MolecularFormula/JSON";
            String body = get(pubchem + path);
            title = jsonString(body, "Title");
            formula = jsonString(body, "MolecularFormula");
        }
        if (entry != null && classyFire != null && !entry.inchikey().isBlank()) {
            String query =
                    "{\"label\":\"metabo\",\"query_input\":\""
                            + entry.inchikey()
                            + "\",\"query_type\":\"INCHIKEY\"}";
            chemicalClass = jsonString(post(classyFire + "/queries.json", query), "name");
        }
        if (gnps != null && scan != null) {
            gnpsName = jsonString(post(gnps + "/search", spectrumJson(scan)), "name");
        }
        return new CatalogNote(title, formula, chemicalClass, gnpsName);
    }

    /**
     * Reads a JSON string field. Nested objects are not interpreted; the first matching key wins.
     *
     * @param json response body, possibly empty
     * @param field field name
     * @return the text, or blank
     */
    public static String jsonString(String json, String field) {
        if (json == null || field == null) {
            return "";
        }
        String key = "\"" + field + "\"";
        int at = json.indexOf(key);
        if (at < 0) {
            return "";
        }
        int colon = json.indexOf(':', at + key.length());
        if (colon < 0) {
            return "";
        }
        int start = json.indexOf('"', colon + 1);
        if (start < 0) {
            return "";
        }
        int end = json.indexOf('"', start + 1);
        if (end < 0) {
            return "";
        }
        return json.substring(start + 1, end);
    }

    private String get(String url) {
        return send(HttpRequest.newBuilder(URI.create(url)).GET().build());
    }

    private String post(String url, String body) {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
        return send(request);
    }

    private String send(HttpRequest request) {
        try {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return "";
            }
            return response.body() == null ? "" : response.body();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return "";
        }
    }

    private static String spectrumJson(MsScan scan) {
        StringBuilder text = new StringBuilder();
        text.append("{\"precursor\":").append(scan.precursorMz()).append(",\"peaks\":[");
        int peaks = Math.min(scan.mz().length, 12);
        for (int i = 0; i < peaks; i++) {
            if (i > 0) {
                text.append(',');
            }
            text.append('[')
                    .append(scan.mz()[i])
                    .append(',')
                    .append(scan.intensity()[i])
                    .append(']');
        }
        text.append("]}");
        return text.toString();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
