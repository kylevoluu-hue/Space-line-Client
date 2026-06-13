package com.spaceline.launcher.version.adapter;

import java.util.Set;

/**
 * Forward-compatible fallback for any stable release newer than the explicitly
 * modelled families (currently 1.22+).
 *
 * <p>It assumes the modern argument format and a Java 21+ baseline, which is a
 * safe lower bound for anything Mojang ships after 1.21. When such a version
 * actually arrives and its real requirements are known, replace this with a
 * concrete adapter. Keeping the fallback means the launcher degrades gracefully
 * rather than failing outright on a brand-new release.
 */
public final class FutureReleaseAdapter extends AbstractVersionAdapter {

    public FutureReleaseAdapter() {
        super("1.22+", "1.22", null, 21, Set.of("vanilla", "fabric"));
    }
}
