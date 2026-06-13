package com.spaceline.launcher.launch;

import java.nio.file.Path;
import java.util.List;

import com.google.gson.JsonObject;

/**
 * The fully-resolved, on-disk artifacts needed to launch a version: the version
 * JSON (already merged with Fabric's profile when applicable), the client jar,
 * the ordered classpath, the extracted natives directory and the asset index id.
 *
 * @param versionId    the effective version id (e.g. "1.21.4" or "fabric-...")
 * @param versionJson  the merged version JSON driving argument/main-class resolution
 * @param clientJar    the Minecraft client jar
 * @param classpath    every jar (libraries + client) for {@code -cp}
 * @param nativesDir   directory containing extracted native libraries
 * @param assetsRoot   the shared assets directory
 * @param assetIndex   the asset index id (e.g. "17")
 */
public record GameInstallation(
        String versionId,
        JsonObject versionJson,
        Path clientJar,
        List<Path> classpath,
        Path nativesDir,
        Path assetsRoot,
        String assetIndex) {
}
