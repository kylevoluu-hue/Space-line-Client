package com.spaceline.launcher.java;

import java.nio.file.Path;

/**
 * A discovered Java installation usable to launch Minecraft.
 *
 * @param home           the JDK/JRE home directory
 * @param executable     the {@code java}/{@code javaw} binary
 * @param majorVersion   the feature/major version (e.g. 21)
 * @param fullVersion    the full version string (e.g. {@code "21.0.4"})
 * @param vendor         the runtime vendor, when known
 * @param is64Bit        whether this is a 64-bit runtime (32-bit is unsupported)
 */
public record JavaRuntime(
        Path home,
        Path executable,
        int majorVersion,
        String fullVersion,
        String vendor,
        boolean is64Bit) {

    /** Space~line supports Java 21 through 25 inclusive. */
    public boolean isSupported() {
        return is64Bit && majorVersion >= 21 && majorVersion <= 25;
    }

    /** Whether this runtime satisfies a version family's required Java major. */
    public boolean satisfies(int requiredMajor) {
        return majorVersion >= requiredMajor && isSupported();
    }

    public String describe() {
        return "Java " + fullVersion + " (" + vendor + ") @ " + home;
    }
}
