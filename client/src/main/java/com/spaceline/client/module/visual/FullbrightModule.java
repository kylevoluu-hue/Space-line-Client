package com.spaceline.client.module.visual;

import com.spaceline.client.module.ToggleModule;
import com.spaceline.common.module.ModuleCategory;
import com.spaceline.common.module.setting.NumberSetting;

/**
 * Brightness / Fullbright. When enabled the Fabric layer overrides the gamma so
 * caves and night are fully lit. The {@link #gamma()} setting allows values well
 * above the vanilla maximum.
 */
public final class FullbrightModule extends ToggleModule {

    private final NumberSetting gamma;

    public FullbrightModule() {
        super("fullbright", "Brightness / Fullbright", ModuleCategory.VISUAL,
                "Brightens the world to maximum");
        this.gamma = register(new NumberSetting("gamma",
                "Gamma", "Brightness multiplier (1 = vanilla max)", 10.0, 1.0, 16.0, 0.5));
    }

    public double gamma() {
        return isEnabled() ? gamma.get() : 1.0;
    }
}
