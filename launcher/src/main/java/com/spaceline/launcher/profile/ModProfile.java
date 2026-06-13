package com.spaceline.launcher.profile;

import java.util.ArrayList;
import java.util.List;

import com.spaceline.common.config.Versioned;

/**
 * A named, saveable bundle of mods, resource packs and shaders that can be
 * applied to an instance, exported as a portable {@code .zip}, and imported
 * again (here or, since it is a plain folder-structured zip, into other
 * launchers).
 *
 * <p>A profile stores two kinds of references:
 * <ul>
 *   <li><b>files</b> — the actual file names captured from an instance, bundled
 *       into the export zip under {@code mods/}, {@code resourcepacks/} and
 *       {@code shaderpacks/}; and</li>
 *   <li><b>content references</b> — Modrinth/CurseForge project slugs that a
 *       pre-built profile installs fresh for the target Minecraft version (so the
 *       likes of "FPS Booster" always pull the newest compatible builds).</li>
 * </ul>
 */
public final class ModProfile implements Versioned {

    public static final int VERSION = 1;

    private int currentVersion = VERSION;
    private String id;
    private String name;
    private String description = "";
    private String minecraftVersion = "";
    private String loaderId = "fabric";
    private boolean builtIn;

    private final List<String> modFiles = new ArrayList<>();
    private final List<String> resourcePacks = new ArrayList<>();
    private final List<String> shaderPacks = new ArrayList<>();
    /** Modrinth project slugs a pre-built profile installs fresh. */
    private final List<String> modrinthSlugs = new ArrayList<>();

    public ModProfile() {
    }

    public ModProfile(String id, String name) {
        this.id = id;
        this.name = name;
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

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public String minecraftVersion() {
        return minecraftVersion;
    }

    public void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }

    public String loaderId() {
        return loaderId;
    }

    public void setLoaderId(String loaderId) {
        this.loaderId = loaderId;
    }

    public boolean builtIn() {
        return builtIn;
    }

    public ModProfile markBuiltIn() {
        this.builtIn = true;
        return this;
    }

    public List<String> modFiles() {
        return modFiles;
    }

    public List<String> resourcePacks() {
        return resourcePacks;
    }

    public List<String> shaderPacks() {
        return shaderPacks;
    }

    public List<String> modrinthSlugs() {
        return modrinthSlugs;
    }

    @Override
    public int currentVersion() {
        return currentVersion;
    }
}
