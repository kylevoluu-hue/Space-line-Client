package com.spaceline.client.fabric;

import com.spaceline.client.engine.SpaceLineClientEngine;
import com.spaceline.common.module.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Registers Space~line's own key bindings and dispatches module toggle keys.
 *
 * <p>A dedicated binding opens the Space~line menu / HUD editor. Per-module
 * keybinds are stored as GLFW key codes on each {@link Module} and are polled
 * each tick with simple edge detection, so any module can be bound to any key
 * from the in-game settings without registering a vanilla {@link KeyBinding} per
 * module.
 */
public final class KeyBindings {

    private static KeyBinding openMenu;
    private static final boolean[] previous = new boolean[GLFW.GLFW_KEY_LAST + 1];

    private KeyBindings() {
    }

    public static void register(SpaceLineClientEngine engine) {
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.spaceline.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.spaceline"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.wasPressed()) {
                // The mod menu / HUD editor screen would be opened here.
                engine.hud().setEditorOpen(!engine.hud().isEditorOpen());
            }
            pollModuleKeybinds(client, engine);
        });
    }

    private static void pollModuleKeybinds(MinecraftClient client, SpaceLineClientEngine engine) {
        // Only act when no screen (e.g. chat) is capturing input.
        if (client.currentScreen != null) {
            return;
        }
        long handle = client.getWindow().getHandle();
        for (Module module : engine.modules().all()) {
            int code = module.keybind().keyCode();
            if (code < 0 || code > GLFW.GLFW_KEY_LAST) {
                continue;
            }
            boolean down = GLFW.glfwGetKey(handle, code) == GLFW.GLFW_PRESS;
            if (down && !previous[code]) {
                module.toggle();
            }
            previous[code] = down;
        }
    }
}
