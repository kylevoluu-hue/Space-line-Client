package com.spaceline.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.spaceline.common.event.EventBus;
import com.spaceline.common.module.Module;
import com.spaceline.common.module.ModuleCategory;
import com.spaceline.common.module.ModuleManager;
import com.spaceline.common.module.setting.NumberSetting;
import org.junit.jupiter.api.Test;

class ModuleFrameworkTest {

    static final class CpsModule extends Module {
        final NumberSetting decay;

        CpsModule() {
            super("cps", "CPS", ModuleCategory.HUD, "Clicks per second");
            this.decay = register(new NumberSetting("decay", "Decay", "Window in ms", 1000, 100, 2000, 50));
        }
    }

    @Test
    void registersAndGroupsByCategory() {
        ModuleManager manager = new ModuleManager(new EventBus());
        manager.register(new CpsModule());
        assertEquals(1, manager.byCategory(ModuleCategory.HUD).size());
        assertTrue(manager.get("cps").isPresent());
    }

    @Test
    void numberSettingClampsToBounds() {
        CpsModule cps = new CpsModule();
        cps.decay.set(99999.0);
        assertEquals(2000.0, cps.decay.get());
        cps.decay.set(-5.0);
        assertEquals(100.0, cps.decay.get());
    }

    @Test
    void persistsEnabledAndSettings() {
        CpsModule cps = new CpsModule();
        cps.setEnabled(true);
        cps.decay.set(1500.0);
        JsonObject json = cps.toJson();

        CpsModule restored = new CpsModule();
        restored.fromJson(json);
        assertTrue(restored.isEnabled());
        assertEquals(1500.0, restored.decay.get());
    }

    @Test
    void keybindTogglesThroughManager() {
        ModuleManager manager = new ModuleManager(new EventBus());
        CpsModule cps = manager.register(new CpsModule());
        cps.keybind().set(67); // 'C'
        manager.handleKeyPress(67);
        assertTrue(cps.isEnabled());
        manager.handleKeyPress(67);
        assertFalse(cps.isEnabled());
    }
}
