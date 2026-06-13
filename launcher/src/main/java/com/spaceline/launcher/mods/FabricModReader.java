package com.spaceline.launcher.mods;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads {@code fabric.mod.json} out of a mod jar into {@link ModMetadata}.
 *
 * <p>Parsing is defensive: a jar with no descriptor, or a malformed one, yields
 * {@link ModMetadata#unknown(String)} rather than throwing, so a single bad file
 * can't break the whole mod list. The Minecraft requirement and inter-mod
 * dependencies are extracted from the standard {@code depends}/{@code breaks}
 * blocks.
 */
public final class FabricModReader {

    private static final Logger LOG = LoggerFactory.getLogger(FabricModReader.class);

    public ModMetadata read(Path jar) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry("fabric.mod.json");
            if (entry == null) {
                return ModMetadata.unknown(jar.getFileName().toString());
            }
            try (InputStream in = zip.getInputStream(entry)) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                return parse(JsonParser.parseString(json).getAsJsonObject(), jar);
            }
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not read mod metadata from {}", jar.getFileName(), e);
            return ModMetadata.unknown(jar.getFileName().toString());
        }
    }

    private ModMetadata parse(JsonObject root, Path jar) {
        String id = optString(root, "id", jar.getFileName().toString());
        String name = optString(root, "name", id);
        String version = optString(root, "version", "0.0.0");

        Map<String, String> depends = new LinkedHashMap<>();
        String minecraftReq = "*";
        if (root.has("depends") && root.get("depends").isJsonObject()) {
            JsonObject dependsObj = root.getAsJsonObject("depends");
            for (String key : dependsObj.keySet()) {
                String predicate = predicateOf(dependsObj.get(key));
                if ("minecraft".equals(key)) {
                    minecraftReq = predicate;
                } else if (!"fabricloader".equals(key) && !"java".equals(key)) {
                    depends.put(key, predicate);
                }
            }
        }

        List<String> breaks = stringList(root, "breaks");
        List<String> provides = stringList(root, "provides");
        return new ModMetadata(id, name, version, depends, breaks, provides, minecraftReq);
    }

    private static String predicateOf(JsonElement element) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            return array.isEmpty() ? "*" : array.get(0).getAsString();
        }
        return element.getAsString();
    }

    private static List<String> stringList(JsonObject root, String key) {
        List<String> result = new ArrayList<>();
        if (root.has(key)) {
            JsonElement element = root.get(key);
            if (element.isJsonArray()) {
                element.getAsJsonArray().forEach(e -> result.add(e.getAsString()));
            } else if (element.isJsonObject()) {
                result.addAll(element.getAsJsonObject().keySet());
            }
        }
        return result;
    }

    private static String optString(JsonObject root, String key, String fallback) {
        JsonElement element = root.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }
}
