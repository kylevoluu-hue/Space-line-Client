package com.spaceline.launcher.version.adapter;

import java.util.Set;

/**
 * Adapter for the 1.21.x family — Space~line's baseline.
 *
 * <p>1.21 raised Mojang's required runtime to Java 21. Both vanilla and Fabric
 * are supported here. The upper bound is left open so future 1.21 patch releases
 * (1.21.5, 1.21.6, …) are handled by this same adapter; when a new line such as
 * 1.22 ships with different requirements, a sibling adapter is registered with a
 * higher specificity.
 */
public final class Modern121Adapter extends AbstractVersionAdapter {

    public Modern121Adapter() {
        super("1.21.x", "1.21", "1.22", 21, Set.of("vanilla", "fabric"));
    }
}
