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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.metabodark.catalog.CatalogNote;
import com.bioinfo.metabodark.example.SimulatedStudy;
import com.bioinfo.metabodark.service.MetaboService;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

class ApiServerTest {

    @Test
    void theRoutesAnnotateTheExample() throws Exception {
        MutableClock clock = new MutableClock();
        WorkspaceStore store = new WorkspaceStore(clock);
        HttpServer server =
                ApiServer.create(
                        "127.0.0.1",
                        0,
                        new MetaboService(),
                        store,
                        (entry, scan) -> new CatalogNote("", "", "", "Remote"));
        server.start();
        HttpClient client = HttpClient.newHttpClient();
        try {
            String root = "http://127.0.0.1:" + server.getAddress().getPort();
            assertEquals(200, send(client, root, "GET", "/api/health", null, null));
            assertEquals(200, send(client, root, "GET", "/api/examples", null, null));
            assertEquals(204, send(client, root, "OPTIONS", "/api/health", null, null));
            assertEquals(404, send(client, root, "POST", "/api/health", null, null));
            assertEquals(404, send(client, root, "GET", "/api/workspaces/", null, null));
            assertEquals(404, send(client, root, "GET", "/nope", null, null));
            assertEquals(
                    201, send(client, root, "POST", "/api/workspaces", "application/json", "{}"));
            assertEquals(
                    400, send(client, root, "POST", "/api/workspaces", "application/json", "{"));
            assertEquals(
                    400,
                    send(
                            client,
                            root,
                            "POST",
                            "/api/workspaces",
                            "application/json",
                            "{\"example\":\"other\"}"));
            int created =
                    send(
                            client,
                            root,
                            "POST",
                            "/api/workspaces",
                            "application/json",
                            "{\"example\":\" " + SimulatedStudy.ID + " \"}");
            assertEquals(201, created);
            String id = idOf(client, root);
            assertEquals(200, send(client, root, "GET", "/api/workspaces/" + id, null, null));
            assertEquals(
                    405,
                    send(client, root, "PUT", "/api/workspaces/" + id, "application/json", "{}"));
            assertEquals(
                    404, send(client, root, "GET", "/api/workspaces/" + id + "/nope", null, null));
            assertEquals(
                    405,
                    send(client, root, "GET", "/api/workspaces/" + id + "/settings", null, null));
            assertEquals(
                    400,
                    send(
                            client,
                            root,
                            "PUT",
                            "/api/workspaces/" + id + "/settings",
                            "application/json",
                            "{"));
            assertEquals(
                    400,
                    send(
                            client,
                            root,
                            "PUT",
                            "/api/workspaces/" + id + "/settings",
                            "application/json",
                            "{\"mzToleranceDa\":5,\"cosineMinimum\":0.7,\"queryPublic\":false}"));
            assertEquals(
                    200,
                    send(
                            client,
                            root,
                            "PUT",
                            "/api/workspaces/" + id + "/settings",
                            "application/json",
                            "{\"mzToleranceDa\":0.02,\"cosineMinimum\":0.7,\"queryPublic\":true}"));
            assertEquals(
                    200,
                    send(client, root, "GET", "/api/workspaces/" + id + "/export?", null, null));
            assertTrue(
                    send(
                                    client,
                                    root,
                                    "GET",
                                    "/api/workspaces/" + id + "/report?language=fr&title=Run",
                                    null,
                                    null)
                            == 200);
            assertEquals(
                    201,
                    send(
                            client,
                            root,
                            "POST",
                            "/api/workspaces?name=demo.mzML",
                            "application/xml",
                            demo()));
            assertEquals(
                    400,
                    send(client, root, "POST", "/api/workspaces?name", "application/xml", "nope"));
            assertEquals(404, send(client, root, "POST", "/api/examples", null, null));
            assertEquals(404, send(client, root, "GET", "/api/workspaces", null, null));
            assertEquals(201, send(client, root, "POST", "/api/workspaces", null, null));
            assertEquals(
                    201, send(client, root, "POST", "/api/workspaces", "application/json", ""));
            assertEquals(
                    201,
                    send(
                            client,
                            root,
                            "POST",
                            "/api/workspaces",
                            "application/json",
                            "{\"example\":\" \"}"));
            assertEquals(
                    404,
                    send(
                            client,
                            root,
                            "GET",
                            "/api/workspaces/" + id + "/export/extra",
                            null,
                            null));
            assertEquals(
                    404,
                    send(
                            client,
                            root,
                            "PUT",
                            "/api/workspaces/" + id + "/export",
                            "application/json",
                            "{}"));
            assertEquals(
                    200,
                    send(client, root, "GET", "/api/workspaces/" + id + "/report", null, null));
            assertEquals(
                    200,
                    send(client, root, "GET", "/api/workspaces/" + id + "/report?%20", null, null));
            String empty = idIn(client, root, "{}");
            assertEquals(
                    400,
                    send(
                            client,
                            root,
                            "PUT",
                            "/api/workspaces/" + empty + "/settings",
                            "application/json",
                            "{\"mzToleranceDa\":0.02,\"cosineMinimum\":0.7,"
                                    + "\"queryPublic\":false}"));
            assertEquals(
                    400,
                    send(client, root, "GET", "/api/workspaces/" + empty + "/export", null, null));
            assertEquals(
                    400,
                    send(
                            client,
                            root,
                            "POST",
                            "/api/workspaces",
                            "application/json",
                            " ".repeat(1024 * 1024 + 1)));
            assertEquals(204, send(client, root, "DELETE", "/api/workspaces/" + id, null, null));
            assertEquals(404, send(client, root, "GET", "/api/workspaces/" + id, null, null));
            Workspace first = store.create();
            clock.plus(Duration.ofMinutes(31));
            store.create();
            ApiException missing =
                    assertThrows(ApiException.class, () -> store.require(first.id()));
            assertEquals(404, missing.status());
            Workspace fresh = store.create();
            clock.plus(Duration.ofMinutes(31));
            ApiException expired =
                    assertThrows(ApiException.class, () -> store.require(fresh.id()));
            assertEquals(410, expired.status());
            store.delete("absent");
            assertEquals(400, ApiException.invalid("C", "m").status());
            assertEquals("METHOD", ApiException.method("no").code());
        } finally {
            server.stop(0);
        }
    }

