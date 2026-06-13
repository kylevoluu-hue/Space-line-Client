package com.spaceline.client.module.utility;

import com.spaceline.client.module.ToggleModule;
import com.spaceline.common.module.ModuleCategory;
import com.spaceline.common.module.setting.BooleanSetting;
import com.spaceline.common.module.setting.NumberSetting;

/**
 * Optifine/Lunar-style zoom. While enabled (typically held via its keybind) the
 * Fabric layer divides the field of view by {@link #zoomLevel()} to magnify the
 * view, optionally easing the transition.
 *
 * <p>Default keybind is {@code C} (GLFW key code 67).
 */
public final class ZoomModule extends ToggleModule {

    private final NumberSetting zoomLevel;
    private final BooleanSetting smooth;

    public ZoomModule() {
        super("zoom", "Zoom", ModuleCategory.UTILITY, "Hold to zoom in", 67);
        this.zoomLevel = register(new NumberSetting("level",
                "Zoom level", "How far to magnify", 4.0, 1.5, 10.0, 0.5));
        this.smooth = register(new BooleanSetting("smooth",
                "Smooth zoom", "Ease the zoom transition", true));
    }

    public double zoomLevel() {
        return zoomLevel.get();
    }

    public boolean smooth() {
        return smooth.get();
    }

    /** The effective FOV multiplier the renderer should apply. */
    public double fovMultiplier() {
        return isEnabled() ? 1.0 / zoomLevel.get() : 1.0;
    }
}
