package com.spaceline.client.fabric;

import com.spaceline.client.render.RenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Minecraft-backed {@link RenderContext}. Adapts the engine's drawing primitives
 * onto Fabric's {@link DrawContext} and the active {@link TextRenderer}, and
 * implements {@link #pushScale}/{@link #popScale} via the matrix stack so HUD
 * elements can be scaled independently.
 */
public final class FabricRenderContext implements RenderContext {

    private final MinecraftClient client;
    private final DrawContext drawContext;
    private final TextRenderer font;

    public FabricRenderContext(MinecraftClient client, DrawContext drawContext) {
        this.client = client;
        this.drawContext = drawContext;
        this.font = client.textRenderer;
    }

    @Override
    public int screenWidth() {
        return client.getWindow().getScaledWidth();
    }

    @Override
    public int screenHeight() {
        return client.getWindow().getScaledHeight();
    }

    @Override
    public int textWidth(String text) {
        return font.getWidth(text);
    }

    @Override
    public int fontHeight() {
        return font.fontHeight;
    }

    @Override
    public void drawText(String text, double x, double y, int color, boolean shadow) {
        drawContext.drawText(font, text, (int) Math.round(x), (int) Math.round(y), color, shadow);
    }

    @Override
    public void fillRect(double x, double y, double width, double height, int color) {
        drawContext.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color);
    }

    @Override
    public void pushScale(double scale) {
        drawContext.getMatrices().push();
        drawContext.getMatrices().scale((float) scale, (float) scale, 1.0f);
    }

    @Override
    public void popScale() {
        drawContext.getMatrices().pop();
    }
}
