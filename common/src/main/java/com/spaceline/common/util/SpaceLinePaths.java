package com.spaceline.common.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves the canonical sub-directories of the Space~line data root and ensures
 * they exist. Centralising the layout here keeps every subsystem (versions,
 * instances, accounts, caches) agreeing on where things live.
 */
public final class SpaceLinePaths {

    private final Path root;

    public SpaceLinePaths() {
        this(Platform.dataRoot());
    }

    public SpaceLinePaths(Path root) {
        this.root = root;
    }

    public Path root() {
        return ensure(root);
    }

    /** Downloaded Minecraft versions, libraries and asset indexes. */
    public Path versions() {
        return ensure(root.resolve("versions"));
    }

    public Path libraries() {
        return ensure(root.resolve("libraries"));
    }

    public Path assets() {
        return ensure(root.resolve("assets"));
    }

    /** Per-instance game directories (saves, options, mods, etc.). */
    public Path instances() {
        return ensure(root.resolve("instances"));
    }

    public Path instance(String id) {
        return ensure(instances().resolve(id));
    }

    /** Account store + auth token cache. */
    public Path accounts() {
        return ensure(root.resolve("accounts"));
    }

    /** Detected/installed Java runtimes metadata. */
    public Path runtimes() {
        return ensure(root.resolve("runtimes"));
    }

    /** Skins and capes uploaded by the user. */
    public Path skins() {
        return ensure(root.resolve("skins"));
    }

    public Path capes() {
        return ensure(root.resolve("capes"));
    }

    /** UI themes (built-in extracted + user uploaded). */
    public Path themes() {
        return ensure(root.resolve("themes"));
    }

    /** Crash reports and exported error bundles. */
    public Path crashReports() {
        return ensure(root.resolve("crash-reports"));
    }

    /** HTTP / metadata cache. */
    public Path cache() {
        return ensure(root.resolve("cache"));
    }

    public Path logs() {
        return ensure(root.resolve("logs"));
    }

    private static Path ensure(Path path) {
        try {
            Files.createDirectories(path);
            return path;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create directory " + path, e);
        }
    }
}
