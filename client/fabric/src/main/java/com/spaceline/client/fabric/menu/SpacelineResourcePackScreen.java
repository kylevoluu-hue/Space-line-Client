package com.spaceline.client.fabric.menu;

import java.nio.file.Path;
import java.util.List;

import com.spaceline.client.browser.ResourcePackBrowser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * In-game resource-pack browser: search Modrinth and install packs straight into
 * the running game's {@code resourcepacks} folder, without leaving Minecraft.
 *
 * <p>Searching and downloading run on background threads (never the render
 * thread); the results list is drawn manually with hover highlighting, and an
 * animated gradient banner gives the screen a custom-client feel. Click a row to
 * install it.
 */
public final class SpacelineResourcePackScreen extends Screen {

    private static final int LIST_TOP = 64;
    private static final int ROW_HEIGHT = 28;

    private final Screen parent;
    private final ResourcePackBrowser browser = new ResourcePackBrowser();
    private TextFieldWidget searchField;
    private volatile List<ResourcePackBrowser.Pack> results = List.of();
    private volatile String status = "Search for resource packs";

    public SpacelineResourcePackScreen(Screen parent) {
        super(Text.literal("Spaceline Resource Packs"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        searchField = new TextFieldWidget(this.textRenderer, this.width / 2 - 150, 32, 230, 20,
                Text.literal("Search"));
        searchField.setPlaceholder(Text.literal("Search resource packs…"));
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Search"), b -> doSearch())
                .dimensions(this.width / 2 + 90, 32, 64, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(this.width / 2 - 100, this.height - 28, 200, 20).build());

        setInitialFocus(searchField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        // Animated top banner.
        long time = System.currentTimeMillis();
        int shift = (int) ((Math.sin(time / 700.0) * 0.5 + 0.5) * 40);
        context.fillGradient(0, 0, this.width, 26,
                0xCC101826, (0xCC000000) | (0x102030 + shift));
        context.drawText(this.textRenderer, "✨ Spaceline Resource Packs", 10, 9, 0xFF6FB8FF, true);

        super.render(context, mouseX, mouseY, delta);

        // Results list.
        List<ResourcePackBrowser.Pack> snapshot = results;
        int y = LIST_TOP;
        for (ResourcePackBrowser.Pack pack : snapshot) {
            boolean hovered = mouseX >= 20 && mouseX <= this.width - 20
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;
            context.fill(20, y, this.width - 20, y + ROW_HEIGHT - 2, hovered ? 0x553A7BFF : 0x33202838);
            context.drawText(this.textRenderer, pack.title(), 28, y + 4, 0xFFFFFFFF, false);
            context.drawText(this.textRenderer,
                    "by " + pack.author() + "  ·  " + pack.downloads() + " downloads",
                    28, y + 15, 0xFFA0A8B8, false);
            y += ROW_HEIGHT;
            if (y > this.height - 40) {
                break;
            }
        }

        context.drawText(this.textRenderer, status, 20, this.height - 44, 0xFFB0B4C0, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseY >= LIST_TOP) {
            int index = (int) ((mouseY - LIST_TOP) / ROW_HEIGHT);
            List<ResourcePackBrowser.Pack> snapshot = results;
            if (index >= 0 && index < snapshot.size() && mouseX >= 20 && mouseX <= this.width - 20) {
                install(snapshot.get(index));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void doSearch() {
        String query = searchField.getText();
        status = "Searching…";
        new Thread(() -> {
            try {
                results = browser.search(query, mcVersion());
                status = results.isEmpty() ? "No packs found" : "Click a pack to install it";
            } catch (Exception e) {
                status = "Search failed: " + e.getMessage();
            }
        }, "spaceline-pack-search").start();
    }

    private void install(ResourcePackBrowser.Pack pack) {
        status = "Installing " + pack.title() + "…";
        new Thread(() -> {
            try {
                Path dir = FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
                java.nio.file.Files.createDirectories(dir);
                Path file = browser.download(pack.projectId(), mcVersion(), dir);
                status = "Installed " + file.getFileName() + " — enable it in Options › Resource Packs";
            } catch (Exception e) {
                status = "Install failed: " + e.getMessage();
            }
        }, "spaceline-pack-install").start();
    }

    private String mcVersion() {
        return this.client != null ? this.client.getGameVersion() : "";
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
