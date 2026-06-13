package com.spaceline.client.module;

import com.spaceline.common.module.Module;
import com.spaceline.common.module.ModuleCategory;

/**
 * Base for non-HUD modules that are primarily a feature toggle with optional
 * settings (Visual/Utility/Movement/etc.). The actual game effect is applied by
 * the Fabric layer, which queries the module's {@link #isEnabled()} state and
 * settings via mixins/events — keeping the module declarations themselves pure
 * Java and unit-testable.
 */
public abstract class ToggleModule extends Module {

    protected ToggleModule(String id, String displayName, ModuleCategory category, String description) {
        super(id, displayName, category, description);
    }

    protected ToggleModule(String id, String displayName, ModuleCategory category,
                           String description, int defaultKey) {
        super(id, displayName, category, description, defaultKey);
    }
}
