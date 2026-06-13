package com.spaceline.common.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * An ARGB colour stored as a packed {@code int}, rendered as a colour picker.
 * Serialized as a {@code #AARRGGBB} hex string for readability in config files.
 */
public final class ColorSetting extends Setting<Integer> {

    public ColorSetting(String key, String displayName, String description, int argb) {
        super(key, displayName, description, argb);
    }

    public int alpha() {
        return (get() >> 24) & 0xFF;
    }

    public int red() {
        return (get() >> 16) & 0xFF;
    }

    public int green() {
        return (get() >> 8) & 0xFF;
    }

    public int blue() {
        return get() & 0xFF;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(String.format("#%08X", get()));
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return;
        }
        String hex = element.getAsString().replace("#", "");
        try {
            set((int) Long.parseLong(hex, 16));
        } catch (NumberFormatException ignored) {
            // Leave as-is on malformed colour.
        }
    }
}
