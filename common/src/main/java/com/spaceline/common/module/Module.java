package com.spaceline.common.module;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.spaceline.common.module.setting.BooleanSetting;
import com.spaceline.common.module.setting.Setting;
import com.spaceline.common.registry.Identifiable;

/**
 * Base class for every client feature (CPS, Fullbright, Coordinates, …).
 *
 * <p>A module is the unit of the client's plugin architecture. It owns:
 * <ul>
 *   <li>an enabled flag and lifecycle hooks ({@link #onEnable()}/{@link #onDisable()}),</li>
 *   <li>an ordered set of typed {@link Setting}s (the first being the keybind-able
 *       toggle), and</li>
 *   <li>a {@link Keybind} for quick toggling.</li>
 * </ul>
 *
 * Modules are event-driven: they are registered with the client {@code EventBus}
 * when enabled and unregistered when disabled, so a disabled module costs nothing
 * on the hot path. Concrete modules live in {@code :client} (Fabric) but extend
 * this Minecraft-agnostic base so the framework stays unit-testable.
 */
public abstract class Module implements Identifiable {

    private final String id;
    private final String displayName;
    private final ModuleCategory category;
    private final String description;
    private final Keybind keybind;
    private final Map<String, Setting<?>> settings = new LinkedHashMap<>();

    private boolean enabled;
    private ModuleManager manager;

    protected Module(String id, String displayName, ModuleCategory category, String description) {
        this(id, displayName, category, description, Keybind.UNBOUND);
    }

    protected Module(String id, String displayName, ModuleCategory category, String description, int defaultKey) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.description = description;
        this.keybind = new Keybind(defaultKey);
    }

    @Override
    public final String id() {
        return id;
    }

    public final String displayName() {
        return displayName;
    }

    public final ModuleCategory category() {
        return category;
    }

    public final String description() {
        return description;
    }

    public final Keybind keybind() {
        return keybind;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    /** Registers a setting and returns it for direct field assignment. */
    protected final <S extends Setting<?>> S register(S setting) {
        if (settings.put(setting.key(), setting) != null) {
            throw new IllegalStateException("Duplicate setting '" + setting.key() + "' in module '" + id + "'");
        }
        return setting;
    }

    public final List<Setting<?>> settings() {
        return new ArrayList<>(settings.values());
    }

    /** Convenience for boolean settings used by many HUD/visual modules. */
    protected final BooleanSetting bool(String key, String name, String desc, boolean def) {
        return register(new BooleanSetting(key, name, desc, def));
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    public final void setEnabled(boolean value) {
        if (this.enabled == value) {
            return;
        }
        this.enabled = value;
        if (value) {
            onEnable();
        } else {
            onDisable();
        }
        if (manager != null) {
            manager.onModuleToggled(this);
        }
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    /** Called once when the module becomes enabled. Subscribe to events here. */
    protected void onEnable() {
    }

    /** Called once when the module becomes disabled. Release resources here. */
    protected void onDisable() {
    }

    void attach(ModuleManager manager) {
        this.manager = manager;
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    /** Serializes enabled state, keybind and all settings. */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);
        json.addProperty("keybind", keybind.keyCode());
        JsonObject settingsJson = new JsonObject();
        for (Setting<?> setting : settings.values()) {
            settingsJson.add(setting.key(), setting.toJson());
        }
        json.add("settings", settingsJson);
        return json;
    }

    /** Restores state from a previously serialized object, tolerating gaps. */
    public void fromJson(JsonObject json) {
        if (json == null) {
            return;
        }
        if (json.has("keybind")) {
            keybind.set(json.get("keybind").getAsInt());
        }
        if (json.has("settings") && json.get("settings").isJsonObject()) {
            JsonObject settingsJson = json.getAsJsonObject("settings");
            for (Setting<?> setting : settings.values()) {
                JsonElement element = settingsJson.get(setting.key());
                if (element != null) {
                    setting.fromJson(element);
                }
            }
        }
        // Apply enabled last so onEnable() sees fully-restored settings.
        if (json.has("enabled")) {
            setEnabled(json.get("enabled").getAsBoolean());
        }
    }
}
