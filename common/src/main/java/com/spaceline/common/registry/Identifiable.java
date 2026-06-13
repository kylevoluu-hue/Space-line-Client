package com.spaceline.common.registry;

/**
 * Anything that can live in a {@link Registry}. The id must be stable across
 * runs (it is used as a config key) and unique within its registry.
 */
public interface Identifiable {

    /**
     * @return a stable, lowercase, dash-or-underscore-free identifier such as
     *         {@code "cps"} or {@code "fullbright"}.
     */
    String id();
}
