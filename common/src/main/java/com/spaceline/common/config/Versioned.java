package com.spaceline.common.config;

/**
 * A config payload that carries a schema version. The {@link ConfigManager}
 * compares the on-disk version against {@link #currentVersion()} and runs the
 * registered {@link ConfigMigration migrations} in order to bring an old file up
 * to date before it is deserialized into its final shape.
 */
public interface Versioned {

    /**
     * @return the schema version this code understands. Bump it whenever the
     *         persisted shape changes in a non-backwards-compatible way and add
     *         a matching {@link ConfigMigration}.
     */
    int currentVersion();
}
