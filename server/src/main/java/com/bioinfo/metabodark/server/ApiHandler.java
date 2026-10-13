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

import com.bioinfo.metabodark.catalog.Catalog;
import com.bioinfo.metabodark.example.SimulatedStudy;
import com.bioinfo.metabodark.model.AnalysisSettings;
import com.bioinfo.metabodark.model.Language;
import com.bioinfo.metabodark.parse.ParseException;
import com.bioinfo.metabodark.report.PdfReportWriter;
import com.bioinfo.metabodark.report.ResultTableWriter;
import com.bioinfo.metabodark.service.Analysis;
import com.bioinfo.metabodark.service.MetaboService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Routes of the REST API. Files are held only in the workspace store. */
public final class ApiHandler implements HttpHandler {

    /** Largest accepted upload. */
    public static final int MAX_UPLOAD_BYTES = 40 * 1024 * 1024;

    private static final int MAX_JSON = 1024 * 1024;

    private final MetaboService service;
    private final WorkspaceStore store;
    private final Catalog catalog;
    private final String version;
    private final ObjectMapper json = new ObjectMapper();

    /**
     * Creates the handler.
     *
     * @param service local pipeline
     * @param store workspaces
     * @param catalog public databases, used only when the visitor asks for them
     * @param version application version
     */
    public ApiHandler(
            MetaboService service, WorkspaceStore store, Catalog catalog, String version) {
        this.service = service;
        this.store = store;
        this.catalog = catalog;
        this.version = version;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders()
                    .set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                send(exchange, 204, new byte[0], "text/plain");
                return;
            }
            route(exchange);
        } catch (ApiException e) {
            sendJson(exchange, e.status(), Map.of("code", e.code(), "message", e.getMessage()));
        } catch (RuntimeException e) {
            sendJson(exchange, 500, Map.of("code", "ERROR", "message", "The request failed."));
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        if ("/api/health".equals(path) && "GET".equals(method)) {
            sendJson(
                    exchange,
                    200,
                    Map.of("status", "ok", "name", "Metabo-Dark Tracker", "version", version));
            return;
        }
        if ("/api/examples".equals(path) && "GET".equals(method)) {
            sendJson(exchange, 200, Map.of("examples", List.of(example())));
            return;
        }
        if ("/api/workspaces".equals(path) && "POST".equals(method)) {
            sendJson(exchange, 201, view(open(exchange)));
            return;
        }
        if (!path.startsWith("/api/workspaces/")) {
            throw ApiException.notFound("Unknown route.");
        }
        String[] parts = path.substring("/api/workspaces/".length()).split("/");
        if (parts[0].isBlank()) {
            throw ApiException.notFound("Unknown route.");
        }
        if (parts.length == 1) {
            workspace(exchange, method, parts[0]);
            return;
        }
        if (parts.length == 2) {
            action(exchange, method, parts[0], parts[1]);
            return;
        }
        throw ApiException.notFound("Unknown route.");
    }

    private void workspace(HttpExchange exchange, String method, String id) throws IOException {
        if ("GET".equals(method)) {
            sendJson(exchange, 200, view(store.require(id)));
        } else if ("DELETE".equals(method)) {
            store.delete(id);
            send(exchange, 204, new byte[0], "text/plain");
        } else {
            throw ApiException.method("Use GET or DELETE on a workspace.");
        }
    }

    private void action(HttpExchange exchange, String method, String id, String action)
            throws IOException {
        Workspace workspace = store.require(id);
        if ("settings".equals(action) && "PUT".equals(method)) {
            AnalysisSettings settings = readSettings(read(exchange, MAX_JSON));
            if (workspace.content() == null) {
                throw ApiException.invalid("NO_FILE", "Open a file first.");
            }
            workspace.settings(settings);
            sendJson(exchange, 200, view(workspace));
            return;
        }
        if ("export".equals(action) && "GET".equals(method)) {
            send(
                    exchange,
                    200,
                    ResultTableWriter.write(analysis(workspace)),
                    "text/tab-separated-values");
            return;
        }
        if ("report".equals(action) && "GET".equals(method)) {
            Map<String, String> query = query(exchange.getRequestURI().getRawQuery());
            byte[] pdf =
                    PdfReportWriter.write(
                            analysis(workspace),
                            Language.parse(query.get("language")),
                            query.get("title"));
            exchange.getResponseHeaders()
                    .set("Content-Disposition", "attachment; filename=\"Metabo-Dark.pdf\"");
            send(exchange, 200, pdf, "application/pdf");
            return;
        }
        if (!"PUT".equals(method) && "settings".equals(action)) {
            throw ApiException.method("Use PUT for the settings.");
        }
        throw ApiException.notFound("Unknown route.");
    }

    private Workspace open(HttpExchange exchange) throws IOException {
        String type = exchange.getRequestHeaders().getFirst("Content-Type");
        byte[] body =
                read(exchange, type != null && type.contains("json") ? MAX_JSON : MAX_UPLOAD_BYTES);
        Workspace workspace = store.create();
        if (type != null && type.contains("json")) {
            openExample(workspace, body);
            return workspace;
        }
        if (body.length == 0) {
            return workspace;
        }
        String name =
                query(exchange.getRequestURI().getRawQuery()).getOrDefault("name", "upload.mzML");
        try {
            service.analyze(body, name, workspace.settings(), false);
        } catch (ParseException e) {
            throw ApiException.invalid(e.code().name(), e.getMessage());
        }
        workspace.file(body, name, false, store.now());
        return workspace;
    }

    private void openExample(Workspace workspace, byte[] body) {
        if (body.length == 0) {
            return;
        }
        JsonNode node = readTree(body);
        JsonNode example = node.get("example");
        if (example == null || example.asText().isBlank()) {
            return;
        }
        if (!SimulatedStudy.ID.equals(example.asText().trim())) {
            throw ApiException.invalid("UNKNOWN_EXAMPLE", "Unknown example.");
        }
        workspace.file(SimulatedStudy.bytes(), SimulatedStudy.FILE_NAME, true, store.now());
    }

    private Analysis analysis(Workspace workspace) {
        if (workspace.content() == null) {
            throw ApiException.invalid("NO_FILE", "Open a file first.");
        }
        MetaboService pipeline =
                workspace.settings().queryPublic()
                        ? new MetaboService(library(), catalog)
                        : service;
        return pipeline.analyze(
                workspace.content(),
                workspace.fileName(),
                workspace.settings(),
                workspace.simulated());
    }

    private List<com.bioinfo.metabodark.model.LibraryEntry> library() {
        return com.bioinfo.metabodark.library.ReferenceLibrary.entries();
    }

    private AnalysisSettings readSettings(byte[] body) {
        try {
            return json.readValue(body, AnalysisSettings.class);
        } catch (IOException e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                throw ApiException.invalid("SETTINGS", "The settings are out of range.");
            }
            throw ApiException.invalid("JSON", "The settings are not valid JSON.");
        }
    }

    private JsonNode readTree(byte[] body) {
        try {
            return json.readTree(body);
        } catch (IOException e) {
            throw ApiException.invalid("JSON", "The request is not valid JSON.");
        }
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> values = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return values;
        }
        for (String part : raw.split("&")) {
            int eq = part.indexOf('=');
            if (eq < 0) {
                values.put(decode(part), "");
            } else {
                values.put(decode(part.substring(0, eq)), decode(part.substring(eq + 1)));
            }
        }
        return values;
    }

    private static String decode(String text) {
        return URLDecoder.decode(text, StandardCharsets.UTF_8);
    }

    private static Map<String, Object> example() {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("id", SimulatedStudy.ID);
        example.put("fileName", SimulatedStudy.FILE_NAME);
        example.put("simulated", true);
        return example;
    }

    private Map<String, Object> view(Workspace workspace) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", workspace.id());
        body.put("fileName", workspace.fileName());
        body.put("simulated", workspace.simulated());
        body.put("settings", workspace.settings());
        body.put("analysis", workspace.content() == null ? null : analysis(workspace));
        return body;
    }

    private static byte[] read(HttpExchange exchange, int limit) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            byte[] body = in.readNBytes(limit + 1);
            if (body.length > limit) {
                throw ApiException.invalid("TOO_LARGE", "The file is too large.");
            }
            return body;
        }
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        send(exchange, status, json.writeValueAsBytes(body), "application/json");
    }

    private static void send(HttpExchange exchange, int status, byte[] body, String type)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
