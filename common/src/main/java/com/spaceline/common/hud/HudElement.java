package com.spaceline.common.hud;

/**
 * The persisted layout state of a single draggable HUD widget.
 *
 * <p>Position is stored as an {@link HudAnchor} plus a pixel offset from that
 * anchor, so the element stays put across resolution and GUI-scale changes. The
 * remaining fields cover the customization surface required of every HUD element:
 * scale, colour, opacity, z-order layering, visibility and snap-to-grid.
 *
 * <p>This is a plain data object (serialized by Gson). The actual drawing,
 * measuring and drag handling lives in the {@code :client} rendering layer.
 */
public final class HudElement {

    private String id;
    private HudAnchor anchor = HudAnchor.TOP_LEFT;
    private double offsetX = 4;
    private double offsetY = 4;
    private double scale = 1.0;
    private int color = 0xFFFFFFFF;       // ARGB
    private int backgroundColor = 0x80000000;
    private double opacity = 1.0;          // 0..1, multiplies element alpha
    private int layer = 0;                 // higher = drawn on top
    private boolean visible = true;
    private boolean background = false;
    private boolean shadow = true;
    private String fontId = "default";

    public HudElement() {
    }

    public HudElement(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public HudAnchor anchor() {
        return anchor;
    }

    public void setAnchor(HudAnchor anchor) {
        this.anchor = anchor;
    }

    public double offsetX() {
        return offsetX;
    }

    public double offsetY() {
        return offsetY;
    }

    public void setOffset(double x, double y) {
        this.offsetX = x;
        this.offsetY = y;
    }

    public double scale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = Math.max(0.25, Math.min(4.0, scale));
    }

    public int color() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int backgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public double opacity() {
        return opacity;
    }

    public void setOpacity(double opacity) {
        this.opacity = Math.max(0.0, Math.min(1.0, opacity));
    }

    public int layer() {
        return layer;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public boolean visible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean background() {
        return background;
    }

    public void setBackground(boolean background) {
        this.background = background;
    }

    public boolean shadow() {
        return shadow;
    }

    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }

    public String fontId() {
        return fontId;
    }

    public void setFontId(String fontId) {
        this.fontId = fontId;
    }

    /**
     * Resolves the element's top-left X in screen pixels for the given screen
     * width and measured element width, applying the anchor factor so a
     * right-anchored element grows leftwards.
     */
    public double resolveX(int screenWidth, double elementWidth) {
        return anchor.originX(screenWidth) + offsetX - elementWidth * anchor.xFactor();
    }

    public double resolveY(int screenHeight, double elementHeight) {
        return anchor.originY(screenHeight) + offsetY - elementHeight * anchor.yFactor();
    }
}
