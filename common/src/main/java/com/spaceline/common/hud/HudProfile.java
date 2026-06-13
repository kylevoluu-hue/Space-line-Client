package com.spaceline.common.hud;

import java.util.LinkedHashMap;
import java.util.Map;

import com.spaceline.common.config.Versioned;

/**
 * A named, saveable collection of {@link HudElement} layouts plus grid settings.
 *
 * <p>Users can keep multiple profiles (e.g. "PvP", "Streaming", "Minimal") and
 * switch between them. Profiles are versioned so the layout schema can evolve
 * with migrations like any other config.
 */
public final class HudProfile implements Versioned {

    public static final int VERSION = 1;

    private String name = "default";
    private boolean snapToGrid = true;
    private int gridSize = 4;
    private final Map<String, HudElement> elements = new LinkedHashMap<>();

    public HudProfile() {
    }

    public HudProfile(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean snapToGrid() {
        return snapToGrid;
    }

    public void setSnapToGrid(boolean snapToGrid) {
        this.snapToGrid = snapToGrid;
    }

    public int gridSize() {
        return gridSize;
    }

    public void setGridSize(int gridSize) {
        this.gridSize = Math.max(1, gridSize);
    }

    /** Returns the element for {@code id}, creating a default one if absent. */
    public HudElement element(String id) {
        return elements.computeIfAbsent(id, HudElement::new);
    }

    public Map<String, HudElement> elements() {
        return elements;
    }

    /** Snaps a coordinate to the grid when snapping is enabled. */
    public double snap(double value) {
        if (!snapToGrid || gridSize <= 1) {
            return value;
        }
        return Math.round(value / gridSize) * (double) gridSize;
    }

    @Override
    public int currentVersion() {
        return VERSION;
    }
}
