package com.spaceline.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.spaceline.client.engine.ClientModules;
import com.spaceline.client.engine.GameState;
import com.spaceline.client.hud.HudModule;
import com.spaceline.client.render.RenderContext;
import com.spaceline.common.event.EventBus;
import com.spaceline.common.hud.HudElement;
import com.spaceline.common.module.Module;
import com.spaceline.common.module.ModuleCategory;
import com.spaceline.common.module.ModuleManager;
import org.junit.jupiter.api.Test;

class ClientModulesTest {

    private ModuleManager registered() {
        ModuleManager manager = new ModuleManager(new EventBus());
        ClientModules.registerAll(manager);
        return manager;
    }

    @Test
    void registersWholeCatalogueWithoutDuplicates() {
        ModuleManager manager = registered();
        assertEquals(ClientModules.count(), manager.all().size());
        assertTrue(manager.all().size() >= 80, "expected the full module catalogue");
    }

    @Test
    void everyCategoryHasModules() {
        ModuleManager manager = registered();
        for (ModuleCategory category : ModuleCategory.values()) {
            assertFalse(manager.byCategory(category).isEmpty(),
                    "category " + category + " should have at least one module");
        }
    }

    @Test
    void everyModuleHasStableIdAndName() {
        for (Module module : registered().all()) {
            assertFalse(module.id().isBlank());
            assertFalse(module.displayName().isBlank());
            assertEquals(module.id(), module.id().toLowerCase());
        }
    }

    @Test
    void hudModulesRenderWithoutError() {
        RecordingRenderContext ctx = new RecordingRenderContext();
        GameState game = new GameState() {
            @Override
            public boolean inGame() {
                return true;
            }

            @Override
            public long pingMillis() {
                return 42;
            }
        };
        for (Module module : registered().all()) {
            if (module instanceof HudModule hud) {
                hud.renderHud(ctx, new HudElement(module.id()), game);
            }
        }
        assertTrue(ctx.drawCalls >= 0); // smoke: no exceptions thrown
    }

    /** Minimal RenderContext that just counts draw calls. */
    private static final class RecordingRenderContext implements RenderContext {
        int drawCalls;

        @Override public int screenWidth() { return 1920; }
        @Override public int screenHeight() { return 1080; }
        @Override public int textWidth(String text) { return text.length() * 6; }
        @Override public int fontHeight() { return 9; }
        @Override public void drawText(String text, double x, double y, int color, boolean shadow) { drawCalls++; }
        @Override public void fillRect(double x, double y, double w, double h, int color) { drawCalls++; }
        @Override public void pushScale(double scale) { }
        @Override public void popScale() { }
    }
}
