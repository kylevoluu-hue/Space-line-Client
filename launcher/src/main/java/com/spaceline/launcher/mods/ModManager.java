package com.spaceline.launcher.mods;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the mod jars of a single instance: importing uploaded {@code .jar}s,
 * listing them with parsed metadata, toggling each via the {@code .disabled}
 * suffix convention and removing them. Dependency/conflict validation is
 * delegated to {@link DependencyChecker}.
 */
public final class ModManager {

    private static final Logger LOG = LoggerFactory.getLogger(ModManager.class);

    private final Path modsDir;
    private final FabricModReader reader = new FabricModReader();
    private final DependencyChecker dependencyChecker = new DependencyChecker();

    public ModManager(Path modsDir) {
        this.modsDir = modsDir;
    }

    /** Copies an uploaded jar into the instance, returning the installed file. */
    public ModFile importMod(Path source) throws IOException {
        String name = source.getFileName().toString();
        if (!name.toLowerCase().endsWith(".jar")) {
            throw new IllegalArgumentException("Not a .jar mod file: " + name);
        }
        Files.createDirectories(modsDir);
        Path target = modsDir.resolve(name);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        LOG.info("Imported mod {}", name);
        return new ModFile(target, reader.read(target));
    }

    public List<ModFile> list() {
        List<ModFile> result = new ArrayList<>();
        if (!Files.isDirectory(modsDir)) {
            return result;
        }
        try (var stream = Files.list(modsDir)) {
            stream.filter(ModManager::isModJar)
                    .forEach(path -> result.add(new ModFile(path, reader.read(path))));
        } catch (IOException e) {
            LOG.error("Failed to list mods in {}", modsDir, e);
        }
        return result;
    }

    /** Enables a mod by stripping the {@code .disabled} suffix. */
    public void enable(ModFile mod) throws IOException {
        if (mod.isEnabled()) {
            return;
        }
        String name = mod.fileName();
        Path target = modsDir.resolve(name.substring(0, name.length() - ModFile.DISABLED_SUFFIX.length()));
        Files.move(mod.path(), target, StandardCopyOption.REPLACE_EXISTING);
        LOG.info("Enabled mod {}", target.getFileName());
    }

    /** Disables a mod by appending the {@code .disabled} suffix. */
    public void disable(ModFile mod) throws IOException {
        if (!mod.isEnabled()) {
            return;
        }
        Path target = modsDir.resolve(mod.fileName() + ModFile.DISABLED_SUFFIX);
        Files.move(mod.path(), target, StandardCopyOption.REPLACE_EXISTING);
        LOG.info("Disabled mod {}", mod.fileName());
    }

    public void remove(ModFile mod) throws IOException {
        Files.deleteIfExists(mod.path());
        LOG.info("Removed mod {}", mod.fileName());
    }

    /** Runs full dependency/conflict/version validation over the current mods. */
    public ModValidation validate(String minecraftVersion) {
        return dependencyChecker.validate(list(), minecraftVersion);
    }

    private static boolean isModJar(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".jar") || name.endsWith(".jar" + ModFile.DISABLED_SUFFIX);
    }
}
