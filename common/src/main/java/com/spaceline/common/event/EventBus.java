package com.spaceline.common.event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A small, allocation-light, thread-safe event bus.
 *
 * <p>Listeners are discovered by reflection once (at registration time) and then
 * invoked through cached {@link MethodHandle}s, so dispatch stays fast on the hot
 * client tick/render paths. Listeners for a given event type are kept sorted by
 * {@link EventPriority}. A throwing listener is logged and isolated so it cannot
 * take down the rest of the chain — important when third-party modules subscribe.
 */
public final class EventBus {

    private static final Logger LOG = LoggerFactory.getLogger(EventBus.class);
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    /** Registered listeners keyed by the exact event type they listen for. */
    private final Map<Class<?>, CopyOnWriteArrayList<Listener>> listeners = new ConcurrentHashMap<>();

    /** Cache of resolved dispatch chains, including super-type listeners. */
    private final Map<Class<?>, List<Listener>> dispatchCache = new ConcurrentHashMap<>();

    /**
     * Scans {@code subscriber} for {@link Subscribe} annotated methods and
     * registers each as a listener. Safe to call from any thread.
     */
    public void register(Object subscriber) {
        for (Method method : subscriber.getClass().getMethods()) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation == null) {
                continue;
            }
            validate(method);
            Class<?> eventType = method.getParameterTypes()[0];
            MethodHandle handle = unreflect(method, subscriber);
            Listener listener = new Listener(subscriber, handle, annotation.priority(),
                    annotation.ignoreCancelled(), method.toString());
            listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
            sort(eventType);
        }
        dispatchCache.clear();
    }

    /** Removes every listener owned by {@code subscriber}. */
    public void unregister(Object subscriber) {
        for (CopyOnWriteArrayList<Listener> bucket : listeners.values()) {
            bucket.removeIf(l -> l.owner == subscriber);
        }
        dispatchCache.clear();
    }

    /**
     * Dispatches {@code event} to all matching listeners (including those that
     * subscribed to a super-type), honouring priority and cancellation.
     *
     * @return the same event instance, for fluent inspection of mutated state.
     */
    public <T extends Event> T post(T event) {
        List<Listener> chain = dispatchCache.computeIfAbsent(event.getClass(), this::resolveChain);
        if (chain.isEmpty()) {
            return event;
        }
        boolean cancellable = event instanceof Cancellable;
        for (Listener listener : chain) {
            if (cancellable && listener.ignoreCancelled && ((Cancellable) event).isCancelled()) {
                continue;
            }
            try {
                listener.handle.invoke(event);
            } catch (Throwable t) {
                LOG.error("Listener {} threw while handling {}", listener.description, event.name(), t);
            }
        }
        return event;
    }

    public boolean hasListeners(Class<? extends Event> eventType) {
        return !dispatchCache.computeIfAbsent(eventType, this::resolveChain).isEmpty();
    }

    private List<Listener> resolveChain(Class<?> eventType) {
        List<Listener> result = new ArrayList<>();
        for (Class<?> type = eventType; type != null && Event.class.isAssignableFrom(type); type = type.getSuperclass()) {
            CopyOnWriteArrayList<Listener> bucket = listeners.get(type);
            if (bucket != null) {
                result.addAll(bucket);
            }
        }
        result.sort(Comparator.comparingInt(l -> l.priority.order()));
        return result;
    }

    private void sort(Class<?> eventType) {
        CopyOnWriteArrayList<Listener> bucket = listeners.get(eventType);
        if (bucket != null) {
            List<Listener> sorted = new ArrayList<>(bucket);
            sorted.sort(Comparator.comparingInt(l -> l.priority.order()));
            bucket.clear();
            bucket.addAll(sorted);
        }
    }

    private static void validate(Method method) {
        if (method.getParameterCount() != 1) {
            throw new IllegalArgumentException("@Subscribe method must take exactly one parameter: " + method);
        }
        if (!Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
            throw new IllegalArgumentException("@Subscribe parameter must extend Event: " + method);
        }
    }

    private static MethodHandle unreflect(Method method, Object owner) {
        try {
            // Setting the accessible flag lets us bind listeners declared on
            // non-public classes (anonymous/inner subscribers are common) while
            // still going through fast MethodHandle invocation.
            method.setAccessible(true);
            return LOOKUP.unreflect(method).bindTo(owner);
        } catch (IllegalAccessException | InaccessibleObjectException e) {
            throw new IllegalStateException("Cannot access @Subscribe method: " + method, e);
        }
    }

    private static final class Listener {
        final Object owner;
        final MethodHandle handle;
        final EventPriority priority;
        final boolean ignoreCancelled;
        final String description;

        Listener(Object owner, MethodHandle handle, EventPriority priority,
                 boolean ignoreCancelled, String description) {
            this.owner = owner;
            this.handle = handle;
            this.priority = priority;
            this.ignoreCancelled = ignoreCancelled;
            this.description = description;
        }
    }
}
