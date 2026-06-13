package com.spaceline.client.render;

/**
 * The abstraction every HUD/visual module draws against.
 *
 * <p>By depending on this interface rather than Minecraft's {@code DrawContext}
 * directly, the entire module catalogue stays free of a Minecraft dependency:
 * it compiles and unit-tests in the root build, and the Fabric layer supplies a
 * concrete implementation backed by the real renderer at runtime. Coordinates
 * are in scaled GUI pixels with the origin at the top-left.
 */
public interface RenderContext {

    int screenWidth();

    int screenHeight();

    /** The pixel width the given string would occupy in the active font. */
    int textWidth(String text);

    /** The line height of the active font. */
    int fontHeight();

    /**
     * Draws a string at {@code (x, y)} in ARGB {@code color}, with an optional
     * drop shadow.
     */
    void drawText(String text, double x, double y, int color, boolean shadow);

    /** Fills an axis-aligned rectangle with an ARGB colour. */
    void fillRect(double x, double y, double width, double height, int color);

    /** Pushes a scale transform; must be paired with {@link #popScale()}. */
    void pushScale(double scale);

    void popScale();
}
