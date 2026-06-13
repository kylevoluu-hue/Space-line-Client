package com.spaceline.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.spaceline.launcher.launch.LaunchCommandBuilder;
import com.spaceline.launcher.launch.VersionInstaller;
import org.junit.jupiter.api.Test;

class LaunchCommandTest {

    @Test
    void substitutesPlaceholders() {
        String result = LaunchCommandBuilder.substitute(
                "--username ${auth_player_name} --uuid ${auth_uuid}",
                Map.of("auth_player_name", "Steve", "auth_uuid", "abc123"));
        assertEquals("--username Steve --uuid abc123", result);
    }

    @Test
    void leavesUnknownTokensUntouched() {
        assertEquals("plain", LaunchCommandBuilder.substitute("plain", Map.of()));
    }

    @Test
    void buildsMavenPathFromCoordinate() {
        assertEquals("net/fabricmc/fabric-loader/0.16.0/fabric-loader-0.16.0.jar",
                VersionInstaller.mavenPath("net.fabricmc:fabric-loader:0.16.0"));
    }

    @Test
    void mavenPathHandlesClassifier() {
        String path = VersionInstaller.mavenPath("org.lwjgl:lwjgl:3.3.3:natives-linux");
        assertTrue(path.endsWith("lwjgl-3.3.3-natives-linux.jar"));
    }

    @Test
    void coordinateKeyIgnoresVersionForDedup() {
        // Different ASM versions collapse to the same key (so one is dropped).
        assertEquals(VersionInstaller.coordinateKey("org.ow2.asm:asm:9.10.1"),
                VersionInstaller.coordinateKey("org.ow2.asm:asm:9.6"));
    }

    @Test
    void coordinateKeyKeepsClassifierDistinct() {
        // A library and its natives jar must NOT be de-duplicated together.
        assertNotEquals(VersionInstaller.coordinateKey("org.lwjgl:lwjgl:3.3.3"),
                VersionInstaller.coordinateKey("org.lwjgl:lwjgl:3.3.3:natives-windows"));
    }
}
