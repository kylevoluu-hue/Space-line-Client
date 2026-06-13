package com.spaceline.launcher.content;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages an instance's shader packs (as used by Iris/Oculus-style mods):
 * importing uploaded {@code .zip} packs, selecting the active one and applying a
 * coarse performance preset.
 *
 * <p>Unlike resource packs, only one shader pack is active at a time, so this
 * tracks a single selection rather than an ordered list.
 */
public final class ShaderManager {

    private static final Logger LOG = LoggerFactory.getLogger(ShaderManager.class);

    /** Coarse quality presets the UI can offer for quick performance tuning. */
    public enum PerformancePreset {
        POTATO, LOW, MEDIUM, HIGH, ULTRA
    }

    private final Path shadersDir;
    private final Path selectionFile;
    private PerformancePreset preset = PerformancePreset.MEDIUM;

    public ShaderManager(Path shadersDir) {
        this.shadersDir = shadersDir;
        this.selectionFile = shadersDir.resolve("active.shader");
    }

    public Path importShader(Path source) throws IOException {
        Files.createDirectories(shadersDir);
        Path target = shadersDir.resolve(source.getFileName().toString());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        LOG.info("Imported shader pack {}", target.getFileName());
        return target;
    }

    public List<String> available() {
        List<String> result = new ArrayList<>();
        if (!Files.isDirectory(shadersDir)) {
            return result;
        }
        try (var stream = Files.list(shadersDir)) {
            stream.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".zip"))
                    .forEach(p -> result.add(p.getFileName().toString()));
        } catch (IOException e) {
            LOG.error("Failed to list shader packs", e);
        }
        return result;
    }

    public void enable(String shaderName) throws IOException {
        Files.createDirectories(shadersDir);
        Files.writeString(selectionFile, shaderName);
        LOG.info("Enabled shader pack {}", shaderName);
    }

    public void disable() throws IOException {
        Files.deleteIfExists(selectionFile);
        LOG.info("Disabled shaders");
    }

    public Optional<String> active() {
        try {
            return Files.exists(selectionFile)
                    ? Optional.of(Files.readString(selectionFile).trim())
                    : Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public PerformancePreset preset() {
        return preset;
    }

    public void setPreset(PerformancePreset preset) {
        this.preset = preset;
        LOG.info("Shader performance preset set to {}", preset);
    }
}
