package com.spaceline.launcher.mods;

import java.nio.file.Path;

/**
 * A mod jar installed in an instance's {@code mods/} folder, together with its
 * parsed metadata and enabled state.
 *
 * <p>Enable/disable is implemented the same way the Fabric ecosystem expects: a
 * disabled mod keeps a {@code .jar.disabled} suffix so the loader ignores it,
 * while an enabled mod ends in {@code .jar}. This keeps the on-disk state the
 * single source of truth and survives the launcher not running.
 */
public final class ModFile {

    public static final String DISABLED_SUFFIX = ".disabled";

    private final Path path;
    private final ModMetadata metadata;

    public ModFile(Path path, ModMetadata metadata) {
        this.path = path;
        this.metadata = metadata;
    }

    public Path path() {
        return path;
    }

    public ModMetadata metadata() {
        return metadata;
    }

    public boolean isEnabled() {
        return !path.getFileName().toString().endsWith(DISABLED_SUFFIX);
    }

    public String fileName() {
        return path.getFileName().toString();
    }
}
