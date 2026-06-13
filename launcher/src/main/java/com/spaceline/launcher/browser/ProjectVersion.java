package com.spaceline.launcher.browser;

import java.util.List;

/**
 * A specific downloadable version of a project, normalised across providers.
 *
 * @param versionId          provider version id
 * @param name               display name (e.g. "1.2.3 for 1.21.4 Fabric")
 * @param downloadUrl        direct download URL of the primary file
 * @param fileName           the file's name on disk
 * @param sha1               SHA-1 hash for integrity verification, may be empty
 * @param gameVersions       supported Minecraft versions
 * @param loaders            supported loaders (e.g. "fabric")
 * @param dependencyProjects ids of required dependency projects for resolution
 */
public record ProjectVersion(
        String versionId,
        String name,
        String downloadUrl,
        String fileName,
        String sha1,
        List<String> gameVersions,
        List<String> loaders,
        List<String> dependencyProjects) {

    /** Whether this version is compatible with the given MC version + loader. */
    public boolean isCompatible(String minecraftVersion, String loaderId) {
        boolean gameOk = gameVersions.isEmpty() || gameVersions.contains(minecraftVersion);
        boolean loaderOk = loaders.isEmpty() || loaders.contains(loaderId.toLowerCase());
        return gameOk && loaderOk;
    }
}
