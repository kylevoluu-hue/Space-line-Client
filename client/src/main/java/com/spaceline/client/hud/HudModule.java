package com.spaceline.client.hud;

import java.util.List;

import com.spaceline.client.engine.GameState;
import com.spaceline.client.render.RenderContext;
import com.spaceline.common.hud.HudElement;
import com.spaceline.common.module.Module;
import com.spaceline.common.module.ModuleCategory;
import com.spaceline.common.module.setting.BooleanSetting;
import com.spaceline.common.module.setting.ColorSetting;
import com.spaceline.common.module.setting.NumberSetting;

/**
 * Base class for the text-based HUD modules (FPS, CPS, Coordinates, Ping, …).
 *
 * <p>A subclass only implements {@link #lines(GameState)} to return the rows of
 * text to display; this base handles the universal HUD customization surface
 * required of every element — drag position (via the {@link HudElement} from the
 * active profile), scaling, colour, opacity, drop shadow, an optional background
 * and brackets — and renders the lines through the {@link RenderContext}. Custom
 * (non-text) HUD modules can override {@link #renderHud} instead.
 */
public abstract class HudModule extends Module {

    protected final BooleanSetting showBackground;
    protected final BooleanSetting textShadow;
    protected final BooleanSetting brackets;
    protected final ColorSetting textColor;
    protected final NumberSetting hudScale;
    protected final NumberSetting hudOpacity;

    protected HudModule(String id, String displayName, String description) {
        super(id, displayName, ModuleCategory.HUD, description);
        this.showBackground = register(new BooleanSetting("background",
                "Background", "Draw a translucent box behind the text", false));
        this.textShadow = register(new BooleanSetting("shadow",
                "Text shadow", "Draw a drop shadow behind the text", true));
        this.brackets = register(new BooleanSetting("brackets",
                "Brackets", "Wrap values in [ ] brackets", false));
        this.textColor = register(new ColorSetting("color",
                "Text colour", "Colour of the HUD text", 0xFFFFFFFF));
        this.hudScale = register(new NumberSetting("scale",
                "Scale", "Size of this HUD element", 1.0, 0.25, 4.0, 0.05));
        this.hudOpacity = register(new NumberSetting("opacity",
                "Opacity", "Transparency of this HUD element", 1.0, 0.0, 1.0, 0.05));
    }

    /** The text rows this element displays this frame. Empty hides it. */
    protected abstract List<String> lines(GameState game);

    /**
     * Renders the element at its profile-defined position. Override for fully
     * custom drawing; the default lays out {@link #lines(GameState)}.
     */
    public void renderHud(RenderContext ctx, HudElement element, GameState game) {
        List<String> lines = lines(game);
        if (lines.isEmpty()) {
            return;
        }

        double scale = hudScale.get() * element.scale();
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, ctx.textWidth(decorate(line)));
        }
        int lineHeight = ctx.fontHeight() + 2;
        int height = lineHeight * lines.size();

        double x = element.resolveX(ctx.screenWidth(), width * scale) / scale;
        double y = element.resolveY(ctx.screenHeight(), height * scale) / scale;

        ctx.pushScale(scale);
        if (showBackground.get()) {
            int bg = applyOpacity(element.backgroundColor());
            ctx.fillRect(x - 2, y - 2, width + 4, height + 2, bg);
        }
        int color = applyOpacity(textColor.get());
        double cursorY = y;
        for (String line : lines) {
            ctx.drawText(decorate(line), x, cursorY, color, textShadow.get());
            cursorY += lineHeight;
        }
        ctx.popScale();
    }

    private String decorate(String line) {
        return brackets.get() ? "[" + line + "]" : line;
    }

    /** Multiplies the colour's alpha by the element + module opacity. */
    protected int applyOpacity(int argb) {
        int baseAlpha = (argb >>> 24) & 0xFF;
        int alpha = (int) Math.round(baseAlpha * hudOpacity.get());
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
