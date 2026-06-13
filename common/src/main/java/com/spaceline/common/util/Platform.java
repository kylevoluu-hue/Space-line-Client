package com.spaceline.common.util;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Cross-platform OS detection and per-user data directory resolution.
 *
 * <p>Space~line stores all of its data (accounts, instances, downloaded
 * versions, caches) under a single root that follows each platform's
 * conventions:
 * <ul>
 *   <li>Windows: {@code %APPDATA%\.spaceline}</li>
 *   <li>macOS:   {@code ~/Library/Application Support/spaceline}</li>
 *   <li>Linux:   {@code $XDG_DATA_HOME/spaceline} or {@code ~/.local/share/spaceline}</li>
 * </ul>
 */
public enum Platform {
    WINDOWS,
    MACOS,
    LINUX,
    UNKNOWN;

    private static final Platform CURRENT = detect();

    public static Platform current() {
        return CURRENT;
    }

    private static Platform detect() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return WINDOWS;
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return MACOS;
        }
        if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return LINUX;
        }
        return UNKNOWN;
    }

    /** The Mojang launcher meta uses these OS names; adapters reuse them. */
    public String mojangName() {
        return switch (this) {
            case WINDOWS -> "windows";
            case MACOS -> "osx";
            case LINUX -> "linux";
            case UNKNOWN -> "unknown";
        };
    }

    /** The native classifier used by LWJGL / natives folders. */
    public boolean isWindows() {
        return this == WINDOWS;
    }

    /** Resolves the platform-appropriate Space~line data root. */
    public static Path dataRoot() {
        String override = System.getProperty("spaceline.dataDir");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        String home = System.getProperty("user.home", ".");
        return switch (CURRENT) {
            case WINDOWS -> {
                String appData = System.getenv("APPDATA");
                Path base = (appData != null && !appData.isBlank()) ? Path.of(appData) : Path.of(home);
                yield base.resolve(".spaceline");
            }
            case MACOS -> Path.of(home, "Library", "Application Support", "spaceline");
            default -> {
                String xdg = System.getenv("XDG_DATA_HOME");
                Path base = (xdg != null && !xdg.isBlank()) ? Path.of(xdg) : Path.of(home, ".local", "share");
                yield base.resolve("spaceline");
            }
        };
    }

    /** The executable name of the bundled/located Java runtime on this OS. */
    public String javaExecutableName() {
        return this == WINDOWS ? "javaw.exe" : "java";
    }
}
