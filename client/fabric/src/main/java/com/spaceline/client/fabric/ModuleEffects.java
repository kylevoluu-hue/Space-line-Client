package com.spaceline.client.fabric;

import com.spaceline.client.engine.SpaceLineClientEngine;
import com.spaceline.client.module.visual.FullbrightModule;
import com.spaceline.common.module.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

/**
 * Applies the per-tick, option-driven module effects that are simpler to push
 * than to mix in — currently the Brightness/Fullbright gamma override.
 *
 * <p>The previous gamma is restored when the module is turned off, so toggling
 * Fullbright leaves the user's own brightness setting intact.
 */
public final class ModuleEffects {

    private static Double savedGamma;

    private ModuleEffects() {
    }

    public static void register(SpaceLineClientEngine engine) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> applyFullbright(client, engine));
    }

    private static void applyFullbright(MinecraftClient client, SpaceLineClientEngine engine) {
        Module module = engine.modules().require("fullbright");
        var gammaOption = client.options.getGamma();
        if (module.isEnabled() && module instanceof FullbrightModule fullbright) {
            if (savedGamma == null) {
                savedGamma = gammaOption.getValue();
            }
            gammaOption.setValue(fullbright.gamma());
        } else if (savedGamma != null) {
            gammaOption.setValue(savedGamma);
            savedGamma = null;
        }
    }
}
