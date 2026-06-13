package com.spaceline.common.ui;

import com.spaceline.common.config.Versioned;

/**
 * A UI theme: a named palette plus a background descriptor. Themes are JSON
 * documents so users can author and upload their own; the built-ins (Dark, Light,
 * Glass, Neon, Minimal) are provided by {@link ThemeManager}.
 *
 * <p>Colours are packed ARGB ints. The {@link Background} supports solid colours,
 * static images and animated/video backgrounds, satisfying the launcher's
 * customizable-background requirement.
 */
public final class Theme implements Versioned {

    public static final int VERSION = 1;

    /** How a theme paints the window background. */
    public enum BackgroundType { COLOR, IMAGE, ANIMATED, VIDEO }

    public static final class Background {
        public BackgroundType type = BackgroundType.COLOR;
        /** File path or resource id for IMAGE/ANIMATED/VIDEO backgrounds. */
        public String source = "";
        /** Tint/overlay colour applied over media backgrounds (ARGB). */
        public int overlay = 0x66000000;
        /** Loop playback for ANIMATED/VIDEO. */
        public boolean loop = true;
    }

    private int currentVersion = VERSION;
    private String id;
    private String name;
    private boolean builtIn;

    // Core palette (ARGB).
    private int accent = 0xFF4C8DFF;
    private int background = 0xFF101218;
    private int surface = 0xFF1A1D26;
    private int textPrimary = 0xFFFFFFFF;
    private int textSecondary = 0xFFB0B4C0;
    private int border = 0xFF2A2E3A;
    private int danger = 0xFFFF5C5C;
    private int success = 0xFF4CD07D;

    private Background backgroundMedia = new Background();
    private double cornerRadius = 8.0;
    private double opacity = 1.0;

    public Theme() {
    }

    public Theme(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public boolean builtIn() {
        return builtIn;
    }

    public Theme markBuiltIn() {
        this.builtIn = true;
        return this;
    }

    public int accent() {
        return accent;
    }

    public int background() {
        return background;
    }

    public int surface() {
        return surface;
    }

    public int textPrimary() {
        return textPrimary;
    }

    public int textSecondary() {
        return textSecondary;
    }

    public int border() {
        return border;
    }

    public int danger() {
        return danger;
    }

    public int success() {
        return success;
    }

    public Background backgroundMedia() {
        return backgroundMedia;
    }

    public double cornerRadius() {
        return cornerRadius;
    }

    public double opacity() {
        return opacity;
    }

    // Fluent setters used by ThemeManager to define the built-ins.
    Theme palette(int accent, int background, int surface, int textPrimary,
                  int textSecondary, int border) {
        this.accent = accent;
        this.background = background;
        this.surface = surface;
        this.textPrimary = textPrimary;
        this.textSecondary = textSecondary;
        this.border = border;
        return this;
    }

    Theme corner(double radius) {
        this.cornerRadius = radius;
        return this;
    }

    Theme opacity(double opacity) {
        this.opacity = Math.max(0.0, Math.min(1.0, opacity));
        return this;
    }

    @Override
    public int currentVersion() {
        return VERSION;
    }
}
