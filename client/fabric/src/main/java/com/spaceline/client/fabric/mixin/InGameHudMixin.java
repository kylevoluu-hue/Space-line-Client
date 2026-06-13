package com.spaceline.client.fabric.mixin;

import com.spaceline.client.engine.SpaceLineClientEngine;
import com.spaceline.client.fabric.SpaceLineFabricClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Implements the Autohide HUD module: when enabled, the vanilla in-game HUD is
 * suppressed (Space~line's own HUD elements, drawn via the HUD render callback,
 * are unaffected). Demonstrates how a module's state gates vanilla rendering.
 */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void spaceline$autohide(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        SpaceLineClientEngine engine = SpaceLineFabricClient.engine();
        if (engine == null) {
            return;
        }
        engine.modules().get("autohide_hud").ifPresent(module -> {
            if (module.isEnabled()) {
                ci.cancel();
            }
        });
    }
}
