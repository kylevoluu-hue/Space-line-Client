package com.spaceline.client.fabric.mixin;

import com.spaceline.client.engine.SpaceLineClientEngine;
import com.spaceline.client.event.MouseClickEvent;
import com.spaceline.client.fabric.SpaceLineFabricClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forwards raw mouse button presses into the engine as {@link MouseClickEvent}s
 * so click-tracking modules (CPS, combo display) work everywhere, not just when
 * an attack actually lands.
 */
@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void spaceline$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        SpaceLineClientEngine engine = SpaceLineFabricClient.engine();
        if (engine == null || action != GLFW.GLFW_PRESS) {
            return;
        }
        MouseClickEvent.Button mapped = switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> MouseClickEvent.Button.LEFT;
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> MouseClickEvent.Button.RIGHT;
            default -> MouseClickEvent.Button.MIDDLE;
        };
        engine.eventBus().post(new MouseClickEvent(mapped));
    }
}
