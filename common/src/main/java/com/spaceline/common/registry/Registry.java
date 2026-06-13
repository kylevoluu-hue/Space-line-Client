package com.spaceline.common.registry;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A simple, insertion-ordered registry of {@link Identifiable} values.
 *
 * <p>Used as the backbone of the plugin/module architecture: the client module
 * manager, HUD element registry and version-adapter table are all instances of
 * this type. Registration is fail-fast on duplicate ids so wiring mistakes are
 * caught at startup rather than producing silent overrides.
 *
 * @param <T> the registered type
 */
public class Registry<T extends Identifiable> implements Iterable<T> {

    private final String name;
    private final Map<String, T> entries = new LinkedHashMap<>();

    public Registry(String name) {
        this.name = name;
    }

    /** Registers {@code value}, returning it for fluent chaining. */
    public synchronized T register(T value) {
        String id = value.id();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Cannot register a value with a blank id into '" + name + "'");
        }
        if (entries.containsKey(id)) {
            throw new IllegalStateException("Duplicate id '" + id + "' in registry '" + name + "'");
        }
        entries.put(id, value);
        return value;
    }

    public synchronized Optional<T> get(String id) {
        return Optional.ofNullable(entries.get(id));
    }

    public T require(String id) {
        return get(id).orElseThrow(() ->
                new IllegalArgumentException("No entry '" + id + "' registered in '" + name + "'"));
    }

    public synchronized boolean contains(String id) {
        return entries.containsKey(id);
    }

    public synchronized List<T> all() {
        return List.copyOf(entries.values());
    }

    public synchronized int size() {
        return entries.size();
    }

    public String name() {
        return name;
    }

    @Override
    public Iterator<T> iterator() {
        return all().iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        all().forEach(action);
    }
}
