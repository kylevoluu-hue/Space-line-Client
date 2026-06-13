package com.spaceline.client.module.hud;

import java.util.List;

import com.spaceline.client.engine.GameState;
import com.spaceline.client.hud.HudModule;

/** Displays JVM memory usage, the heart of the System Resources overlay. */
public final class SystemResourcesModule extends HudModule {

    public SystemResourcesModule() {
        super("system_resources", "System Resources", "Shows memory usage");
    }

    @Override
    protected List<String> lines(GameState game) {
        long usedMb = game.usedMemoryBytes() / (1024 * 1024);
        long maxMb = game.maxMemoryBytes() / (1024 * 1024);
        int percent = maxMb == 0 ? 0 : (int) (usedMb * 100 / maxMb);
        return List.of("Mem: " + usedMb + " / " + maxMb + " MB (" + percent + "%)");
    }
}
