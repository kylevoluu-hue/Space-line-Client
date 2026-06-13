package com.spaceline.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads and saves a single typed, versioned config file as JSON.
 *
 * <p>The schema version is persisted alongside the data under
 * {@value #VERSION_KEY}. On load, any registered {@link ConfigMigration}s are
 * applied in ascending order to upgrade an older document before it is
 * deserialized, and a timestamped backup of the pre-migration file is written so
 * a bad migration can never silently destroy user data. Saves are atomic: the
 * file is written to a temp sibling and then moved into place.
 *
 * @param <T> the typed config model; must implement {@link Versioned}
 */
public final class ConfigManager<T extends Versioned> {

    static final String VERSION_KEY = "configVersion";
    private static final Logger LOG = LoggerFactory.getLogger(ConfigManager.class);

    private final Path file;
    private final Class<T> type;
    private final java.util.function.Supplier<T> defaultFactory;
    private final List<ConfigMigration> migrations = new ArrayList<>();

    public ConfigManager(Path file, Class<T> type, java.util.function.Supplier<T> defaultFactory) {
        this.file = file;
        this.type = type;
        this.defaultFactory = defaultFactory;
    }

    /** Registers a migration. Order of registration does not matter. */
    public ConfigManager<T> withMigration(ConfigMigration migration) {
        migrations.add(migration);
        migrations.sort(Comparator.comparingInt(ConfigMigration::fromVersion));
        return this;
    }

    /**
     * Loads the config, creating and persisting defaults if the file is missing
     * and migrating it forward if it is from an older schema. Never throws for a
     * merely-absent file; a corrupt file is backed up and replaced with defaults.
     */
    public T load() {
        if (!Files.exists(file)) {
            T defaults = defaultFactory.get();
            save(defaults);
            return defaults;
        }
        String raw;
        try {
            raw = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Failed to read config {}, using defaults", file, e);
            return defaultFactory.get();
        }

        try {
            JsonObject document = Json.parseObject(raw);
            int onDisk = readVersion(document);
            int target = defaultFactory.get().currentVersion();
            if (onDisk < target) {
                backup(onDisk);
                document = runMigrations(document, onDisk, target);
            } else if (onDisk > target) {
                LOG.warn("Config {} is from a newer version ({} > {}); loading best-effort",
                        file, onDisk, target);
            }
            T value = Json.gson().fromJson(document, type);
            return value != null ? value : defaultFactory.get();
        } catch (RuntimeException e) {
            LOG.error("Config {} is corrupt; backing up and resetting to defaults", file, e);
            backupCorrupt();
            T defaults = defaultFactory.get();
            save(defaults);
            return defaults;
        }
    }

    /** Atomically persists {@code value}, stamping the current schema version. */
    public void save(T value) {
        try {
            Files.createDirectories(file.getParent());
            JsonObject document = Json.gson().toJsonTree(value).getAsJsonObject();
            document.add(VERSION_KEY, new JsonPrimitive(value.currentVersion()));
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, Json.gson().toJson(document), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.error("Failed to save config {}", file, e);
        }
    }

    private JsonObject runMigrations(JsonObject document, int from, int to) {
        LOG.info("Migrating config {} from v{} to v{}", file, from, to);
        for (int v = from; v < to; v++) {
            for (ConfigMigration migration : migrations) {
                if (migration.fromVersion() == v) {
                    migration.migrate(document);
                }
            }
            document.add(VERSION_KEY, new JsonPrimitive(v + 1));
        }
        return document;
    }

    private static int readVersion(JsonObject document) {
        return document.has(VERSION_KEY) ? document.get(VERSION_KEY).getAsInt() : 1;
    }

    private void backup(int version) {
        copyTo(file.resolveSibling(file.getFileName() + ".v" + version + ".bak"));
    }

    private void backupCorrupt() {
        copyTo(file.resolveSibling(file.getFileName() + ".corrupt." + System.currentTimeMillis() + ".bak"));
    }

    private void copyTo(Path destination) {
        try {
            Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOG.warn("Could not back up config {} to {}", file, destination, e);
        }
    }

    public Path file() {
        return file;
    }
}
