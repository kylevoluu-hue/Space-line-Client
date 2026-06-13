package com.spaceline.launcher.instance;

import com.spaceline.common.config.Versioned;

/**
 * A self-contained, configured game profile: a specific Minecraft version, a mod
 * loader, an isolated game directory and per-instance launch settings.
 *
 * <p>Instances are the unit the launcher's Play/Stop controls operate on and
 * what the mod, resource-pack and shader managers scope their files to. This is
 * a versioned config object persisted as {@code instance.json} inside the
 * instance directory.
 */
public final class Instance implements Versioned {

    public static final int VERSION = 1;

    private int currentVersion = VERSION;
    private String id;
    private String name;
    private String minecraftVersion;
    private String loaderId = ModLoader.VANILLA.id();
    private String fabricLoaderVersion;   // null for vanilla

    // Per-instance launch tuning.
    private int minMemoryMb = 1024;
    private int maxMemoryMb = 4096;
    private String javaPathOverride;       // null = auto-select by version family
    private String extraJvmArgs = "";
    private boolean autoRestartOnCrash = false;
    private int windowWidth = 854;
    private int windowHeight = 480;

    public Instance() {
    }

    public Instance(String id, String name, String minecraftVersion, ModLoader loader) {
        this.id = id;
        this.name = name;
        this.minecraftVersion = minecraftVersion;
        this.loaderId = loader.id();
    }

    public String id() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String minecraftVersion() {
        return minecraftVersion;
    }

    public void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }

    public ModLoader loader() {
        return ModLoader.fromId(loaderId);
    }

    public void setLoader(ModLoader loader) {
        this.loaderId = loader.id();
    }

    public String fabricLoaderVersion() {
        return fabricLoaderVersion;
    }

    public void setFabricLoaderVersion(String fabricLoaderVersion) {
        this.fabricLoaderVersion = fabricLoaderVersion;
    }

    public int minMemoryMb() {
        return minMemoryMb;
    }

    public void setMinMemoryMb(int minMemoryMb) {
        this.minMemoryMb = minMemoryMb;
    }

    public int maxMemoryMb() {
        return maxMemoryMb;
    }

    public void setMaxMemoryMb(int maxMemoryMb) {
        this.maxMemoryMb = maxMemoryMb;
    }

    public String javaPathOverride() {
        return javaPathOverride;
    }

    public void setJavaPathOverride(String javaPathOverride) {
        this.javaPathOverride = javaPathOverride;
    }

    public String extraJvmArgs() {
        return extraJvmArgs;
    }

    public void setExtraJvmArgs(String extraJvmArgs) {
        this.extraJvmArgs = extraJvmArgs == null ? "" : extraJvmArgs;
    }

    public boolean autoRestartOnCrash() {
        return autoRestartOnCrash;
    }

    public void setAutoRestartOnCrash(boolean autoRestartOnCrash) {
        this.autoRestartOnCrash = autoRestartOnCrash;
    }

    public int windowWidth() {
        return windowWidth;
    }

    public int windowHeight() {
        return windowHeight;
    }

    public void setWindowSize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
    }

    @Override
    public int currentVersion() {
        return currentVersion;
    }
}
