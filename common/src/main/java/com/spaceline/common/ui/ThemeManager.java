package com.spaceline.common.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.spaceline.common.config.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the built-in UI themes and loads/saves user themes from the themes
 * directory.
 *
 * <p>Five built-ins are always available — Dark, Light, Glass, Neon and Minimal.
 * Users can drop additional {@code .json} themes into the themes folder (or use
 * the upload action, which copies a file in) and they appear alongside the
 * built-ins. The active theme id is the launcher/client's single UI setting that
 * drives the whole palette.
 */
public final class ThemeManager {

    private static final Logger LOG = LoggerFactory.getLogger(ThemeManager.class);

    private final Path themesDir;
    private final Map<String, Theme> themes = new LinkedHashMap<>();
    private String activeId = "dark";

    public ThemeManager(Path themesDir) {
        this.themesDir = themesDir;
        registerBuiltIns();
    }

    private void registerBuiltIns() {
        put(new Theme("dark", "Dark").markBuiltIn()
                .palette(0xFF4C8DFF, 0xFF101218, 0xFF1A1D26, 0xFFFFFFFF, 0xFFB0B4C0, 0xFF2A2E3A));
        put(new Theme("light", "Light").markBuiltIn()
                .palette(0xFF2F6BFF, 0xFFF5F6FA, 0xFFFFFFFF, 0xFF14161C, 0xFF5A5E6B, 0xFFD8DBE2));
        put(new Theme("glass", "Glass").markBuiltIn()
                .palette(0xFF8FB7FF, 0x66161A24, 0x44222838, 0xFFFFFFFF, 0xFFC8D0E0, 0x33FFFFFF)
                .corner(14).opacity(0.78));
        put(new Theme("neon", "Neon").markBuiltIn()
                .palette(0xFF00E5FF, 0xFF0A0A14, 0xFF12122A, 0xFFE6FBFF, 0xFFFF3DAE, 0xFF2A2A55)
                .corner(4));
        put(new Theme("minimal", "Minimal").markBuiltIn()
                .palette(0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF111111, 0xFF777777, 0xFFE6E6E6)
                .corner(2));
    }

    /** Loads any user-authored themes from disk, on top of the built-ins. */
    public void loadUserThemes() {
        if (!Files.isDirectory(themesDir)) {
            return;
        }
        try (var stream = Files.list(themesDir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(this::loadTheme);
        } catch (IOException e) {
            LOG.error("Failed to scan themes directory {}", themesDir, e);
        }
    }

    private void loadTheme(Path file) {
        try {
            Theme theme = Json.fromJson(Files.readString(file, StandardCharsets.UTF_8), Theme.class);
            if (theme != null && theme.id() != null) {
                themes.put(theme.id(), theme);
                LOG.info("Loaded user theme '{}'", theme.id());
            }
        } catch (IOException | RuntimeException e) {
            LOG.warn("Skipping invalid theme file {}", file.getFileName(), e);
        }
    }

    /** Imports an uploaded theme file into the themes directory and registers it. */
    public Theme upload(Path source) throws IOException {
        Files.createDirectories(themesDir);
        Theme theme = Json.fromJson(Files.readString(source, StandardCharsets.UTF_8), Theme.class);
        if (theme == null || theme.id() == null || theme.id().isBlank()) {
            throw new IOException("Theme file is missing an id");
        }
        Files.writeString(themesDir.resolve(theme.id() + ".json"), Json.toJson(theme));
        themes.put(theme.id(), theme);
        return theme;
    }

    public List<Theme> all() {
        return List.copyOf(themes.values());
    }

    public Optional<Theme> get(String id) {
        return Optional.ofNullable(themes.get(id));
    }

    public Theme active() {
        return themes.getOrDefault(activeId, themes.get("dark"));
    }

    public void setActive(String id) {
        if (!themes.containsKey(id)) {
            throw new IllegalArgumentException("Unknown theme: " + id);
        }
        this.activeId = id;
    }

    private void put(Theme theme) {
        themes.put(theme.id(), theme);
    }
}
