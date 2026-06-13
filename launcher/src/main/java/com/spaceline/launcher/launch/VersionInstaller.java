package com.spaceline.launcher.launch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.spaceline.common.config.Json;
import com.spaceline.common.net.Http;
import com.spaceline.common.util.Checksums;
import com.spaceline.common.util.SpaceLinePaths;
import com.spaceline.launcher.instance.ModLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Downloads and verifies everything required to launch a Minecraft version:
 * the version JSON, client jar, libraries (honouring OS {@link RuleEvaluator
 * rules}) and assets. For Fabric instances it additionally fetches the Fabric
 * launch profile and merges it over the vanilla JSON.
 *
 * <p>All downloads are integrity-checked against their published SHA-1 and are
 * skipped when an already-correct copy exists, so re-launching is fast and a
 * partially-completed install can resume safely.
 */
public final class VersionInstaller {

    private static final Logger LOG = LoggerFactory.getLogger(VersionInstaller.class);
    private static final String RESOURCES_BASE = "https://resources.download.minecraft.net";

    private final SpaceLinePaths paths;
    private final FabricMetaService fabricMeta;

    public VersionInstaller(SpaceLinePaths paths, FabricMetaService fabricMeta) {
        this.paths = paths;
        this.fabricMeta = fabricMeta;
    }

    /**
     * Ensures the given version (and loader) is fully installed, returning the
     * resolved {@link GameInstallation}.
     *
     * @param minecraftVersion   e.g. "1.21.4"
     * @param manifestUrl        the per-version JSON URL from the version manifest
     * @param loader             vanilla or fabric
     * @param fabricLoaderVersion the Fabric loader version (ignored for vanilla)
     */
    public GameInstallation install(String minecraftVersion, String manifestUrl, ModLoader loader,
                                    String fabricLoaderVersion) throws IOException {
        JsonObject vanillaJson = fetchVersionJson(minecraftVersion, manifestUrl);
        JsonObject effective = vanillaJson;
        String versionId = minecraftVersion;

        if (loader == ModLoader.FABRIC) {
            JsonObject fabricProfile = fabricMeta.launchProfile(minecraftVersion, fabricLoaderVersion);
            effective = mergeFabric(vanillaJson, fabricProfile);
            versionId = "fabric-" + fabricLoaderVersion + "-" + minecraftVersion;
        }

        Path clientJar = downloadClient(minecraftVersion, vanillaJson);
        List<Path> classpath = downloadLibraries(effective);
        classpath.add(clientJar);

        String assetIndex = downloadAssets(vanillaJson);
        Path nativesDir = paths.versions().resolve(minecraftVersion).resolve("natives");
        Files.createDirectories(nativesDir);

        LOG.info("Version {} installed ({} classpath entries)", versionId, classpath.size());
        return new GameInstallation(versionId, effective, clientJar, classpath,
                nativesDir, paths.assets(), assetIndex);
    }

