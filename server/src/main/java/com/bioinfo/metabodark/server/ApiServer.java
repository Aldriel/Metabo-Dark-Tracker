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
import com.bioinfo.metabodark.catalog.HttpCatalog;
import com.bioinfo.metabodark.service.MetaboService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/** JDK HTTP server for the web application. */
public final class ApiServer {

    /** Port used when none is configured. */
    public static final int DEFAULT_PORT = 7074;

    private static final Logger LOG = Logger.getLogger(ApiServer.class.getName());
    private static final String VERSION = readVersion();

    private ApiServer() {}

    /**
     * Builds a server that is not started yet.
     *
     * @param host address to bind
     * @param port port, or 0 for an ephemeral port
     * @param service annotation pipeline
     * @param store workspace store
     * @return the server
     * @throws IOException when the port cannot be bound
     */
    public static HttpServer create(
            String host, int port, MetaboService service, WorkspaceStore store) throws IOException {
        return create(host, port, service, store, HttpCatalog.defaults());
    }

    /**
     * Builds a server with an explicit catalog, so tests do not call the public databases.
     *
     * @param host address to bind
     * @param port port, or 0 for an ephemeral port
     * @param service annotation pipeline
     * @param store workspace store
     * @param catalog public databases
     * @return the server
     * @throws IOException when the port cannot be bound
     */
    public static HttpServer create(
            String host, int port, MetaboService service, WorkspaceStore store, Catalog catalog)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/", new ApiHandler(service, store, catalog, VERSION));
        server.setExecutor(Executors.newCachedThreadPool());
        return server;
    }

    /**
     * Starts the API on {@code 127.0.0.1}.
     *
     * @param args optional port; otherwise the PORT variable, otherwise 7074
     * @throws IOException when the port is already in use
     */
    public static void main(String[] args) throws IOException {
        String port = args.length > 0 ? args[0] : System.getenv("PORT");
        int portNumber = port == null || port.isBlank() ? DEFAULT_PORT : Integer.parseInt(port);
        HttpServer server =
                create(
                        "127.0.0.1",
                        portNumber,
                        new MetaboService(),
                        new WorkspaceStore(Clock.systemUTC()));
        server.start();
        LOG.info(
                () ->
                        "Metabo-Dark Tracker API "
                                + VERSION
                                + " on port "
                                + server.getAddress().getPort());
    }

    private static String readVersion() {
        Properties properties = new Properties();
        try (InputStream in = ApiServer.class.getResourceAsStream("server.properties")) {
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