    private static int send(
            HttpClient client, String root, String method, String path, String type, String body)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(root + path));
        if (type != null) {
            builder.header("Content-Type", type);
        }
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    @Test
    void aFailingCatalogBecomesAnError() throws Exception {
        HttpServer server =
                ApiServer.create(
                        "127.0.0.1",
                        0,
                        new MetaboService(),
                        new WorkspaceStore(Clock.systemUTC()),
                        (entry, scan) -> {
                            throw new IllegalStateException("down");
                        });
        server.start();
        HttpClient client = HttpClient.newHttpClient();
        try {
            String root = "http://127.0.0.1:" + server.getAddress().getPort();
            String id = idIn(client, root, "{\"example\":\"" + SimulatedStudy.ID + "\"}");
            assertEquals(
                    500,
                    send(
                            client,
                            root,
                            "PUT",
                            "/api/workspaces/" + id + "/settings",
                            "application/json",
                            "{\"mzToleranceDa\":0.02,\"cosineMinimum\":0.7,\"queryPublic\":true}"));
        } finally {
            server.stop(0);
        }
    }

    private static String idOf(HttpClient client, String root) throws Exception {
        return idIn(client, root, "{\"example\":\"" + SimulatedStudy.ID + "\"}");
    }

    private static String idIn(HttpClient client, String root, String json) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(root + "/api/workspaces"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
        String text = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        int at = text.indexOf("\"id\":\"");
        return text.substring(at + 6, text.indexOf('"', at + 6));
    }

    private static String demo() {
        return new String(SimulatedStudy.bytes(), java.nio.charset.StandardCharsets.UTF_8);
    }

    /** A clock the test can move forward. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        private void plus(Duration duration) {
            now = now.plus(duration);
        }
    }
}
