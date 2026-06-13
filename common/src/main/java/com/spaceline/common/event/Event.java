package com.spaceline.common.event;

/**
 * Marker base type for everything dispatched through the {@link EventBus}.
 *
 * <p>Events are plain data carriers. Keep them immutable where possible; the
 * exception is {@link Cancellable} state and mutable "tweak" fields that
 * listeners are explicitly allowed to change (e.g. a render colour).
 */
public abstract class Event {

    /**
     * @return a stable, human-readable name used in logs and debugging tools.
     *         Defaults to the simple class name.
     */
    public String name() {
        return getClass().getSimpleName();
    }
}
