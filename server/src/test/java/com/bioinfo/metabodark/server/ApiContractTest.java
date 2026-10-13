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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.metabodark.catalog.CatalogNote;
import com.bioinfo.metabodark.example.SimulatedStudy;
import com.bioinfo.metabodark.service.MetaboService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

/** What the web client relies on: the JSON shapes, the headers and the error codes. */
class ApiContractTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static HttpServer server;
    private static String root;

    @BeforeAll
    static void start() throws Exception {
        server =
                ApiServer.create(
                        "127.0.0.1",
                        0,
                        new MetaboService(),
                        new WorkspaceStore(Clock.systemUTC()),
                        (entry, scan) -> new CatalogNote("", "", "", ""));
        server.start();
        root = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stop() {
        server.stop(0);
    }

    @Test
    void healthNamesTheApplicationAndItsVersion() throws Exception {
        JsonNode body = json(get("/api/health"));
        assertEquals("ok", body.get("status").asText());
        assertEquals("Metabo-Dark Tracker", body.get("name").asText());
        assertFalse(body.get("version").asText().isBlank());
    }

    @Test
    void everyAnswerAllowsTheBrowserClient() throws Exception {
        HttpResponse<byte[]> response = get("/api/health");
        assertEquals("*", response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("json"));
    }

    @Test
    void theExampleIsAdvertisedAsSimulated() throws Exception {
        JsonNode example = json(get("/api/examples")).get("examples").get(0);
        assertEquals(SimulatedStudy.ID, example.get("id").asText());
        assertEquals(SimulatedStudy.FILE_NAME, example.get("fileName").asText());
        assertTrue(example.get("simulated").asBoolean());
    }

    @Test
    void anOpenedExampleCarriesItsAnalysisAndDefaultSettings() throws Exception {
        JsonNode workspace = json(openExample());
        assertFalse(workspace.get("id").asText().isBlank());
        assertTrue(workspace.get("simulated").asBoolean());
        JsonNode settings = workspace.get("settings");
        assertEquals(0.02, settings.get("mzToleranceDa").asDouble(), 1e-9);
        assertEquals(0.70, settings.get("cosineMinimum").asDouble(), 1e-9);
        assertFalse(settings.get("queryPublic").asBoolean());
        JsonNode analysis = workspace.get("analysis");
        int ms2 = analysis.get("ms2").asInt();
        assertEquals(ms2, analysis.get("annotations").size());
        assertEquals(
                ms2,
                analysis.get("libraryHits").asInt()
                        + analysis.get("familyHits").asInt()
                        + analysis.get("dark").asInt());
    }

    @Test
    void anEmptyWorkspaceHasNoAnalysisYet() throws Exception {
        HttpResponse<byte[]> response = post("/api/workspaces", "application/json", "{}");
        assertEquals(201, response.statusCode());
        assertTrue(json(response).get("analysis").isNull());
    }

    @Test
    void stricterSettingsAreReflectedInTheAnalysis() throws Exception {
        String id = json(openExample()).get("id").asText();
        HttpResponse<byte[]> response =
                send(
                        "PUT",
                        "/api/workspaces/" + id + "/settings",
                        "application/json",
                        "{\"mzToleranceDa\":0.01,\"cosineMinimum\":0.99,\"queryPublic\":false}");
        JsonNode body = json(response);
        assertEquals(0.99, body.get("settings").get("cosineMinimum").asDouble(), 1e-9);
        JsonNode lenient = json(openExample()).get("analysis");
        assertTrue(
                body.get("analysis").get("libraryHits").asInt()
                        <= lenient.get("libraryHits").asInt());
    }

    @Test
    void theTableDownloadIsTabSeparatedWithOneRowPerSpectrum() throws Exception {
        JsonNode workspace = json(openExample());
        HttpResponse<byte[]> response =
                get("/api/workspaces/" + workspace.get("id").asText() + "/export");
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("tab"));
        String[] lines = new String(response.body(), StandardCharsets.UTF_8).split("\n");
        assertEquals(1 + workspace.get("analysis").get("ms2").asInt(), lines.length);
        assertTrue(lines[0].startsWith("scan\t"));
    }

    @Test
    void theReportIsAnAttachedPdf() throws Exception {
        String id = json(openExample()).get("id").asText();
        HttpResponse<byte[]> response =
                get("/api/workspaces/" + id + "/report?language=fr&title=Run%207");
        assertEquals("application/pdf", response.headers().firstValue("Content-Type").get());
        String disposition = response.headers().firstValue("Content-Disposition").orElse("");
        assertTrue(disposition.startsWith("attachment"));
        assertTrue(new String(response.body(), 0, 5, StandardCharsets.US_ASCII).equals("%PDF-"));
    }

    @Test
    void anUploadedFileIsAnnotatedUnderItsDecodedName() throws Exception {
        HttpResponse<byte[]> response =
                post(
                        "/api/workspaces?name=plasma%20run.mzML",
                        "application/octet-stream",
                        new String(SimulatedStudy.bytes(), StandardCharsets.UTF_8));
        assertEquals(201, response.statusCode());
        JsonNode body = json(response);
        assertEquals("plasma run.mzML", body.get("fileName").asText());
        assertFalse(body.get("simulated").asBoolean());
    }

    @ParameterizedTest(name = "upload \"{0}\" is refused")
    @ValueSource(strings = {"not xml", "<mzML></mzML>", "<html><body/></html>"})
    void anUnreadableUploadReturnsACodeAndNoStackTrace(String content) throws Exception {
        HttpResponse<byte[]> response =
                post("/api/workspaces?name=bad.mzML", "application/octet-stream", content);
        assertEquals(400, response.statusCode());
        JsonNode body = json(response);
        assertFalse(body.get("code").asText().isBlank());
        assertFalse(body.get("message").asText().contains("Exception"));
    }

    @Test
    void anOversizedUploadIsRefused() throws Exception {
        byte[] big = new byte[ApiHandler.MAX_UPLOAD_BYTES + 1];
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(root + "/api/workspaces?name=big.mzML"))
                        .header("Content-Type", "application/octet-stream")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(big))
                        .build();
        HttpResponse<byte[]> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(400, response.statusCode());
        assertEquals("TOO_LARGE", json(response).get("code").asText());
    }

    @Test
    void anUnknownWorkspaceIsNotFound() throws Exception {
        HttpResponse<byte[]> response = get("/api/workspaces/does-not-exist");
        assertEquals(404, response.statusCode());
        assertFalse(json(response).get("code").asText().isBlank());
    }

    private static HttpResponse<byte[]> openExample() throws Exception {
        String body = "{\"example\":\"" + SimulatedStudy.ID + "\"}";
        return post("/api/workspaces", "application/json", body);
    }

    private static HttpResponse<byte[]> get(String path) throws Exception {
        return send("GET", path, null, null);
    }

    private static HttpResponse<byte[]> post(String path, String type, String body)
            throws Exception {
        return send("POST", path, type, body);
    }

    private static HttpResponse<byte[]> send(String method, String path, String type, String body)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(root + path));
        if (type != null) {
            builder.header("Content-Type", type);
        }
        builder.method(
                method,
                body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private static JsonNode json(HttpResponse<byte[]> response) throws Exception {
        return JSON.readTree(response.body());
    }
}
