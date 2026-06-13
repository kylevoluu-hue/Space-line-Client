package com.spaceline.client.engine;

import java.util.List;

/**
 * A read-only snapshot/accessor of live game state that HUD and utility modules
 * consume.
 *
 * <p>This is the second half of the rendering abstraction: where
 * {@link com.spaceline.client.render.RenderContext} lets modules <em>draw</em>
 * without Minecraft, {@code GameState} lets them <em>read</em> game data without
 * Minecraft. The Fabric layer supplies a live implementation; tests use a fake.
 * Every method has a sensible default so a module only overrides interest, and
 * {@link #EMPTY} provides safe values when not in a world.
 */
public interface GameState {

    GameState EMPTY = new GameState() { };

    default boolean inGame() {
        return false;
    }

    default String username() {
        return "Player";
    }

    default int fps() {
        return 0;
    }

    default double x() {
        return 0;
    }

    default double y() {
        return 0;
    }

    default double z() {
        return 0;
    }

    default float yaw() {
        return 0;
    }

    default float pitch() {
        return 0;
    }

    /** Cardinal facing such as "North (-Z)". */
    default String facing() {
        return "North";
    }

    default String biome() {
        return "Plains";
    }

    default String dimension() {
        return "Overworld";
    }

    /** Latency to the current server in milliseconds, or -1 if singleplayer. */
    default long pingMillis() {
        return -1;
    }

    /** Server-side ticks per second estimate, or 20 when unknown. */
    default float tps() {
        return 20f;
    }

    default String serverAddress() {
        return "singleplayer";
    }

    /** In-game time of day, 0–24000. */
    default long worldTime() {
        return 0;
    }

    default float health() {
        return 20f;
    }

    default float maxHealth() {
        return 20f;
    }

    default int food() {
        return 20;
    }

    default float saturation() {
        return 5f;
    }

    /** Active potion effects as display strings (e.g. "Speed II 1:23"). */
    default List<String> potionEffects() {
        return List.of();
    }

    /** Held/used JVM and system memory for the System Resources HUD. */
    default long usedMemoryBytes() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    default long maxMemoryBytes() {
        return Runtime.getRuntime().maxMemory();
    }
}
