package com.spaceline.common.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Free-form text, rendered as a text field. */
public final class StringSetting extends Setting<String> {

    private final int maxLength;

    public StringSetting(String key, String displayName, String description, String defaultValue) {
        this(key, displayName, description, defaultValue, 256);
    }

    public StringSetting(String key, String displayName, String description, String defaultValue, int maxLength) {
        super(key, displayName, description, defaultValue);
        this.maxLength = maxLength;
    }

    public int maxLength() {
        return maxLength;
    }

    @Override
    protected String sanitize(String candidate) {
        if (candidate == null) {
            return "";
        }
        return candidate.length() > maxLength ? candidate.substring(0, maxLength) : candidate;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            set(element.getAsString());
        }
    }
}
