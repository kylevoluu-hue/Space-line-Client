package com.spaceline.client.module.hud;

import java.util.List;

import com.spaceline.client.engine.GameState;
import com.spaceline.client.hud.HudModule;
import com.spaceline.common.module.setting.BooleanSetting;

/** Displays the current frames-per-second. */
public final class FpsModule extends HudModule {

    private final BooleanSetting showLabel;

    public FpsModule() {
        super("fps", "FPS", "Shows the current frame rate");
        this.showLabel = bool("label", "Show label", "Append 'FPS' after the number", true);
    }

    @Override
    protected List<String> lines(GameState game) {
        String value = game.fps() + (showLabel.get() ? " FPS" : "");
        return List.of(value);
    }
}
