package com.spaceline.common.ui;

import com.spaceline.common.config.Versioned;

/**
 * Persisted UI/appearance preferences: active theme, UI font, accent text colour
 * style, particle background toggle and the chosen background image.
 *
 * <p>Saved as a versioned JSON document so the look of the launcher survives
 * restarts and can be migrated like any other config.
 */
public final class UiSettings implements Versioned {

    public static final int VERSION = 1;

    private int currentVersion = VERSION;
    private String themeId = "dark";
    private String fontFamily = "SansSerif";
    private int fontSize = 13;
    private boolean particlesEnabled = true;
    private String backgroundImage = "";   // path under backgrounds/, or empty
    private TextColorStyle titleColor = TextColorStyle.gradient(0xFF4C8DFF, 0xFFC46BFF);

    public String themeId() {
        return themeId;
    }

    public void setThemeId(String themeId) {
        this.themeId = themeId;
    }

    public String fontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public int fontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = Math.max(8, Math.min(28, fontSize));
    }

    public boolean particlesEnabled() {
        return particlesEnabled;
    }

    public void setParticlesEnabled(boolean particlesEnabled) {
        this.particlesEnabled = particlesEnabled;
    }

    public String backgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage == null ? "" : backgroundImage;
    }

    public TextColorStyle titleColor() {
        return titleColor;
    }

    public void setTitleColor(TextColorStyle titleColor) {
        this.titleColor = titleColor;
    }

    @Override
    public int currentVersion() {
        return currentVersion;
    }
}
