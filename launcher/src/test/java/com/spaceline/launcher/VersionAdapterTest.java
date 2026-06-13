package com.spaceline.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.spaceline.launcher.version.MinecraftVersion;
import com.spaceline.launcher.version.SemanticVersion;
import com.spaceline.launcher.version.VersionType;
import com.spaceline.launcher.version.adapter.VersionAdapter;
import com.spaceline.launcher.version.adapter.VersionAdapterRegistry;
import org.junit.jupiter.api.Test;

class VersionAdapterTest {

    private MinecraftVersion release(String id) {
        return new MinecraftVersion(id, VersionType.RELEASE, "url", "sha", "2024-01-01");
    }

    @Test
    void rejectsVersionsBelowMinimum() {
        assertFalse(release("1.20.4").isSupported());
        assertFalse(release("1.16.5").isSupported());
        assertTrue(release("1.21").isSupported());
        assertTrue(release("1.21.4").isSupported());
    }

    @Test
    void snapshotsAreNeverSupported() {
        MinecraftVersion snapshot = new MinecraftVersion(
                "24w14a", VersionType.SNAPSHOT, "url", "sha", "2024");
        assertFalse(snapshot.isSupported());
    }

    @Test
    void selectsModern121Adapter() {
        VersionAdapterRegistry registry = VersionAdapterRegistry.withDefaults();
        VersionAdapter adapter = registry.require(release("1.21.4"));
        assertEquals("1.21.x", adapter.family());
        assertEquals(21, adapter.requiredJavaMajor());
        assertTrue(adapter.usesModernArguments());
    }

    @Test
    void futureReleasesFallBackToForwardAdapter() {
        VersionAdapterRegistry registry = VersionAdapterRegistry.withDefaults();
        VersionAdapter adapter = registry.require(release("1.23.1"));
        assertEquals("1.22+", adapter.family());
    }

    @Test
    void unsupportedLoaderIsRejected() {
        VersionAdapter adapter = VersionAdapterRegistry.withDefaults().require(release("1.21"));
        assertThrows(VersionAdapter.UnsupportedLoaderException.class,
                () -> adapter.verifyLoaderSupported("forge"));
        adapter.verifyLoaderSupported("fabric"); // does not throw
        adapter.verifyLoaderSupported("vanilla");
    }

    @Test
    void semanticVersionOrders() {
        assertTrue(SemanticVersion.parse("1.21.4").compareTo(SemanticVersion.parse("1.21")) > 0);
        assertTrue(SemanticVersion.parse("1.21").compareTo(SemanticVersion.parse("1.22")) < 0);
        assertFalse(SemanticVersion.parse("24w14a").isValid());
    }
}
