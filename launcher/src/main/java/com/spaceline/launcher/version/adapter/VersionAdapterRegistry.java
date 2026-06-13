package com.spaceline.launcher.version.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.spaceline.launcher.version.MinecraftVersion;
import com.spaceline.launcher.version.SemanticVersion;

/**
 * Resolves the correct {@link VersionAdapter} for a given Minecraft version.
 *
 * <p>Adapters are tried in registration order and the first that
 * {@link VersionAdapter#handles(SemanticVersion) handles} the version wins, so
 * more specific families must be registered before broader fallbacks. The
 * default set covers everything Space~line supports (1.21.x and the forward
 * fallback for 1.22+).
 */
public final class VersionAdapterRegistry {

    private final List<VersionAdapter> adapters = new ArrayList<>();

    /** Builds the registry preloaded with the built-in adapters. */
    public static VersionAdapterRegistry withDefaults() {
        VersionAdapterRegistry registry = new VersionAdapterRegistry();
        registry.register(new Modern121Adapter());
        registry.register(new FutureReleaseAdapter());
        return registry;
    }

    public void register(VersionAdapter adapter) {
        adapters.add(adapter);
    }

    public Optional<VersionAdapter> find(SemanticVersion version) {
        return adapters.stream().filter(a -> a.handles(version)).findFirst();
    }

    public VersionAdapter require(MinecraftVersion version) {
        return find(version.semantic()).orElseThrow(() -> new IllegalStateException(
                "No version adapter handles " + version.id()
                        + " — Space~line supports stable releases from "
                        + MinecraftVersion.MINIMUM_SUPPORTED + " onward"));
    }

    public List<VersionAdapter> all() {
        return List.copyOf(adapters);
    }
}
