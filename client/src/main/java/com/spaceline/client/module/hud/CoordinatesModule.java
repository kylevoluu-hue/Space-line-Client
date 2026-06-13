package com.spaceline.client.module.hud;

import java.util.ArrayList;
import java.util.List;

import com.spaceline.client.engine.GameState;
import com.spaceline.client.hud.HudModule;
import com.spaceline.common.module.setting.BooleanSetting;

/**
 * Displays the player's XYZ position, optionally with facing direction and the
 * current biome/dimension.
 */
public final class CoordinatesModule extends HudModule {

    private final BooleanSetting showFacing;
    private final BooleanSetting showBiome;

    public CoordinatesModule() {
        super("coordinates", "Coordinates", "Shows your current XYZ position");
        this.showFacing = bool("facing", "Show facing", "Append the cardinal direction", true);
        this.showBiome = bool("biome", "Show biome", "Append the current biome", false);
    }

    @Override
    protected List<String> lines(GameState game) {
        if (!game.inGame()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        lines.add(String.format("XYZ: %.1f / %.1f / %.1f", game.x(), game.y(), game.z()));
        if (showFacing.get()) {
            lines.add("Facing: " + game.facing());
        }
        if (showBiome.get()) {
            lines.add("Biome: " + game.biome());
        }
        return lines;
    }
}
