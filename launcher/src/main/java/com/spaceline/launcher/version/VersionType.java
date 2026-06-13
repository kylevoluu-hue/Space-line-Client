package com.spaceline.launcher.version;

/**
 * The kinds of versions Mojang publishes. Space~line only ever installs and
 * launches {@link #RELEASE}; the others exist solely so the manifest parser can
 * recognise and discard them.
 */
public enum VersionType {
    RELEASE,
    SNAPSHOT,
    OLD_BETA,
    OLD_ALPHA,
    UNKNOWN;

    public static VersionType fromManifest(String raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        return switch (raw) {
            case "release" -> RELEASE;
            case "snapshot" -> SNAPSHOT;
            case "old_beta" -> OLD_BETA;
            case "old_alpha" -> OLD_ALPHA;
            default -> UNKNOWN;
        };
    }
}
