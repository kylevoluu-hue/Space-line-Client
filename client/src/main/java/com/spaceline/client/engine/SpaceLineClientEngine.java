package com.spaceline.client.engine;

import java.nio.file.Path;
import java.util.function.Supplier;

import com.google.gson.JsonObject;
import com.spaceline.client.event.HudRenderEvent;
import com.spaceline.client.event.KeyInputEvent;
import com.spaceline.client.hud.HudManager;
import com.spaceline.common.config.ConfigManager;
import com.spaceline.common.config.Json;
import com.spaceline.common.config.Versioned;
import com.spaceline.common.event.EventBus;
import com.spaceline.common.event.Subscribe;
import com.spaceline.common.hud.HudProfile;
import com.spaceline.common.module.ModuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The platform-agnostic heart of the Space~line client.
 *
 * <p>Owns the {@link EventBus}, the {@link ModuleManager} (with every module
 * registered), the {@link HudManager} and the active {@link HudProfile}, plus
 * config persistence for all of it. The Fabric layer constructs one of these,
 * feeds it a live {@link GameState} supplier and a {@link RenderContext} per
 * frame, and forwards game/input events into the bus — all the actual client
 * behaviour then lives in this engine and the modules, independent of Minecraft.
 */
public final class SpaceLineClientEngine {

    private static final Logger LOG = LoggerFactory.getLogger(SpaceLineClientEngine.class);

    private final EventBus eventBus = new EventBus();
    private final ModuleManager moduleManager = new ModuleManager(eventBus);
    private final HudProfile hudProfile;
    private final HudManager hudManager;
    private final Supplier<GameState> gameStateSupplier;

    private final ConfigManager<ModuleConfig> moduleConfig;
    private final ConfigManager<HudProfile> hudConfig;

    public SpaceLineClientEngine(Path configDir, Supplier<GameState> gameStateSupplier) {
        this.gameStateSupplier = gameStateSupplier;
        this.moduleConfig = new ConfigManager<>(configDir.resolve("modules.json"),
                ModuleConfig.class, ModuleConfig::new);
        this.hudConfig = new ConfigManager<>(configDir.resolve("hud.json"),
                HudProfile.class, HudProfile::new);
        this.hudProfile = hudConfig.load();
        this.hudManager = new HudManager(moduleManager, hudProfile);
    }

    /** Registers all modules, restores saved state and starts listening. */
    public void start() {
        ClientModules.registerAll(moduleManager);
        LOG.info("Registered {} client modules", ClientModules.count());

        ModuleConfig saved = moduleConfig.load();
        if (saved.modules != null) {
            moduleManager.fromJson(saved.modules);
        }
        // Persist whenever a module is toggled or its settings change.
        moduleManager.setSaveHook(this::saveModules);
        eventBus.register(this);
    }

    public EventBus eventBus() {
        return eventBus;
    }

    public ModuleManager modules() {
        return moduleManager;
    }

    public HudManager hud() {
        return hudManager;
    }

    public GameState gameState() {
        return gameStateSupplier.get();
    }

    // ------------------------------------------------------------------
    // Engine-level event handling
    // ------------------------------------------------------------------

    @Subscribe
    public void onHudRender(HudRenderEvent event) {
        hudManager.render(event.context(), gameStateSupplier.get());
    }

    @Subscribe
    public void onKeyInput(KeyInputEvent event) {
        if (event.pressed()) {
            moduleManager.handleKeyPress(event.keyCode());
        }
    }

    public void saveModules() {
        ModuleConfig config = new ModuleConfig();
        config.modules = moduleManager.toJson();
        moduleConfig.save(config);
    }

    public void saveHud() {
        hudConfig.save(hudProfile);
    }

    /** Versioned wrapper persisting the whole module manager state. */
    public static final class ModuleConfig implements Versioned {
        public static final int VERSION = 1;
        JsonObject modules = new JsonObject();

        @Override
        public int currentVersion() {
            return VERSION;
        }
    }

    /** Exposes the Gson instance for the Fabric layer's own serialization. */
    public static com.google.gson.Gson gson() {
        return Json.gson();
    }
}
