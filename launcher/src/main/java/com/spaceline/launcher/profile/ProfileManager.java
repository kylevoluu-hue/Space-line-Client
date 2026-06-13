package com.spaceline.launcher.profile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.spaceline.common.config.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages mod profiles: saving an instance's mods/resource-packs/shaders as a
 * named profile, applying a profile back onto an instance, and exporting /
 * importing profiles as portable {@code .zip} files.
 *
 * <p>On-disk layout, one folder per profile:
 * <pre>
 *   profiles/&lt;id&gt;/profile.json
 *   profiles/&lt;id&gt;/mods/*.jar
 *   profiles/&lt;id&gt;/resourcepacks/*
 *   profiles/&lt;id&gt;/shaderpacks/*
 * </pre>
 * The export zip mirrors this structure, so it is human-readable and the
 * {@code mods/} etc. folders drop straight into other launchers' instances.
 * Profiles are unbounded — limited only by available disk space, never forced.
 */
public final class ProfileManager {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileManager.class);
    private static final List<String> CONTENT_DIRS = List.of("mods", "resourcepacks", "shaderpacks");

    private final Path profilesDir;

    public ProfileManager(Path profilesDir) {
        this.profilesDir = profilesDir;
    }

    /** Built-in starter profiles plus every user profile saved on disk. */
    public List<ModProfile> list() {
        List<ModProfile> profiles = new ArrayList<>(PrebuiltProfiles.all());
        if (Files.isDirectory(profilesDir)) {
            try (var stream = Files.list(profilesDir)) {
                stream.filter(Files::isDirectory).forEach(dir -> {
                    Path manifest = dir.resolve("profile.json");
                    if (Files.exists(manifest)) {
                        readManifest(manifest).ifPresent(profiles::add);
                    }
                });
            } catch (IOException e) {
                LOG.error("Failed to list profiles", e);
            }
        }
        return profiles;
    }

    public Optional<ModProfile> get(String id) {
        return list().stream().filter(p -> id.equals(p.id())).findFirst();
    }

    /**
     * Captures the current content of {@code instanceDir} into a new (or updated)
     * profile, copying its mods, resource packs and shaders into the profile folder.
     */
    public ModProfile saveFromInstance(String name, String minecraftVersion, String loaderId,
                                       Path instanceDir) throws IOException {
        String id = slug(name);
        Path profileDir = profilesDir.resolve(id);
        Files.createDirectories(profileDir);

        ModProfile profile = new ModProfile(id, name);
        profile.setMinecraftVersion(minecraftVersion);
        profile.setLoaderId(loaderId);

        for (String dirName : CONTENT_DIRS) {
            Path from = instanceDir.resolve(dirName);
            Path to = profileDir.resolve(dirName);
            List<String> copied = copyTree(from, to);
            switch (dirName) {
                case "mods" -> profile.modFiles().addAll(copied);
                case "resourcepacks" -> profile.resourcePacks().addAll(copied);
                case "shaderpacks" -> profile.shaderPacks().addAll(copied);
                default -> { }
            }
        }
        writeManifest(profileDir.resolve("profile.json"), profile);
        LOG.info("Saved profile '{}' ({} mods)", id, profile.modFiles().size());
        return profile;
    }

    /** Copies a stored profile's files into {@code instanceDir}. */
    public void applyToInstance(String profileId, Path instanceDir) throws IOException {
        Path profileDir = profilesDir.resolve(profileId);
        if (!Files.isDirectory(profileDir)) {
            throw new IOException("Profile '" + profileId + "' has no stored files");
        }
        for (String dirName : CONTENT_DIRS) {
            copyTree(profileDir.resolve(dirName), instanceDir.resolve(dirName));
        }
        LOG.info("Applied profile '{}' to {}", profileId, instanceDir.getFileName());
    }

    public void delete(String profileId) throws IOException {
        Path profileDir = profilesDir.resolve(profileId);
        if (Files.exists(profileDir)) {
            deleteTree(profileDir);
        }
    }

    /** Exports a profile folder to a single portable {@code .zip}. */
    public void exportZip(String profileId, Path destinationZip) throws IOException {
        Path profileDir = profilesDir.resolve(profileId);
        if (!Files.isDirectory(profileDir)) {
            throw new IOException("Nothing to export for profile '" + profileId + "'");
        }
        Files.createDirectories(destinationZip.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(destinationZip))) {
            try (var walk = Files.walk(profileDir)) {
                for (Path path : (Iterable<Path>) walk::iterator) {
                    if (Files.isRegularFile(path)) {
                        String entry = profileDir.relativize(path).toString().replace('\\', '/');
                        zip.putNextEntry(new ZipEntry(entry));
                        Files.copy(path, zip);
                        zip.closeEntry();
                    }
                }
            }
        }
        LOG.info("Exported profile '{}' to {}", profileId, destinationZip);
    }

    /** Imports a previously-exported profile zip, returning the new profile. */
    public ModProfile importZip(Path zipFile) throws IOException {
        Path temp = Files.createTempDirectory("spaceline-profile");
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                Path target = safeResolve(temp, entry.getName());
                Files.createDirectories(target.getParent());
                try (OutputStream out = Files.newOutputStream(target)) {
                    zip.transferTo(out);
                }
            }
        }
        ModProfile profile = readManifest(temp.resolve("profile.json"))
                .orElseThrow(() -> new IOException("Zip is not a valid Space~line profile (no profile.json)"));
        String id = slug(profile.name());
        profile.setId(id);
        Path dest = profilesDir.resolve(id);
        deleteTree(dest);
        Files.createDirectories(dest.getParent());
        copyTreeRecursive(temp, dest);
        writeManifest(dest.resolve("profile.json"), profile);
        deleteTree(temp);
        LOG.info("Imported profile '{}'", id);
        return profile;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Optional<ModProfile> readManifest(Path manifest) {
        try {
            return Optional.ofNullable(
                    Json.fromJson(Files.readString(manifest, StandardCharsets.UTF_8), ModProfile.class));
        } catch (IOException | RuntimeException e) {
            LOG.warn("Skipping invalid profile manifest {}", manifest, e);
            return Optional.empty();
        }
    }

    private void writeManifest(Path manifest, ModProfile profile) throws IOException {
        Files.createDirectories(manifest.getParent());
        Files.writeString(manifest, Json.toJson(profile), StandardCharsets.UTF_8);
    }

    private List<String> copyTree(Path from, Path to) throws IOException {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(from)) {
            return names;
        }
        Files.createDirectories(to);
        try (var stream = Files.list(from)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (Files.isRegularFile(path)) {
                    Files.copy(path, to.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    names.add(path.getFileName().toString());
                }
            }
        }
        return names;
    }

    private void copyTreeRecursive(Path from, Path to) throws IOException {
        try (var walk = Files.walk(from)) {
            for (Path path : (Iterable<Path>) walk::iterator) {
                Path rel = from.relativize(path);
                Path dest = to.resolve(rel.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best effort
                }
            });
        }
    }

    /** Prevents zip-slip by ensuring extracted paths stay within {@code base}. */
    private Path safeResolve(Path base, String entryName) throws IOException {
        Path resolved = base.resolve(entryName).normalize();
        if (!resolved.startsWith(base)) {
            throw new IOException("Zip entry escapes target directory: " + entryName);
        }
        return resolved;
    }

    private static String slug(String name) {
        String s = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return s.isEmpty() ? "profile-" + System.currentTimeMillis() : s;
    }
}
