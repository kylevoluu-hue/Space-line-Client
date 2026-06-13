package com.spaceline.launcher.instance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.spaceline.common.config.ConfigManager;
import com.spaceline.common.util.SpaceLinePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates, lists, loads and deletes {@link Instance}s, each stored in its own
 * directory under {@code instances/} with an {@code instance.json} descriptor and
 * the standard {@code mods/}, {@code resourcepacks/}, {@code shaderpacks/} and
 * {@code saves/} sub-folders.
 */
public final class InstanceManager {

    private static final Logger LOG = LoggerFactory.getLogger(InstanceManager.class);

    private final SpaceLinePaths paths;

    public InstanceManager(SpaceLinePaths paths) {
        this.paths = paths;
    }

    /** Creates a new instance directory tree and persists its descriptor. */
    public Instance create(String name, String minecraftVersion, ModLoader loader) {
        String id = slugify(name);
        Path dir = paths.instance(id);
        Instance instance = new Instance(id, name, minecraftVersion, loader);
        prepareDirectories(dir);
        configFor(id).save(instance);
        LOG.info("Created instance '{}' ({} {})", id, minecraftVersion, loader.id());
        return instance;
    }

    public List<Instance> list() {
        List<Instance> result = new ArrayList<>();
        Path root = paths.instances();
        try (var stream = Files.list(root)) {
            stream.filter(Files::isDirectory).forEach(dir -> {
                Path descriptor = dir.resolve("instance.json");
                if (Files.exists(descriptor)) {
                    result.add(configFor(dir.getFileName().toString()).load());
                }
            });
        } catch (IOException e) {
            LOG.error("Failed to list instances", e);
        }
        return result;
    }

    public Optional<Instance> get(String id) {
        Path descriptor = paths.instances().resolve(id).resolve("instance.json");
        return Files.exists(descriptor) ? Optional.of(configFor(id).load()) : Optional.empty();
    }

    public void save(Instance instance) {
        configFor(instance.id()).save(instance);
    }

    public void delete(String id) {
        Path dir = paths.instances().resolve(id);
        try (var walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(InstanceManager::deleteQuietly);
            LOG.info("Deleted instance '{}'", id);
        } catch (IOException e) {
            LOG.error("Failed to delete instance '{}'", id, e);
        }
    }

    /** The game directory passed to the JVM as the working directory. */
    public Path gameDir(String id) {
        return paths.instance(id);
    }

    public Path modsDir(String id) {
        return ensure(paths.instance(id).resolve("mods"));
    }

    public Path resourcePacksDir(String id) {
        return ensure(paths.instance(id).resolve("resourcepacks"));
    }

    public Path shaderPacksDir(String id) {
        return ensure(paths.instance(id).resolve("shaderpacks"));
    }

    private void prepareDirectories(Path dir) {
        ensure(dir.resolve("mods"));
        ensure(dir.resolve("resourcepacks"));
        ensure(dir.resolve("shaderpacks"));
        ensure(dir.resolve("saves"));
        ensure(dir.resolve("config"));
    }

    private ConfigManager<Instance> configFor(String id) {
        Path descriptor = paths.instances().resolve(id).resolve("instance.json");
        return new ConfigManager<>(descriptor, Instance.class, Instance::new);
    }

    private static String slugify(String name) {
        String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "instance-" + System.currentTimeMillis() : slug;
    }

    private static Path ensure(Path path) {
        try {
            Files.createDirectories(path);
            return path;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort recursive delete.
        }
    }
}
