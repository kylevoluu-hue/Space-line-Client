package com.spaceline.common.net;

/**
 * Compile-time build constants. Kept tiny and dependency-free so it can be used
 * from the lowest-level networking code.
 */
public final class Build {

    /** Mirrors {@code spaceLineVersion} in gradle.properties. */
    public static final String VERSION = "0.1.0";

    private Build() {
    }
}
