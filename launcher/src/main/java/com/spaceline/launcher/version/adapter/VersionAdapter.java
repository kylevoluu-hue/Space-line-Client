package com.spaceline.launcher.version.adapter;

import java.util.List;

import com.spaceline.launcher.version.SemanticVersion;

/**
 * Isolates every behavioural difference between Minecraft version families
 * behind a single, version-agnostic interface.
 *
 * <p>This is the "version abstraction / adapter layer" the launcher is built
 * around: the launch pipeline, Java selection and mod-loader validation all talk
 * to a {@code VersionAdapter} rather than special-casing individual releases.
 * When Mojang ships a new line that changes, say, the required Java major or the
 * argument format, we add (or adjust) one adapter instead of threading
 * {@code if (version.equals("1.21.4"))} checks through the codebase.
 *
 * <p>Adapters are selected by {@link VersionAdapterRegistry} using
 * {@link #handles(SemanticVersion)}; the most specific match wins.
 */
public interface VersionAdapter {

    /** A short, stable name for logs/UI, e.g. {@code "1.21.x"}. */
    String family();

    /** Whether this adapter is responsible for the given version. */
    boolean handles(SemanticVersion version);

    /**
     * The Java major version Mojang requires for this family. Space~line will
     * refuse to launch (or warn) if the selected runtime is older.
     */
    int requiredJavaMajor();

    /**
     * Whether this family uses the modern split {@code arguments} object
     * (game/jvm arrays with rules) introduced in 1.13, as opposed to the legacy
     * flat {@code minecraftArguments} string. Everything from 1.21 does, but the
     * abstraction keeps the launch code honest.
     */
    boolean usesModernArguments();

    /**
     * Extra JVM flags this family benefits from (e.g. modern G1/ZGC tuning). The
     * launcher appends these after the user's own JVM args.
     */
    List<String> defaultJvmArgs();

    /**
     * Validates that {@code loaderId} (e.g. {@code "fabric"}, {@code "vanilla"})
     * is supported for this family, throwing {@link UnsupportedLoaderException}
     * with an actionable message otherwise.
     */
    void verifyLoaderSupported(String loaderId);

    /** Thrown when a requested mod loader cannot run on a version family. */
    class UnsupportedLoaderException extends RuntimeException {
        public UnsupportedLoaderException(String message) {
            super(message);
        }
    }
}
