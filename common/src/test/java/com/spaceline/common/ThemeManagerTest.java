package com.spaceline.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.spaceline.common.ui.Theme;
import com.spaceline.common.ui.ThemeManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ThemeManagerTest {

    @Test
    void providesFiveBuiltInThemes(@TempDir Path dir) {
        ThemeManager manager = new ThemeManager(dir);
        assertEquals(5, manager.all().size());
        for (String id : new String[]{"dark", "light", "glass", "neon", "minimal"}) {
            assertTrue(manager.get(id).isPresent(), id + " should be built in");
            assertTrue(manager.get(id).get().builtIn());
        }
    }

    @Test
    void defaultsToDarkAndSwitches(@TempDir Path dir) {
        ThemeManager manager = new ThemeManager(dir);
        assertEquals("dark", manager.active().id());
        manager.setActive("neon");
        assertEquals("neon", manager.active().id());
        assertThrows(IllegalArgumentException.class, () -> manager.setActive("nope"));
    }

    @Test
    void uploadsUserTheme(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("custom.json");
        Files.writeString(file, "{\"id\":\"custom\",\"name\":\"Custom\"}");
        ThemeManager manager = new ThemeManager(dir);
        Theme theme = manager.upload(file);
        assertEquals("custom", theme.id());
        assertEquals(6, manager.all().size());
        assertTrue(Files.exists(dir.resolve("custom.json")));
    }
}
