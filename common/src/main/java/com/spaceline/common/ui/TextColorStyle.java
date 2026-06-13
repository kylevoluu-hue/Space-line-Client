package com.spaceline.common.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * A reusable text-colour style that the launcher UI and HUD share.
 *
 * <p>Supports four modes:
 * <ul>
 *   <li>{@link Mode#SOLID} — one colour (e.g. red, yellow);</li>
 *   <li>{@link Mode#GRADIENT} — a left-to-right blend across two or more colours;</li>
 *   <li>{@link Mode#CHROME} — an animated rainbow/RGB cycle that scrolls over time;</li>
 *   <li>{@link Mode#PALETTE} — colours assigned per-character, cycling a palette.</li>
 * </ul>
 *
 * Colours are packed ARGB ints. {@link #colorAt(float, long)} returns the colour
 * for a normalized position {@code 0..1} at a given time (for animation), which
 * a renderer calls per character to paint gradient/chrome text.
 */
public final class TextColorStyle {

    public enum Mode { SOLID, GRADIENT, CHROME, PALETTE }

    private Mode mode = Mode.SOLID;
    private final List<Integer> colors = new ArrayList<>(List.of(0xFFFFFFFF));
    private float speed = 1.0f; // chrome scroll speed multiplier

    public static TextColorStyle solid(int argb) {
        TextColorStyle s = new TextColorStyle();
        s.mode = Mode.SOLID;
        s.colors.clear();
        s.colors.add(argb);
        return s;
    }

    public static TextColorStyle gradient(int... argb) {
        TextColorStyle s = new TextColorStyle();
        s.mode = Mode.GRADIENT;
        s.colors.clear();
        for (int c : argb) {
            s.colors.add(c);
        }
        return s;
    }

    public static TextColorStyle chrome() {
        TextColorStyle s = new TextColorStyle();
        s.mode = Mode.CHROME;
        return s;
    }

    public Mode mode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public List<Integer> colors() {
        return colors;
    }

    public float speed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * Resolves the ARGB colour at normalized position {@code t} (0..1) and time
     * {@code timeMillis} (used by CHROME's scroll).
     */
    public int colorAt(float t, long timeMillis) {
        t = clamp01(t);
        return switch (mode) {
            case SOLID -> colors.isEmpty() ? 0xFFFFFFFF : colors.get(0);
            case GRADIENT -> gradientColor(t);
            case PALETTE -> colors.isEmpty() ? 0xFFFFFFFF
                    : colors.get((int) (t * colors.size()) % colors.size());
            case CHROME -> {
                float hue = (t + (timeMillis % 3000L) / 3000f * speed) % 1f;
                yield 0xFF000000 | (hsbToRgb(hue, 0.7f, 1f) & 0x00FFFFFF);
            }
        };
    }

    private int gradientColor(float t) {
        if (colors.isEmpty()) {
            return 0xFFFFFFFF;
        }
        if (colors.size() == 1) {
            return colors.get(0);
        }
        float scaled = t * (colors.size() - 1);
        int i = Math.min((int) scaled, colors.size() - 2);
        float local = scaled - i;
        return lerpArgb(colors.get(i), colors.get(i + 1), local);
    }

    private static int lerpArgb(int a, int b, float t) {
        int aa = lerpChannel(a, b, t, 24);
        int rr = lerpChannel(a, b, t, 16);
        int gg = lerpChannel(a, b, t, 8);
        int bb = lerpChannel(a, b, t, 0);
        return (aa << 24) | (rr << 16) | (gg << 8) | bb;
    }

    private static int lerpChannel(int a, int b, float t, int shift) {
        int ca = (a >> shift) & 0xFF;
        int cb = (b >> shift) & 0xFF;
        return Math.round(ca + (cb - ca) * t);
    }

    private static int hsbToRgb(float h, float s, float b) {
        float r = 0, g = 0, bl = 0;
        int i = (int) (h * 6) % 6;
        float f = h * 6 - (float) Math.floor(h * 6);
        float p = b * (1 - s);
        float q = b * (1 - f * s);
        float tt = b * (1 - (1 - f) * s);
        switch (i) {
            case 0 -> { r = b; g = tt; bl = p; }
            case 1 -> { r = q; g = b; bl = p; }
            case 2 -> { r = p; g = b; bl = tt; }
            case 3 -> { r = p; g = q; bl = b; }
            case 4 -> { r = tt; g = p; bl = b; }
            default -> { r = b; g = p; bl = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (bl * 255);
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }
}
