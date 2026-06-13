package com.spaceline.common.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A choice among the constants of an enum, rendered as a cycle button or
 * dropdown. Serialized by {@link Enum#name()} so reordering constants does not
 * corrupt saved configs.
 *
 * @param <E> the enum type
 */
public final class EnumSetting<E extends Enum<E>> extends Setting<E> {

    private final Class<E> enumType;

    public EnumSetting(String key, String displayName, String description, E defaultValue) {
        super(key, displayName, description, defaultValue);
        this.enumType = defaultValue.getDeclaringClass();
    }

    public E[] options() {
        return enumType.getEnumConstants();
    }

    /** Advances to the next constant, wrapping around. */
    public void cycle() {
        E[] values = options();
        set(values[(get().ordinal() + 1) % values.length]);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get().name());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return;
        }
        try {
            set(Enum.valueOf(enumType, element.getAsString()));
        } catch (IllegalArgumentException ignored) {
            // Unknown constant (e.g. renamed); keep the existing/default value.
        }
    }
}
