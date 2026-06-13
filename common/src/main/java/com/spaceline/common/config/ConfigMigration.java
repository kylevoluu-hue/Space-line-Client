package com.spaceline.common.config;

import com.google.gson.JsonObject;

/**
 * Transforms a config document from one schema version to the next.
 *
 * <p>Migrations operate on the raw {@link JsonObject} (not the typed object) so
 * that fields which no longer exist in the current model can still be read,
 * renamed or dropped. Each migration advances the document by exactly one
 * version: a migration with {@link #fromVersion()} == 3 produces a version-4
 * document.
 */
public interface ConfigMigration {

    /** The schema version this migration upgrades <em>from</em>. */
    int fromVersion();

    /**
     * Mutates {@code document} in place so it conforms to {@code fromVersion + 1}.
     * Implementations should be defensive: a field may be missing if the file was
     * hand-edited.
     */
    void migrate(JsonObject document);
}