    private JsonObject fetchVersionJson(String version, String manifestUrl) throws IOException {
        Path file = paths.versions().resolve(version).resolve(version + ".json");
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            Http.download(manifestUrl, file);
        }
        return Json.parseObject(Files.readString(file));
    }

    private Path downloadClient(String version, JsonObject versionJson) throws IOException {
        JsonObject client = versionJson.getAsJsonObject("downloads").getAsJsonObject("client");
        Path jar = paths.versions().resolve(version).resolve(version + ".jar");
        downloadIfNeeded(client.get("url").getAsString(), jar,
                client.has("sha1") ? client.get("sha1").getAsString() : "");
        return jar;
    }

    private List<Path> downloadLibraries(JsonObject versionJson) throws IOException {
        List<Path> classpath = new ArrayList<>();
        if (!versionJson.has("libraries")) {
            return classpath;
        }
        for (JsonElement element : versionJson.getAsJsonArray("libraries")) {
            JsonObject library = element.getAsJsonObject();
            JsonArray rules = library.has("rules") ? library.getAsJsonArray("rules") : null;
            if (!RuleEvaluator.applies(rules)) {
                continue;
            }
            Path jar = resolveLibrary(library);
            if (jar != null && !classpath.contains(jar)) {
                classpath.add(jar);
            }
        }
        return classpath;
    }

    /** Downloads a library jar (artifact form, or maven-coordinate fallback). */
    private Path resolveLibrary(JsonObject library) throws IOException {
        if (library.has("downloads")) {
            JsonObject downloads = library.getAsJsonObject("downloads");
            if (downloads.has("artifact")) {
                JsonObject artifact = downloads.getAsJsonObject("artifact");
                Path target = paths.libraries().resolve(artifact.get("path").getAsString());
                downloadIfNeeded(artifact.get("url").getAsString(), target,
                        artifact.has("sha1") ? artifact.get("sha1").getAsString() : "");
                return target;
            }
        }
        // Fabric libraries use a maven coordinate + base url instead of downloads.
        if (library.has("name") && library.has("url")) {
            return downloadMavenLibrary(library.get("name").getAsString(),
                    library.get("url").getAsString());
        }
        return null;
    }

    private Path downloadMavenLibrary(String coordinate, String baseUrl) throws IOException {
        String relative = mavenPath(coordinate);
        Path target = paths.libraries().resolve(relative);
        String url = baseUrl.endsWith("/") ? baseUrl + relative : baseUrl + "/" + relative;
        downloadIfNeeded(url, target, "");
        return target;
    }

    /** Converts {@code group:artifact:version[:classifier]} into a maven path. */
    public static String mavenPath(String coordinate) {
        String[] parts = coordinate.split(":");
        String group = parts[0].replace('.', '/');
        String artifact = parts[1];
        String version = parts[2];
        String classifier = parts.length > 3 ? "-" + parts[3] : "";
        return group + "/" + artifact + "/" + version + "/"
                + artifact + "-" + version + classifier + ".jar";
    }

    private String downloadAssets(JsonObject versionJson) throws IOException {
        JsonObject assetIndexInfo = versionJson.getAsJsonObject("assetIndex");
        String indexId = assetIndexInfo.get("id").getAsString();
        Path indexFile = paths.assets().resolve("indexes").resolve(indexId + ".json");
        downloadIfNeeded(assetIndexInfo.get("url").getAsString(), indexFile,
                assetIndexInfo.has("sha1") ? assetIndexInfo.get("sha1").getAsString() : "");

        JsonObject index = Json.parseObject(Files.readString(indexFile));
        JsonObject objects = index.getAsJsonObject("objects");
        Path objectsDir = paths.assets().resolve("objects");
        int downloaded = 0;
        for (String key : objects.keySet()) {
            String hash = objects.getAsJsonObject(key).get("hash").getAsString();
            String prefix = hash.substring(0, 2);
            Path target = objectsDir.resolve(prefix).resolve(hash);
            if (downloadIfNeeded(RESOURCES_BASE + "/" + prefix + "/" + hash, target, hash)) {
                downloaded++;
            }
        }
        LOG.info("Assets ready (index {}, {} new objects)", indexId, downloaded);
        return indexId;
    }

    /**
     * Downloads {@code url} to {@code target} unless a file with the expected
     * SHA-1 already exists. Returns true if a download actually happened.
     */
    private boolean downloadIfNeeded(String url, Path target, String sha1) throws IOException {
        if (Files.exists(target) && Checksums.matches(target, sha1, "SHA-1")) {
            return false;
        }
        Http.download(url, target);
        if (!Checksums.matches(target, sha1, "SHA-1")) {
            Files.deleteIfExists(target);
            throw new IOException("Checksum mismatch for " + url);
        }
        return true;
    }

    /** Merges a Fabric profile over the vanilla JSON (libraries, mainClass, args). */
    private JsonObject mergeFabric(JsonObject vanilla, JsonObject fabric) {
        JsonObject merged = vanilla.deepCopy();
        merged.add("mainClass", fabric.get("mainClass"));

        JsonArray libraries = merged.getAsJsonArray("libraries");
        if (fabric.has("libraries")) {
            // Fabric libraries go first so the loader sits ahead of the game.
            JsonArray combined = new JsonArray();
            combined.addAll(fabric.getAsJsonArray("libraries"));
            combined.addAll(libraries);
            merged.add("libraries", combined);
        }
        if (fabric.has("arguments") && merged.has("arguments")) {
            mergeArguments(merged.getAsJsonObject("arguments"),
                    fabric.getAsJsonObject("arguments"));
        }
        return merged;
    }

    private void mergeArguments(JsonObject target, JsonObject extra) {
        for (String key : List.of("game", "jvm")) {
            if (extra.has(key)) {
                JsonArray targetArr = target.has(key) ? target.getAsJsonArray(key) : new JsonArray();
                targetArr.addAll(extra.getAsJsonArray(key));
                target.add(key, targetArr);
            }
        }
    }
}
