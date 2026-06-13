package com.spaceline.common.module.setting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.JsonElement;

/**
 * A single, typed, persistable configuration value belonging to a module.
 *
 * <p>Settings are the unit of UI generation and JSON serialization: the mod menu
 * renders a control per setting based on its concrete subtype, and the config
 * system reads/writes each via {@link #toJson()} / {@link #fromJson(JsonElement)}.
 * Each setting carries a stable {@link #key()} used as its JSON field name.
 *
 * @param <T> the value type
 */
public abstract class Setting<T> {

    private final String key;
    private final String displayName;
    private final String description;
    private final T defaultValue;
    private T value;
    private final List<Consumer<T>> listeners = new ArrayList<>();

    protected Setting(String key, String displayName, String description, T defaultValue) {
        this.key = key;
        this.displayName = displayName;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public T get() {
        return value;
    }

    /** Sets the value (after {@link #sanitize(Object) sanitizing}) and notifies listeners. */
    public void set(T newValue) {
        T sanitized = sanitize(newValue);
        if (java.util.Objects.equals(this.value, sanitized)) {
            return;
        }
        this.value = sanitized;
        listeners.forEach(l -> l.accept(sanitized));
    }

    public void reset() {
        set(defaultValue);
    }

    /** Registers a change listener; returns {@code this} for fluent chaining. */
    public Setting<T> onChange(Consumer<T> listener) {
        listeners.add(listener);
        return this;
    }

    /** Clamps / validates an incoming value. Default is identity. */
    protected T sanitize(T candidate) {
        return candidate;
    }

    /** Serializes the current value to JSON. */
    public abstract JsonElement toJson();

    /** Restores the value from JSON, ignoring malformed input. */
    public abstract void fromJson(JsonElement element);
}
