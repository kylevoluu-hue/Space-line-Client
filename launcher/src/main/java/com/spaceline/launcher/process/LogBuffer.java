package com.spaceline.launcher.process;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * A bounded, thread-safe ring buffer of recent log lines backing the launcher's
 * live log viewer. Old lines are evicted once {@code capacity} is exceeded so a
 * long-running, chatty instance can never exhaust memory. Listeners receive each
 * new line as it arrives for live streaming into the UI.
 */
public final class LogBuffer {

    private final Deque<String> lines;
    private final int capacity;
    private final List<Consumer<String>> listeners = new ArrayList<>();

    public LogBuffer(int capacity) {
        this.capacity = capacity;
        this.lines = new ArrayDeque<>(capacity);
    }

    public synchronized void append(String line) {
        if (lines.size() >= capacity) {
            lines.removeFirst();
        }
        lines.addLast(line);
        // Copy listeners out of the lock to avoid holding it during callbacks.
        List<Consumer<String>> snapshot;
        synchronized (listeners) {
            snapshot = new ArrayList<>(listeners);
        }
        snapshot.forEach(l -> l.accept(line));
    }

    public synchronized List<String> snapshot() {
        return new ArrayList<>(lines);
    }

    public void addListener(Consumer<String> listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(Consumer<String> listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public synchronized void clear() {
        lines.clear();
    }
}
