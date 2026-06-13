package com.spaceline.launcher.bundled;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.spaceline.launcher.browser.ModBrowser;
import com.spaceline.launcher.instance.ModLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs the always-up-to-date "essential" mods into a Fabric instance:
 * <b>Fabric API</b>, <b>Sodium</b> and <b>FerriteCore</b>. Each is resolved fresh
 * from Modrinth for the instance's exact Minecraft version, so they track the
 * newest compatible build rather than shipping a frozen jar.
 *
 * <p>These are only installed for the Fabric loader (vanilla instances get
 * nothing), and the set is opt-in per instance so users who want a clean setup
 * aren't forced into it.
 */
public final class BundledMods {

    private static final Logger LOG = LoggerFactory.getLogger(BundledMods.class);

    /** Modrinth slugs of the bundled essentials, in dependency-friendly order. */
    public static final List<String> ESSENTIAL_SLUGS = List.of("fabric-api", "sodium", "ferrite-core");

    private final ModBrowser browser;

    public BundledMods(ModBrowser browser) {
        this.browser = browser;
    }

    /**
     * Ensures the essential mods are present (newest compatible builds) in
     * {@code modsDir}. No-op for non-Fabric loaders. Returns the installed paths.
     */
    public List<Path> installEssentials(ModLoader loader, String minecraftVersion, Path modsDir) {
        List<Path> installed = new ArrayList<>();
        if (loader != ModLoader.FABRIC) {
            return installed;
        }
        for (String slug : ESSENTIAL_SLUGS) {
            try {
                List<Path> files = browser.installById("modrinth", slug, minecraftVersion, "fabric", modsDir);
                installed.addAll(files);
                LOG.info("Installed bundled '{}' ({} file(s))", slug, files.size());
            } catch (IOException e) {
                // A single unavailable essential shouldn't block the others or the launch.
                LOG.warn("Could not install bundled mod '{}' for {}: {}", slug, minecraftVersion, e.getMessage());
            }
        }
        return installed;
    }
}
