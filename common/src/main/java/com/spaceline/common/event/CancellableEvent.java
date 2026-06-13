package com.spaceline.common.event;

/**
 * Convenience base class for the common case of a cancellable {@link Event}.
 */
public abstract class CancellableEvent extends Event implements Cancellable {

    private boolean cancelled;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
