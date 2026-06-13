package com.spaceline.client.browser;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.spaceline.common.net.Http;

/**
 * A slim, in-game resource-pack browser backed by the public Modrinth API.
 *
 * <p>Lives in the platform-agnostic client engine (only depends on {@code :common})
 * so it compiles and is unit-testable without Minecraft. The Fabric resource-pack
 * screen calls {@link #search} to populate its list and {@link #download} to drop
 * a chosen pack straight into the running instance's {@code resourcepacks} folder.
 */
public final class ResourcePackBrowser {

    private static final String BASE = "https://api.modrinth.com/v2";

    /** A normalized search hit. */
    public record Pack(String projectId, String title, String author, String description,
                       long downloads, String iconUrl) {
    }

    /** Searches Modrinth resource packs compatible with {@code minecraftVersion}. */
    public List<Pack> search(String query, String minecraftVersion) throws IOException {
        String facets = "[[\"project_type:resourcepack\"]"
                + (minecraftVersion == null || minecraftVersion.isBlank()
                        ? "" : ",[\"versions:" + minecraftVersion + "\"]") + "]";
        String url = BASE + "/search?limit=30&query=" + enc(query) + "&facets=" + enc(facets);

        JsonObject root = Http.getJson(url).getAsJsonObject();
        List<Pack> packs = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("hits")) {
            JsonObject hit = element.getAsJsonObject();
            packs.add(new Pack(
                    str(hit, "project_id"),
                    str(hit, "title"),
                    str(hit, "author"),
                    str(hit, "description"),
                    hit.has("downloads") ? hit.get("downloads").getAsLong() : 0,
                    str(hit, "icon_url")));
        }
        return packs;
    }

    /**
     * Downloads the best compatible file of {@code projectId} into {@code resourcePacksDir},
     * returning the installed file path.
     */
    public Path download(String projectId, String minecraftVersion, Path resourcePacksDir) throws IOException {
        String url = BASE + "/project/" + enc(projectId) + "/version"
                + "?game_versions=" + enc("[\"" + minecraftVersion + "\"]");
        JsonArray versions = Http.getJson(url).getAsJsonArray();
        if (versions.isEmpty()) {
            throw new IOException("No compatible version for Minecraft " + minecraftVersion);
        }
        JsonObject version = versions.get(0).getAsJsonObject();
        JsonObject file = pickPrimary(version.getAsJsonArray("files"));
        Path target = resourcePacksDir.resolve(str(file, "filename"));
        Http.download(str(file, "url"), target);
        return target;
    }

    private static JsonObject pickPrimary(JsonArray files) {
        for (JsonElement element : files) {
            JsonObject file = element.getAsJsonObject();
            if (file.has("primary") && file.get("primary").getAsBoolean()) {
                return file;
            }
        }
        return files.get(0).getAsJsonObject();
    }

    private static String str(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        return e != null && !e.isJsonNull() ? e.getAsString() : "";
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
