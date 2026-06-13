package com.spaceline.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.spaceline.common.config.ConfigManager;
import com.spaceline.common.config.Versioned;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigMigrationTest {

    /** A config whose schema reached v2 by renaming {@code user -> username}. */
    static final class Settings implements Versioned {
        String username = "Steve";
        int volume = 100;

        @Override
        public int currentVersion() {
            return 2;
        }
    }

    @Test
    void createsDefaultsWhenMissing(@TempDir Path dir) {
        ConfigManager<Settings> manager = new ConfigManager<>(
                dir.resolve("settings.json"), Settings.class, Settings::new);
        Settings loaded = manager.load();
        assertEquals("Steve", loaded.username);
        assertTrue(Files.exists(dir.resolve("settings.json")));
    }

    @Test
    void migratesOldDocumentForward(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("settings.json");
        // A v1 document using the old field name and no version stamp upgrade.
        Files.writeString(file, "{\"configVersion\":1,\"user\":\"Alex\",\"volume\":50}");

        ConfigManager<Settings> manager = new ConfigManager<Settings>(file, Settings.class, Settings::new)
                .withMigration(new com.spaceline.common.config.ConfigMigration() {
                    @Override
                    public int fromVersion() {
                        return 1;
                    }

                    @Override
                    public void migrate(com.google.gson.JsonObject document) {
                        if (document.has("user")) {
                            document.add("username", document.remove("user"));
                        }
                    }
                });

        Settings loaded = manager.load();
        assertEquals("Alex", loaded.username);
        assertEquals(50, loaded.volume);
        // A backup of the pre-migration v1 file must exist.
        assertTrue(Files.exists(dir.resolve("settings.json.v1.bak")));
    }

    @Test
    void roundTripsThroughSave(@TempDir Path dir) {
        Path file = dir.resolve("settings.json");
        ConfigManager<Settings> manager = new ConfigManager<>(file, Settings.class, Settings::new);
        Settings settings = new Settings();
        settings.username = "Notch";
        manager.save(settings);
        assertEquals("Notch", manager.load().username);
    }
}
