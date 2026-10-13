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

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Workspaces of the visitors currently using the site. A workspace expires after 30 minutes. */
public final class WorkspaceStore {

    /** How long a workspace survives without a request. */
    public static final Duration TIME_TO_LIVE = Duration.ofMinutes(30);

    private final Clock clock;
    private final Map<String, Workspace> workspaces = new ConcurrentHashMap<>();

    /**
     * Creates a store that reads the time from {@code clock}.
     *
     * @param clock clock, so tests can move it forward
     */
    public WorkspaceStore(Clock clock) {
        this.clock = clock;
    }

    /**
     * Opens an empty workspace.
     *
     * @return the new workspace
     */
    public Workspace create() {
        purge();
        Workspace workspace = new Workspace(UUID.randomUUID().toString(), clock.millis());
        workspaces.put(workspace.id(), workspace);
        return workspace;
    }

    /**
     * Returns a workspace that is still alive and refreshes its timer.
     *
     * @param id identifier from the URL
     * @return the workspace
     */
    public Workspace require(String id) {
        Workspace workspace = workspaces.get(id);
        if (workspace == null) {
            throw ApiException.notFound("Unknown workspace.");
        }
        if (clock.millis() - workspace.touchedMillis() > TIME_TO_LIVE.toMillis()) {
            workspaces.remove(id);
            throw ApiException.expired("The workspace has expired.");
        }
        purge();
        workspace.touch(clock.millis());
        return workspace;
    }

    /** Forgets a workspace. An unknown id is ignored. */
    public void delete(String id) {
        workspaces.remove(id);
    }

    /** Current time of the store's clock, in milliseconds. */
    public long now() {
        return clock.millis();
    }

    private void purge() {
        long now = clock.millis();
        workspaces
                .entrySet()
                .removeIf(
                        entry -> now - entry.getValue().touchedMillis() > TIME_TO_LIVE.toMillis());
    }
}
