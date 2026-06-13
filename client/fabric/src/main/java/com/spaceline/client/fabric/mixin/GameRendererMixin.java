package com.spaceline.client.fabric.mixin;

import com.spaceline.client.engine.SpaceLineClientEngine;
import com.spaceline.client.fabric.SpaceLineFabricClient;
import com.spaceline.client.module.utility.FovChangerModule;
import com.spaceline.client.module.utility.ZoomModule;
import com.spaceline.common.module.Module;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the Zoom and FOV Changer modules by adjusting the field of view the
 * renderer computes. Zoom multiplies the FOV down while held; FOV Changer pins
 * it to a fixed value. Both read their live state from the engine's modules.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void spaceline$applyFov(Camera camera, float tickDelta, boolean changingFov,
                                    CallbackInfoReturnable<Double> cir) {
        SpaceLineClientEngine engine = SpaceLineFabricClient.engine();
        if (engine == null) {
            return;
        }
        double fov = cir.getReturnValueD();

        Module fovChanger = engine.modules().require("fov_changer");
        if (fovChanger.isEnabled() && fovChanger instanceof FovChangerModule changer) {
            fov = changer.fov();
        }

        Module zoom = engine.modules().require("zoom");
        if (zoom.isEnabled() && zoom instanceof ZoomModule z) {
            fov *= z.fovMultiplier();
        }

        cir.setReturnValue(fov);
    }
}
