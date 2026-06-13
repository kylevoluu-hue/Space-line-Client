package com.spaceline.launcher.skin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.spaceline.common.config.ConfigManager;
import com.spaceline.common.config.Versioned;
import com.spaceline.common.net.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A persistent library of saved skins: each can be named, favourited, imported
 * from a file, downloaded from a URL (e.g. a public skin fetched by username),
 * and exported back out as a PNG.
 *
 * <p>Skin PNGs are stored as {@code <id>.png} in the skins directory and indexed
 * by a versioned {@code library.json} manifest.
 */
public final class SkinLibrary {

    private static final Logger LOG = LoggerFactory.getLogger(SkinLibrary.class);

    /** One saved skin. */
    public static final class Entry {
        public String id;
        public String name;
        public String sourceUsername = "";
        public String model = "classic"; // "classic" or "slim"
        public boolean favorite;
    }

    static final class Document implements Versioned {
        int currentVersion = 1;
        List<Entry> skins = new ArrayList<>();

        @Override
        public int currentVersion() {
            return currentVersion;
        }
    }

    private final Path skinsDir;
    private final ConfigManager<Document> config;
    private Document document;

    public SkinLibrary(Path skinsDir) {
        this.skinsDir = skinsDir;
        this.config = new ConfigManager<>(skinsDir.resolve("library.json"), Document.class, Document::new);
        this.document = config.load();
    }

    public List<Entry> list() {
        return new ArrayList<>(document.skins);
    }

    public Path pngPath(Entry entry) {
        return skinsDir.resolve(entry.id + ".png");
    }

    public Optional<Entry> get(String id) {
        return document.skins.stream().filter(e -> e.id.equals(id)).findFirst();
    }

    /** Imports a skin PNG from a local file. */
    public Entry importFromFile(String name, Path pngFile, String model) throws IOException {
        validatePng(pngFile);
        Entry entry = newEntry(name, model);
        Files.createDirectories(skinsDir);
        Files.copy(pngFile, pngPath(entry), StandardCopyOption.REPLACE_EXISTING);
        commit(entry);
        return entry;
    }

    /** Downloads a skin PNG from a URL (used for fetch-by-username). */
    public Entry importFromUrl(String name, String url, String sourceUsername, String model) throws IOException {
        Entry entry = newEntry(name, model);
        entry.sourceUsername = sourceUsername == null ? "" : sourceUsername;
        Http.download(url, pngPath(entry));
        commit(entry);
        LOG.info("Saved skin '{}' from {}", name, sourceUsername);
        return entry;
    }

    public void setFavorite(String id, boolean favorite) {
        get(id).ifPresent(e -> {
            e.favorite = favorite;
            save();
        });
    }

    public void rename(String id, String newName) {
        get(id).ifPresent(e -> {
            e.name = newName;
            save();
        });
    }

    public void delete(String id) {
        get(id).ifPresent(e -> {
            try {
                Files.deleteIfExists(pngPath(e));
            } catch (IOException ignored) {
                // best effort
            }
            document.skins.removeIf(x -> x.id.equals(id));
            save();
        });
    }

    /** Exports a saved skin's PNG to a chosen destination. */
    public void export(String id, Path destination) throws IOException {
        Entry entry = get(id).orElseThrow(() -> new IOException("No skin " + id));
        Files.copy(pngPath(entry), destination, StandardCopyOption.REPLACE_EXISTING);
    }

    private Entry newEntry(String name, String model) {
        Entry entry = new Entry();
        entry.id = UUID.randomUUID().toString().substring(0, 8);
        entry.name = (name == null || name.isBlank()) ? "Skin" : name.trim();
        entry.model = "slim".equalsIgnoreCase(model) ? "slim" : "classic";
        return entry;
    }

    private void commit(Entry entry) {
        document.skins.add(entry);
        save();
    }

    private void save() {
        config.save(document);
    }

    private static void validatePng(Path file) throws IOException {
        if (!file.getFileName().toString().toLowerCase().endsWith(".png")) {
            throw new IOException("Skins must be PNG files");
        }
    }
}
