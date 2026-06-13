package com.spaceline.client.module.movement;

import com.spaceline.client.module.ToggleModule;
import com.spaceline.common.module.ModuleCategory;
import com.spaceline.common.module.setting.BooleanSetting;

/**
 * Toggle Sprint / Toggle Sneak. When enabled the Fabric layer keeps the player
 * sprinting (and optionally sneaking) without holding the key.
 */
public final class ToggleSprintModule extends ToggleModule {

    private final BooleanSetting toggleSneak;

    public ToggleSprintModule() {
        super("toggle_sprint", "Toggle Sprint", ModuleCategory.MOVEMENT,
                "Keeps you sprinting without holding the key");
        this.toggleSneak = register(new BooleanSetting("sneak",
                "Toggle sneak", "Also toggle sneaking", false));
    }

    public boolean toggleSneak() {
        return toggleSneak.get();
    }
}
