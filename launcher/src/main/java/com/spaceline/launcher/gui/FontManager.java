package com.spaceline.launcher.gui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lists the fonts available to the UI — common families (Georgia, Arial, …) that
 * exist on the system, plus user-imported {@code .ttf}/{@code .otf} fonts from
 * the fonts directory — and registers imported fonts so Swing can use them by
 * name.
 */
public final class FontManager {

    private static final Logger LOG = LoggerFactory.getLogger(FontManager.class);
    private static final List<String> PREFERRED = List.of(
            "SansSerif", "Serif", "Monospaced", "Georgia", "Arial", "Verdana",
            "Tahoma", "Times New Roman", "Courier New", "Comic Sans MS", "Segoe UI");

    private final Path fontsDir;

    public FontManager(Path fontsDir) {
        this.fontsDir = fontsDir;
        loadImported();
    }

    /** Available font family names: preferred + every system family. */
    public List<String> available() {
        Set<String> families = new LinkedHashSet<>();
        Set<String> system = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames());
        for (String name : PREFERRED) {
            if (system.contains(name) || name.equals("SansSerif")
                    || name.equals("Serif") || name.equals("Monospaced")) {
                families.add(name);
            }
        }
        families.addAll(system);
        return new ArrayList<>(families);
    }

    /** Imports a font file, registering it so it can be selected by family name. */
    public String importFont(Path fontFile) throws IOException {
        Files.createDirectories(fontsDir);
        Path target = fontsDir.resolve(fontFile.getFileName());
        Files.copy(fontFile, target, StandardCopyOption.REPLACE_EXISTING);
        return register(target);
    }

    private void loadImported() {
        if (!Files.isDirectory(fontsDir)) {
            return;
        }
        try (var stream = Files.list(fontsDir)) {
            stream.filter(FontManager::isFontFile).forEach(p -> {
                try {
                    register(p);
                } catch (IOException e) {
                    LOG.warn("Could not load font {}", p.getFileName(), e);
                }
            });
        } catch (IOException e) {
            LOG.warn("Could not scan fonts dir", e);
        }
    }

    private String register(Path fontFile) throws IOException {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile.toFile());
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            LOG.info("Registered font '{}'", font.getFamily());
            return font.getFamily();
        } catch (java.awt.FontFormatException e) {
            throw new IOException("Not a valid font file: " + fontFile.getFileName(), e);
        }
    }

    private static boolean isFontFile(Path p) {
        String n = p.getFileName().toString().toLowerCase();
        return n.endsWith(".ttf") || n.endsWith(".otf");
    }
}
