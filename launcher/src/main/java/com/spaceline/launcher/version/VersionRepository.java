package com.spaceline.launcher.version;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.spaceline.common.net.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches Mojang's official version manifest and exposes only the versions
 * Space~line is willing to install: stable {@link VersionType#RELEASE releases}
 * at or beyond {@link MinecraftVersion#MINIMUM_SUPPORTED 1.21}.
 *
 * <p>Snapshots, pre-releases, release candidates and the historical
 * alpha/beta builds are dropped here, at the single source of truth, so no
 * downstream code ever has to re-check "is this a dev build?".
 */
public final class VersionRepository {

    private static final Logger LOG = LoggerFactory.getLogger(VersionRepository.class);
    private static final String MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private final String manifestUrl;
    private List<MinecraftVersion> cache;

    public VersionRepository() {
        this(MANIFEST_URL);
    }

    public VersionRepository(String manifestUrl) {
        this.manifestUrl = manifestUrl;
    }

    /**
     * Returns the supported versions, newest first, fetching and caching the
     * manifest on first call.
     */
    public List<MinecraftVersion> supportedVersions() throws IOException {
        if (cache == null) {
            cache = fetchAndFilter();
        }
        return cache;
    }

    /** Forces a re-fetch on the next {@link #supportedVersions()} call. */
    public void invalidate() {
        cache = null;
    }

    private List<MinecraftVersion> fetchAndFilter() throws IOException {
        LOG.info("Fetching Minecraft version manifest from {}", manifestUrl);
        JsonObject root = Http.getJson(manifestUrl).getAsJsonObject();
        JsonArray versions = root.getAsJsonArray("versions");

        List<MinecraftVersion> result = new ArrayList<>();
        for (JsonElement element : versions) {
            MinecraftVersion version = parse(element.getAsJsonObject());
            if (version.isSupported()) {
                result.add(version);
            }
        }
        result.sort(Comparator.reverseOrder());
        LOG.info("Found {} supported stable releases (>= {})",
                result.size(), MinecraftVersion.MINIMUM_SUPPORTED);
        return List.copyOf(result);
    }

    private static MinecraftVersion parse(JsonObject json) {
        return new MinecraftVersion(
                json.get("id").getAsString(),
                VersionType.fromManifest(optString(json, "type")),
                optString(json, "url"),
                optString(json, "sha1"),
                optString(json, "releaseTime"));
    }

    private static String optString(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return element != null && !element.isJsonNull() ? element.getAsString() : "";
    }
}
