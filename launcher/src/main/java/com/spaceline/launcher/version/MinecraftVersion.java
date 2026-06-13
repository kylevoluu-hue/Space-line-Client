package com.spaceline.launcher.version;

/**
 * An immutable descriptor of a single Minecraft release as published in Mojang's
 * version manifest.
 *
 * <p>Space~line only ever surfaces {@link VersionType#RELEASE stable releases}
 * from 1.21 onward — snapshots, pre-releases and old/experimental builds are
 * filtered out by {@link VersionRepository} before a {@code MinecraftVersion} is
 * ever constructed.
 */
public record MinecraftVersion(
        String id,
        VersionType type,
        String manifestUrl,
        String sha1,
        String releaseTime) implements Comparable<MinecraftVersion> {

    /** The oldest line Space~line supports. */
    public static final SemanticVersion MINIMUM_SUPPORTED = SemanticVersion.parse("1.21");

    /** @return this version's id parsed into comparable numeric components. */
    public SemanticVersion semantic() {
        return SemanticVersion.parse(id);
    }

    /** Whether this is a supported, stable release at or beyond 1.21. */
    public boolean isSupported() {
        return type == VersionType.RELEASE && semantic().compareTo(MINIMUM_SUPPORTED) >= 0;
    }

    @Override
    public int compareTo(MinecraftVersion other) {
        return semantic().compareTo(other.semantic());
    }
}
