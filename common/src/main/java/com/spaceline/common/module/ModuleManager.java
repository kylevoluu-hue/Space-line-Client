package com.spaceline.common.module;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.spaceline.common.event.EventBus;
import com.spaceline.common.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The central registry and lifecycle owner for client {@link Module}s.
 *
 * <p>This is the heart of the plugin architecture: modules register themselves
 * here, are grouped by {@link ModuleCategory}, and have their enabled-state and
 * settings persisted as a single JSON document. A shared {@link EventBus} is
 * exposed so enabled modules can subscribe to game events.
 */
public final class ModuleManager {

    private static final Logger LOG = LoggerFactory.getLogger(ModuleManager.class);

    private final Registry<Module> registry = new Registry<>("modules");
    private final EventBus eventBus;
    private Runnable saveHook = () -> { };

    public ModuleManager(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public EventBus eventBus() {
        return eventBus;
    }

    /** Registers a module. Returns it so callers can keep a typed reference. */
    public <M extends Module> M register(M module) {
        registry.register(module);
        module.attach(this);
        return module;
    }

    public List<Module> all() {
        return registry.all();
    }

    public Module require(String id) {
        return registry.require(id);
    }

    public java.util.Optional<Module> get(String id) {
        return registry.get(id);
    }

    /** All modules in a category, in registration order. */
    public List<Module> byCategory(ModuleCategory category) {
        List<Module> result = new ArrayList<>();
        for (Module module : registry) {
            if (module.category() == category) {
                result.add(module);
            }
        }
        return result;
    }

    public Map<ModuleCategory, List<Module>> grouped() {
        Map<ModuleCategory, List<Module>> map = new EnumMap<>(ModuleCategory.class);
        for (ModuleCategory category : ModuleCategory.values()) {
            map.put(category, byCategory(category));
        }
        return map;
    }

    /** Dispatches a keypress to any module whose keybind matches. */
    public void handleKeyPress(int keyCode) {
        for (Module module : registry) {
            if (module.keybind().matches(keyCode)) {
                module.toggle();
                LOG.debug("Toggled module '{}' via keybind -> {}", module.id(), module.isEnabled());
            }
        }
    }

    /** Lets the client wire a debounced save when any module changes. */
    public void setSaveHook(Runnable saveHook) {
        this.saveHook = saveHook;
    }

    void onModuleToggled(Module module) {
        // When a module enables/disables we (un)subscribe it from the bus so the
        // event chain only ever contains active modules.
        if (module.isEnabled()) {
            eventBus.register(module);
        } else {
            eventBus.unregister(module);
        }
        saveHook.run();
    }

    // ------------------------------------------------------------------
    // Persistence — a single document mapping module id -> its state.
    // ------------------------------------------------------------------

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        for (Module module : registry) {
            root.add(module.id(), module.toJson());
        }
        return root;
    }

    public void fromJson(JsonObject root) {
        if (root == null) {
            return;
        }
        for (Module module : registry) {
            if (root.has(module.id()) && root.get(module.id()).isJsonObject()) {
                module.fromJson(root.getAsJsonObject(module.id()));
            }
        }
    }
}
