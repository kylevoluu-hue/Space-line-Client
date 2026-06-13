package com.spaceline.launcher.browser;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.spaceline.common.net.Http;

/**
 * A {@link ContentProvider} backed by the public Modrinth v2 API.
 *
 * <p>Search uses Modrinth's facet system to constrain results to the requested
 * project type, Minecraft version and loader, so only compatible content is ever
 * shown. No API key is required for read access; the shared {@code User-Agent}
 * identifies Space~line per Modrinth's etiquette guidelines.
 */
public final class ModrinthClient implements ContentProvider {

    private static final String BASE = "https://api.modrinth.com/v2";

    @Override
    public String id() {
        return "modrinth";
    }

    @Override
    public List<SearchResult> search(String query, ProjectType type, String minecraftVersion,
                                     String loaderId, int limit) throws IOException {
        String facets = buildFacets(type, minecraftVersion, loaderId);
        String url = BASE + "/search?query=" + enc(query)
                + "&facets=" + enc(facets)
                + "&limit=" + limit;

        JsonObject root = Http.getJson(url).getAsJsonObject();
        JsonArray hits = root.getAsJsonArray("hits");
        List<SearchResult> results = new ArrayList<>();
        for (JsonElement element : hits) {
            JsonObject hit = element.getAsJsonObject();
            results.add(new SearchResult(
                    id(),
                    str(hit, "project_id"),
                    str(hit, "title"),
                    str(hit, "author"),
                    str(hit, "description"),
                    hit.has("downloads") ? hit.get("downloads").getAsLong() : 0,
                    str(hit, "icon_url"),
                    type));
        }
        return results;
    }

    @Override
    public List<ProjectVersion> versions(String projectId, String minecraftVersion,
                                         String loaderId) throws IOException {
        String url = BASE + "/project/" + enc(projectId) + "/version"
                + "?game_versions=" + enc("[\"" + minecraftVersion + "\"]")
                + "&loaders=" + enc("[\"" + loaderId + "\"]");

        JsonArray array = Http.getJson(url).getAsJsonArray();
        List<ProjectVersion> versions = new ArrayList<>();
        for (JsonElement element : array) {
            versions.add(parseVersion(element.getAsJsonObject()));
        }
        return versions;
    }

    private ProjectVersion parseVersion(JsonObject json) {
        JsonArray files = json.getAsJsonArray("files");
        JsonObject primary = pickPrimaryFile(files);

        List<String> gameVersions = stringArray(json.getAsJsonArray("game_versions"));
        List<String> loaders = stringArray(json.getAsJsonArray("loaders"));
        List<String> dependencies = new ArrayList<>();
        if (json.has("dependencies")) {
            for (JsonElement dep : json.getAsJsonArray("dependencies")) {
                JsonObject depObj = dep.getAsJsonObject();
                if ("required".equals(str(depObj, "dependency_type")) && depObj.has("project_id")
                        && !depObj.get("project_id").isJsonNull()) {
                    dependencies.add(depObj.get("project_id").getAsString());
                }
            }
        }

        String sha1 = primary.has("hashes")
                ? str(primary.getAsJsonObject("hashes"), "sha1") : "";
        return new ProjectVersion(
                str(json, "id"),
                str(json, "name"),
                str(primary, "url"),
                str(primary, "filename"),
                sha1,
                gameVersions,
                loaders,
                dependencies);
    }

    private static JsonObject pickPrimaryFile(JsonArray files) {
        for (JsonElement element : files) {
            JsonObject file = element.getAsJsonObject();
            if (file.has("primary") && file.get("primary").getAsBoolean()) {
                return file;
            }
        }
        return files.isEmpty() ? new JsonObject() : files.get(0).getAsJsonObject();
    }

    private static String buildFacets(ProjectType type, String minecraftVersion, String loaderId) {
        // Modrinth facets are an array of AND-ed arrays of OR-ed conditions.
        StringBuilder sb = new StringBuilder("[");
        sb.append("[\"project_type:").append(type.modrinthFacet()).append("\"]");
        if (minecraftVersion != null && !minecraftVersion.isBlank()) {
            sb.append(",[\"versions:").append(minecraftVersion).append("\"]");
        }
        if (loaderId != null && !loaderId.isBlank() && type != ProjectType.RESOURCE_PACK) {
            sb.append(",[\"categories:").append(loaderId).append("\"]");
        }
        sb.append("]");
        return sb.toString();
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
