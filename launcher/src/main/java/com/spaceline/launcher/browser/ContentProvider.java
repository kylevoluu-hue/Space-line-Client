package com.spaceline.launcher.browser;

import java.io.IOException;
import java.util.List;

/**
 * Common interface implemented by both the Modrinth and CurseForge clients so
 * the rest of the launcher (search UI, one-click install, update checker) can be
 * written once against a single abstraction.
 */
public interface ContentProvider {

    /** A stable provider id, e.g. {@code "modrinth"}. */
    String id();

    /**
     * Searches for content of {@code type} matching {@code query}, filtered to
     * the given Minecraft version and loader where the API supports it.
     */
    List<SearchResult> search(String query, ProjectType type,
                              String minecraftVersion, String loaderId, int limit) throws IOException;

    /**
     * Lists the available versions of a project, newest first. The caller filters
     * with {@link ProjectVersion#isCompatible(String, String)}.
     */
    List<ProjectVersion> versions(String projectId, String minecraftVersion, String loaderId) throws IOException;

    /** Resolves the best compatible version, if any, for one-click install. */
    default java.util.Optional<ProjectVersion> bestVersion(String projectId, String minecraftVersion,
                                                            String loaderId) throws IOException {
        return versions(projectId, minecraftVersion, loaderId).stream()
                .filter(v -> v.isCompatible(minecraftVersion, loaderId))
                .findFirst();
    }
}
