package com.spaceline.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Central Gson configuration. A single pretty-printing, null-serializing
 * instance is shared so every config file on disk looks consistent and is
 * diff-friendly in version control.
 */
public final class Json {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private Json() {
    }

    public static Gson gson() {
        return GSON;
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }

    public static JsonObject parseObject(String json) {
        JsonElement element = com.google.gson.JsonParser.parseString(json);
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("Expected a JSON object but got: " + element);
        }
        return element.getAsJsonObject();
    }
}
