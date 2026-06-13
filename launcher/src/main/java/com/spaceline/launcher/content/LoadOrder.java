package com.spaceline.launcher.content;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A persisted, ordered list of enabled item names (resource packs or shaders),
 * stored one-per-line. Encapsulates the add/remove/reorder logic shared by the
 * content managers.
 */
final class LoadOrder {

    private final Path file;

    LoadOrder(Path file) {
        this.file = file;
    }

    synchronized List<String> load() {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try {
            List<String> lines = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    lines.add(line.trim());
                }
            }
            return lines;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    synchronized void add(String name) {
        List<String> order = load();
        if (!order.contains(name)) {
            order.add(name);
            persist(order);
        }
    }

    synchronized void remove(String name) {
        List<String> order = load();
        if (order.remove(name)) {
            persist(order);
        }
    }

    synchronized void move(String name, int delta) {
        List<String> order = load();
        int index = order.indexOf(name);
        if (index < 0) {
            return;
        }
        int target = Math.max(0, Math.min(order.size() - 1, index + delta));
        order.remove(index);
        order.add(target, name);
        persist(order);
    }

    private void persist(List<String> order) {
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, order, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
