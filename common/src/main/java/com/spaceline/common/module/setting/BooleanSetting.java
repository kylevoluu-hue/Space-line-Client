package com.spaceline.common.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A simple on/off toggle, rendered as a switch. */
public final class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String key, String displayName, String description, boolean defaultValue) {
        super(key, displayName, description, defaultValue);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            set(element.getAsBoolean());
        }
    }
}
