package com.spaceline.client.hud;

import java.util.Comparator;
import java.util.List;

import com.spaceline.client.engine.GameState;
import com.spaceline.client.render.RenderContext;
import com.spaceline.common.hud.HudElement;
import com.spaceline.common.hud.HudProfile;
import com.spaceline.common.module.Module;
import com.spaceline.common.module.ModuleManager;

/**
 * Draws every enabled {@link HudModule} each frame and owns the HUD editor's
 * drag/snap behaviour.
 *
 * <p>Layout state (anchor, offset, scale, colour, layer, visibility) lives in the
 * active {@link HudProfile}, keyed by module id, so positions persist and can be
 * swapped wholesale by switching profiles. Elements are drawn in ascending
 * {@link HudElement#layer()} order to honour layer/z-ordering.
 */
public final class HudManager {

    private final ModuleManager modules;
    private HudProfile profile;
    private boolean editorOpen;

    public HudManager(ModuleManager modules, HudProfile profile) {
        this.modules = modules;
        this.profile = profile;
    }

    public HudProfile profile() {
        return profile;
    }

    public void setProfile(HudProfile profile) {
        this.profile = profile;
    }

    public boolean isEditorOpen() {
        return editorOpen;
    }

    public void setEditorOpen(boolean editorOpen) {
        this.editorOpen = editorOpen;
    }

    /** Renders all enabled HUD elements in layer order. */
    public void render(RenderContext ctx, GameState game) {
        for (HudModule module : enabledHudModules()) {
            HudElement element = profile.element(module.id());
            if (!element.visible()) {
                continue;
            }
            module.renderHud(ctx, element, game);
        }
        if (editorOpen) {
            drawGrid(ctx);
        }
    }

    /**
     * Moves a HUD element to a new top-left pixel position (used by the editor's
     * drag handling), converting it back into an anchor-relative offset and
     * snapping to the grid when enabled.
     */
    public void dragTo(String moduleId, double pixelX, double pixelY,
                       double elementWidth, double elementHeight,
                       int screenWidth, int screenHeight) {
        HudElement element = profile.element(moduleId);
        double anchorX = element.anchor().originX(screenWidth);
        double anchorY = element.anchor().originY(screenHeight);
        double offsetX = profile.snap(pixelX - anchorX + elementWidth * element.anchor().xFactor());
        double offsetY = profile.snap(pixelY - anchorY + elementHeight * element.anchor().yFactor());
        element.setOffset(offsetX, offsetY);
    }

    private List<HudModule> enabledHudModules() {
        return modules.all().stream()
                .filter(Module::isEnabled)
                .filter(m -> m instanceof HudModule)
                .map(m -> (HudModule) m)
                .sorted(Comparator.comparingInt(m -> profile.element(m.id()).layer()))
                .toList();
    }

    private void drawGrid(RenderContext ctx) {
        if (!profile.snapToGrid()) {
            return;
        }
        int step = Math.max(2, profile.gridSize());
        int gridColor = 0x22FFFFFF;
        for (int x = 0; x < ctx.screenWidth(); x += step) {
            ctx.fillRect(x, 0, 1, ctx.screenHeight(), gridColor);
        }
        for (int y = 0; y < ctx.screenHeight(); y += step) {
            ctx.fillRect(0, y, ctx.screenWidth(), 1, gridColor);
        }
    }
}
