package com.spaceline.common.hud;

/**
 * The corner/edge of the screen a HUD element is anchored to. Anchoring (rather
 * than storing absolute pixels) keeps elements correctly placed when the window
 * is resized or the GUI scale changes.
 */
public enum HudAnchor {
    TOP_LEFT(0.0, 0.0),
    TOP_CENTER(0.5, 0.0),
    TOP_RIGHT(1.0, 0.0),
    MIDDLE_LEFT(0.0, 0.5),
    CENTER(0.5, 0.5),
    MIDDLE_RIGHT(1.0, 0.5),
    BOTTOM_LEFT(0.0, 1.0),
    BOTTOM_CENTER(0.5, 1.0),
    BOTTOM_RIGHT(1.0, 1.0);

    private final double xFactor;
    private final double yFactor;

    HudAnchor(double xFactor, double yFactor) {
        this.xFactor = xFactor;
        this.yFactor = yFactor;
    }

    public double xFactor() {
        return xFactor;
    }

    public double yFactor() {
        return yFactor;
    }

    /** Resolves the anchor's origin in screen pixels for a given screen size. */
    public double originX(int screenWidth) {
        return screenWidth * xFactor;
    }

    public double originY(int screenHeight) {
        return screenHeight * yFactor;
    }
}
