package com.spaceline.launcher.version.adapter;

import java.util.List;
import java.util.Set;

import com.spaceline.launcher.version.SemanticVersion;

/**
 * Convenience base implementing the parts of {@link VersionAdapter} that are
 * uniform across the modern (1.13+) version families, leaving subclasses to
 * declare their range, required Java and supported loaders.
 */
public abstract class AbstractVersionAdapter implements VersionAdapter {

    private final String family;
    private final SemanticVersion lowerInclusive;
    private final SemanticVersion upperExclusive;
    private final int requiredJavaMajor;
    private final Set<String> supportedLoaders;

    /**
     * @param upperExclusive the first version this adapter does <em>not</em>
     *                       handle, or {@code null} for "no upper bound"
     */
    protected AbstractVersionAdapter(String family, String lowerInclusive, String upperExclusive,
                                     int requiredJavaMajor, Set<String> supportedLoaders) {
        this.family = family;
        this.lowerInclusive = SemanticVersion.parse(lowerInclusive);
        this.upperExclusive = upperExclusive == null ? null : SemanticVersion.parse(upperExclusive);
        this.requiredJavaMajor = requiredJavaMajor;
        this.supportedLoaders = supportedLoaders;
    }

    @Override
    public String family() {
        return family;
    }

    @Override
    public boolean handles(SemanticVersion version) {
        if (!version.isValid() || version.compareTo(lowerInclusive) < 0) {
            return false;
        }
        return upperExclusive == null || version.compareTo(upperExclusive) < 0;
    }

    @Override
    public int requiredJavaMajor() {
        return requiredJavaMajor;
    }

    @Override
    public boolean usesModernArguments() {
        return true;
    }

    @Override
    public List<String> defaultJvmArgs() {
        // Conservative, broadly-good defaults; families can override.
        return List.of(
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseG1GC",
                "-XX:G1NewSizePercent=20",
                "-XX:G1ReservePercent=20",
                "-XX:MaxGCPauseMillis=50",
                "-XX:G1HeapRegionSize=32M");
    }

    @Override
    public void verifyLoaderSupported(String loaderId) {
        if (!supportedLoaders.contains(loaderId)) {
            throw new UnsupportedLoaderException(
                    "Loader '" + loaderId + "' is not supported on Minecraft " + family
                            + " (supported: " + String.join(", ", supportedLoaders) + ")");
        }
    }
}
