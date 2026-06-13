package com.spaceline.client.fabric;

import java.nio.file.Path;

import com.spaceline.client.engine.GameState;
import com.spaceline.client.engine.SpaceLineClientEngine;
import com.spaceline.client.event.ClientTickEvent;
import com.spaceline.client.event.HudRenderEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric client entrypoint. Constructs the {@link SpaceLineClientEngine}, wires
 * a Minecraft-backed {@link GameState} into it and bridges Fabric's HUD-render
 * and tick callbacks to the engine's event bus.
 *
 * <p>This class — and the rest of {@code com.spaceline.client.fabric} — is the
 * only Minecraft-aware code in the project; everything it drives lives in the
 * platform-agnostic {@code :client} engine.
 */
public final class SpaceLineFabricClient implements ClientModInitializer {

    private static final Logger LOG = LoggerFactory.getLogger("SpaceLine");

    private static SpaceLineClientEngine engine;

    public static SpaceLineClientEngine engine() {
        return engine;
    }

    @Override
    public void onInitializeClient() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("spaceline");
        GameState gameState = new FabricGameState();
        engine = new SpaceLineClientEngine(configDir, () -> gameState);
        engine.start();
        LOG.info("Space~line client engine initialised");

        KeyBindings.register(engine);
        ModuleEffects.register(engine);
        com.spaceline.client.fabric.menu.SpacelineMenu.register();

        // Bridge Fabric's per-frame HUD render into the engine's event bus.
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            FabricRenderContext ctx = new FabricRenderContext(
                    MinecraftClient.getInstance(), drawContext);
            engine.eventBus().post(new HudRenderEvent(ctx, tickCounter.getTickDelta(false)));
        });

        // Bridge the client tick.
        long[] tick = {0};
        ClientTickEvents.END_CLIENT_TICK.register(client ->
                engine.eventBus().post(new ClientTickEvent(tick[0]++)));
    }
}
