package com.spaceline.launcher.launch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.spaceline.common.net.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Talks to the Fabric Meta API to resolve loader versions and the launch profile
 * for a given Minecraft version + loader.
 *
 * <p>The profile JSON Fabric returns has the same shape as a vanilla version
 * JSON (mainClass, libraries, arguments), so the launch pipeline can treat a
 * Fabric launch as "vanilla version JSON, plus Fabric's libraries and main
 * class" without special cases beyond fetching it here.
 */
public final class FabricMetaService {

    private static final Logger LOG = LoggerFactory.getLogger(FabricMetaService.class);
    private static final String BASE = "https://meta.fabricmc.net/v2";

    /** Lists stable Fabric loader versions, newest first. */
    public List<String> loaderVersions() throws IOException {
        JsonArray array = Http.getJson(BASE + "/versions/loader").getAsJsonArray();
        List<String> versions = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject loader = element.getAsJsonObject();
            if (loader.has("stable") && loader.get("stable").getAsBoolean()) {
                versions.add(loader.get("version").getAsString());
            }
        }
        return versions;
    }

    /** The newest stable loader version, used as the default for new instances. */
    public String latestStableLoader() throws IOException {
        List<String> versions = loaderVersions();
        if (versions.isEmpty()) {
            throw new IOException("Fabric Meta returned no stable loader versions");
        }
        return versions.get(0);
    }

    /**
     * Fetches the merged Fabric launch profile (vanilla-version-JSON shaped) for a
     * specific Minecraft + loader pairing.
     */
    public JsonObject launchProfile(String minecraftVersion, String loaderVersion) throws IOException {
        String url = BASE + "/versions/loader/" + minecraftVersion + "/" + loaderVersion + "/profile/json";
        LOG.info("Fetching Fabric profile for MC {} loader {}", minecraftVersion, loaderVersion);
        return Http.getJson(url).getAsJsonObject();
    }
}
