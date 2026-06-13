package com.spaceline.launcher.instance;

/**
 * The mod loader an instance launches with. Space~line supports unmodded
 * vanilla and the Fabric loader.
 */
public enum ModLoader {
    VANILLA("vanilla", "Vanilla"),
    FABRIC("fabric", "Fabric");

    private final String id;
    private final String displayName;

    ModLoader(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public boolean supportsMods() {
        return this == FABRIC;
    }

    public static ModLoader fromId(String id) {
        for (ModLoader loader : values()) {
            if (loader.id.equalsIgnoreCase(id)) {
                return loader;
            }
        }
        return VANILLA;
    }
}
