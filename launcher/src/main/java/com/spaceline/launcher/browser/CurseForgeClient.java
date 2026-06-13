package com.spaceline.launcher.browser;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.spaceline.common.net.Http;

/**
 * A {@link ContentProvider} backed by the CurseForge Eternal API.
 *
 * <p>CurseForge requires an API key, supplied via the {@code CURSEFORGE_API_KEY}
 * environment variable (or constructor). When no key is configured the client is
 * {@link #isConfigured() inactive} and the launcher simply falls back to Modrinth
 * — searches return empty rather than failing, so the browser still works out of
 * the box.
 */
public final class CurseForgeClient implements ContentProvider {

    private static final String BASE = "https://api.curseforge.com/v1";
    private static final int MINECRAFT_GAME_ID = 432;
    private static final int FABRIC_MOD_LOADER_TYPE = 4;

    private final String apiKey;

    public CurseForgeClient() {
        this(System.getenv("CURSEFORGE_API_KEY"));
    }

    public CurseForgeClient(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String id() {
        return "curseforge";
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public List<SearchResult> search(String query, ProjectType type, String minecraftVersion,
                                     String loaderId, int limit) throws IOException {
        if (!isConfigured()) {
            return List.of();
        }
        StringBuilder url = new StringBuilder(BASE + "/mods/search?gameId=" + MINECRAFT_GAME_ID)
                .append("&classId=").append(type.curseForgeClassId())
                .append("&searchFilter=").append(enc(query))
                .append("&pageSize=").append(limit)
                .append("&sortField=2&sortOrder=desc");
        if (minecraftVersion != null && !minecraftVersion.isBlank()) {
            url.append("&gameVersion=").append(enc(minecraftVersion));
        }
        if ("fabric".equalsIgnoreCase(loaderId) && type == ProjectType.MOD) {
            url.append("&modLoaderType=").append(FABRIC_MOD_LOADER_TYPE);
        }

        JsonObject root = Http.getJson(url.toString(), headers()).getAsJsonObject();
        JsonArray data = root.getAsJsonArray("data");
        List<SearchResult> results = new ArrayList<>();
        for (JsonElement element : data) {
            JsonObject mod = element.getAsJsonObject();
            results.add(new SearchResult(
                    id(),
                    str(mod, "id"),
                    str(mod, "name"),
                    firstAuthor(mod),
                    str(mod, "summary"),
                    mod.has("downloadCount") ? mod.get("downloadCount").getAsLong() : 0,
                    logoUrl(mod),
                    type));
        }
        return results;
    }

    @Override
    public List<ProjectVersion> versions(String projectId, String minecraftVersion,
                                         String loaderId) throws IOException {
        if (!isConfigured()) {
            return List.of();
        }
        String url = BASE + "/mods/" + enc(projectId) + "/files?pageSize=50"
                + "&gameVersion=" + enc(minecraftVersion);
        JsonObject root = Http.getJson(url, headers()).getAsJsonObject();
        JsonArray data = root.getAsJsonArray("data");

        List<ProjectVersion> versions = new ArrayList<>();
        for (JsonElement element : data) {
            JsonObject file = element.getAsJsonObject();
            List<String> gameVersions = stringArray(file.getAsJsonArray("gameVersions"));
            // CurseForge lists loaders alongside game versions in the same array.
            List<String> loaders = gameVersions.stream()
                    .filter(v -> v.equalsIgnoreCase("fabric") || v.equalsIgnoreCase("vanilla"))
                    .map(String::toLowerCase)
                    .toList();
            versions.add(new ProjectVersion(
                    str(file, "id"),
                    str(file, "displayName"),
                    str(file, "downloadUrl"),
                    str(file, "fileName"),
                    "", // CurseForge exposes fingerprints, not a plain sha1 here
                    gameVersions,
                    loaders,
                    List.of()));
        }
        return versions;
    }

    private Map<String, String> headers() {
        return Map.of("x-api-key", apiKey, "Accept", "application/json");
    }

    private static String firstAuthor(JsonObject mod) {
        if (mod.has("authors") && mod.get("authors").isJsonArray()) {
            JsonArray authors = mod.getAsJsonArray("authors");
            if (!authors.isEmpty()) {
                return str(authors.get(0).getAsJsonObject(), "name");
            }
        }
        return "";
    }

    private static String logoUrl(JsonObject mod) {
        if (mod.has("logo") && mod.get("logo").isJsonObject()) {
            return str(mod.getAsJsonObject("logo"), "thumbnailUrl");
        }
        return "";
    }

    private static List<String> stringArray(JsonArray array) {
        List<String> result = new ArrayList<>();
        if (array != null) {
            array.forEach(e -> result.add(e.getAsString()));
        }
        return result;
    }

    private static String str(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        return element != null && !element.isJsonNull() ? element.getAsString() : "";
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
