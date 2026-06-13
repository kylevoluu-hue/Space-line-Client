package com.spaceline.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.spaceline.launcher.profile.ModProfile;
import com.spaceline.launcher.profile.PrebuiltProfiles;
import com.spaceline.launcher.profile.ProfileManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfileManagerTest {

    private Path fakeInstance(Path root) throws IOException {
        Path instance = root.resolve("instance");
        Files.createDirectories(instance.resolve("mods"));
        Files.createDirectories(instance.resolve("resourcepacks"));
        Files.writeString(instance.resolve("mods").resolve("sodium.jar"), "fake-jar");
        Files.writeString(instance.resolve("mods").resolve("ferritecore.jar"), "fake-jar");
        Files.writeString(instance.resolve("resourcepacks").resolve("pack.zip"), "fake-pack");
        return instance;
    }

    @Test
    void shipsPrebuiltProfiles() {
        assertTrue(PrebuiltProfiles.all().size() >= 3);
        assertTrue(PrebuiltProfiles.all().stream().anyMatch(p -> p.name().equals("FPS Booster")));
        assertTrue(PrebuiltProfiles.fpsBooster().modrinthSlugs().contains("sodium"));
    }

    @Test
    void savesProfileFromInstance(@TempDir Path dir) throws IOException {
        ProfileManager manager = new ProfileManager(dir.resolve("profiles"));
        ModProfile profile = manager.saveFromInstance("My PvP", "1.21.4", "fabric", fakeInstance(dir));
        assertEquals("my-pvp", profile.id());
        assertEquals(2, profile.modFiles().size());
        assertEquals(1, profile.resourcePacks().size());
        assertTrue(manager.get("my-pvp").isPresent());
    }

    @Test
    void exportsAndImportsRoundTrip(@TempDir Path dir) throws IOException {
        ProfileManager manager = new ProfileManager(dir.resolve("profiles"));
        manager.saveFromInstance("Roundtrip", "1.21.4", "fabric", fakeInstance(dir));

        Path zip = dir.resolve("export/roundtrip.zip");
        manager.exportZip("roundtrip", zip);
        assertTrue(Files.exists(zip));

        // Import into a fresh manager (simulating another install).
        ProfileManager other = new ProfileManager(dir.resolve("profiles2"));
        ModProfile imported = other.importZip(zip);
        assertEquals("roundtrip", imported.id());
        assertEquals(2, imported.modFiles().size());
        assertTrue(other.get("roundtrip").isPresent());
    }

    @Test
    void appliesProfileToInstance(@TempDir Path dir) throws IOException {
        ProfileManager manager = new ProfileManager(dir.resolve("profiles"));
        manager.saveFromInstance("Apply", "1.21.4", "fabric", fakeInstance(dir));

        Path target = dir.resolve("target-instance");
        Files.createDirectories(target);
        manager.applyToInstance("apply", target);
        assertTrue(Files.exists(target.resolve("mods").resolve("sodium.jar")));
        assertTrue(Files.exists(target.resolve("resourcepacks").resolve("pack.zip")));
    }
}
