package com.spaceline.client.module.utility;

import com.spaceline.client.module.ToggleModule;
import com.spaceline.common.module.ModuleCategory;
import com.spaceline.common.module.setting.NumberSetting;

/**
 * Overrides the field of view to a fixed value, independent of the vanilla
 * options slider, and can suppress the dynamic FOV change from sprinting/speed.
 */
public final class FovChangerModule extends ToggleModule {

    private final NumberSetting fov;
    private final com.spaceline.common.module.setting.BooleanSetting staticFov;

    public FovChangerModule() {
        super("fov_changer", "FOV Changer", ModuleCategory.UTILITY, "Sets a custom field of view");
        this.fov = register(new NumberSetting("fov",
                "FOV", "Field of view in degrees", 90, 30, 120, 1));
        this.staticFov = register(new com.spaceline.common.module.setting.BooleanSetting(
                "static", "Static FOV", "Disable sprint/speed FOV changes", true));
    }

    public int fov() {
        return fov.asInt();
    }

    public boolean staticFov() {
        return staticFov.get();
    }
}
