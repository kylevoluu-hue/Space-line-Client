package com.spaceline.client.event;

import com.spaceline.client.render.RenderContext;
import com.spaceline.common.event.Event;

/**
 * Fired every frame while the in-game HUD is being drawn. Carries the
 * {@link RenderContext} HUD modules render through and the frame's partial-tick
 * fraction for smooth interpolation.
 */
public final class HudRenderEvent extends Event {

    private final RenderContext context;
    private final float tickDelta;

    public HudRenderEvent(RenderContext context, float tickDelta) {
        this.context = context;
        this.tickDelta = tickDelta;
    }

    public RenderContext context() {
        return context;
    }

    public float tickDelta() {
        return tickDelta;
    }
}
