package com.spaceline.common.module;

/**
 * The fixed set of categories every client module is filed under. Used to group
 * modules in the mod menu and to drive per-category UI sections.
 */
public enum ModuleCategory {
    HUD("HUD"),
    VISUAL("Visual"),
    UTILITY("Utility"),
    MOVEMENT("Movement"),
    WORLD("World"),
    SOCIAL("Social"),
    PERFORMANCE("Performance");

    private final String displayName;

    ModuleCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
