package com.spaceline.common.module;

/**
 * A platform-agnostic key binding. The integer key code is interpreted by the
 * client layer (GLFW codes under Fabric); keeping it abstract here lets the
 * module framework and config system live in {@code :common} without a
 * Minecraft dependency.
 *
 * <p>{@link #UNBOUND} ({@code -1}) means no key is assigned.
 */
public final class Keybind {

    public static final int UNBOUND = -1;

    private int keyCode;

    public Keybind(int keyCode) {
        this.keyCode = keyCode;
    }

    public static Keybind unbound() {
        return new Keybind(UNBOUND);
    }

    public int keyCode() {
        return keyCode;
    }

    public void set(int keyCode) {
        this.keyCode = keyCode;
    }

    public boolean isBound() {
        return keyCode != UNBOUND;
    }

    public boolean matches(int pressedKey) {
        return isBound() && keyCode == pressedKey;
    }
}
