package com.spaceline.launcher.profile;

import java.util.List;

/**
 * The built-in starter profiles every install ships with. Each references
 * Modrinth project slugs so that, when applied to an instance, the launcher
 * installs the newest builds compatible with that instance's Minecraft version
 * (rather than baking in stale jars).
 */
public final class PrebuiltProfiles {

    private PrebuiltProfiles() {
    }

    public static List<ModProfile> all() {
        return List.of(pureVanilla(), fpsBooster(), pvp(), legitPerformance());
    }

    /** No mods — a clean vanilla experience. */
    public static ModProfile pureVanilla() {
        ModProfile p = new ModProfile("pure_vanilla", "Pure Vanilla").markBuiltIn();
        p.setDescription("No mods. A clean, unmodified game.");
        p.setLoaderId("vanilla");
        return p;
    }

    /** Maximum frames: Sodium + Lithium + FerriteCore + Fabric API. */
    public static ModProfile fpsBooster() {
        ModProfile p = new ModProfile("fps_booster", "FPS Booster").markBuiltIn();
        p.setDescription("Sodium, Lithium and FerriteCore for the highest frame rate.");
        p.modrinthSlugs().addAll(List.of("fabric-api", "sodium", "lithium", "ferrite-core"));
        return p;
    }

    /** Latency/clarity tuned for PvP. */
    public static ModProfile pvp() {
        ModProfile p = new ModProfile("pvp", "PvP").markBuiltIn();
        p.setDescription("Rendering + performance mods tuned for competitive play.");
        p.modrinthSlugs().addAll(List.of("fabric-api", "sodium", "lithium", "ferrite-core", "iris"));
        return p;
    }

    /** Performance mods that are safe on most servers. */
    public static ModProfile legitPerformance() {
        ModProfile p = new ModProfile("legit_performance", "Legit Performance").markBuiltIn();
        p.setDescription("Server-safe performance mods only.");
        p.modrinthSlugs().addAll(List.of("fabric-api", "sodium", "lithium", "ferrite-core", "entityculling"));
        return p;
    }
}
