package com.spaceline.client.fabric.menu;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Brands and extends Minecraft's title screen without replacing it.
 *
 * <p>Using Fabric's Screen API (no mixin), it adds a <b>Spaceline Packs</b> button
 * that opens the in-game {@link SpacelineResourcePackScreen}, and paints an
 * animated "SPACE~LINE" wordmark with drifting particles over the menu so the
 * start screen feels like a custom client rather than vanilla.
 */
public final class SpacelineMenu {

    private SpacelineMenu() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof TitleScreen)) {
                return;
            }
            // A button into the Spaceline resource-pack browser.
            ButtonWidget packs = ButtonWidget.builder(
                            Text.literal("✨ Spaceline Packs"),
                            b -> client.setScreen(new SpacelineResourcePackScreen(screen)))
                    .dimensions(scaledWidth / 2 - 100, scaledHeight / 4 + 120, 200, 20)
                    .build();
            Screens.getButtons(screen).add(packs);

            // Animated branding overlay drawn on top of the panorama each frame.
            ScreenEvents.afterRender(screen).register((scr, context, mouseX, mouseY, delta) ->
                    drawBranding(client, context, scaledWidth));
        });
    }

    private static void drawBranding(MinecraftClient client, DrawContext context, int width) {
        long time = System.currentTimeMillis();

        // Drifting particle dots.
        for (int i = 0; i < 24; i++) {
            double phase = i * 0.7 + time / 1400.0;
            int px = (int) ((Math.sin(phase) * 0.5 + 0.5) * width);
            int py = (int) (20 + (i * 37 % 120) + Math.cos(phase * 1.3) * 6);
            int alpha = 0x40 + (int) ((Math.sin(phase * 2) * 0.5 + 0.5) * 0x60);
            context.fill(px, py, px + 2, py + 2, (alpha << 24) | 0x6FB8FF);
        }

        // Chrome-cycling "SPACE~LINE" wordmark.
        String title = "SPACE~LINE";
        float hue = (time % 4000L) / 4000f;
        int color = 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, 0.5f, 1f) & 0x00FFFFFF);
        context.getMatrices().push();
        context.getMatrices().translate(width / 2.0, 8, 0);
        context.getMatrices().scale(2.0f, 2.0f, 1.0f);
        int textWidth = client.textRenderer.getWidth(title);
        context.drawText(client.textRenderer, title, -textWidth / 2, 0, color, true);
        context.getMatrices().pop();
    }
}
