package com.spaceline.launcher.content;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages an instance's resource packs: importing uploaded packs (zip or folder),
 * enabling/disabling them and controlling their load order.
 *
 * <p>Minecraft applies resource packs in order, later packs overriding earlier
 * ones. The enabled set and its order are persisted in {@code resourcepacks.order}
 * so the launcher can write the matching {@code resourcePacks} list into the
 * game's options on launch.
 */
public final class ResourcePackManager {

    private static final Logger LOG = LoggerFactory.getLogger(ResourcePackManager.class);

    private final Path packsDir;
    private final LoadOrder order;

    public ResourcePackManager(Path packsDir) {
        this.packsDir = packsDir;
        this.order = new LoadOrder(packsDir.resolve("resourcepacks.order"));
    }

    /** Copies an uploaded pack into the instance. */
    public Path importPack(Path source) throws IOException {
        Files.createDirectories(packsDir);
        Path target = packsDir.resolve(source.getFileName().toString());
        if (Files.isDirectory(source)) {
            copyTree(source, target);
        } else {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
        LOG.info("Imported resource pack {}", target.getFileName());
        return target;
    }

    public List<String> available() {
        List<String> packs = new ArrayList<>();
        if (!Files.isDirectory(packsDir)) {
            return packs;
        }
        try (var stream = Files.list(packsDir)) {
            stream.filter(p -> isPack(p))
                    .forEach(p -> packs.add(p.getFileName().toString()));
        } catch (IOException e) {
            LOG.error("Failed to list resource packs", e);
        }
        return packs;
    }

    public List<String> enabledInOrder() {
        return order.load();
    }

    public void enable(String packName) {
        order.add(packName);
    }

    public void disable(String packName) {
        order.remove(packName);
    }

    /** Moves a pack up/down in the load order (clamped to the bounds). */
    public void move(String packName, int delta) {
        order.move(packName, delta);
    }

    private static boolean isPack(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".zip") || Files.isDirectory(path);
    }

    private static void copyTree(Path source, Path target) throws IOException {
        try (var walk = Files.walk(source)) {
            for (Path path : (Iterable<Path>) walk::iterator) {
                Path rel = source.relativize(path);
                Path dest = target.resolve(rel.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
