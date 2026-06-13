package com.spaceline.launcher.version;

import java.util.Arrays;

/**
 * A lenient numeric version parser for Minecraft release ids such as
 * {@code "1.21"} or {@code "1.21.4"}. Non-numeric ids (snapshots like
 * {@code "24w14a"}) parse to {@link #INVALID}, which sorts below every real
 * release so they are naturally excluded from "supported" comparisons.
 */
public final class SemanticVersion implements Comparable<SemanticVersion> {

    public static final SemanticVersion INVALID = new SemanticVersion(new int[]{-1}, true);

    private final int[] parts;
    private final boolean invalid;

    private SemanticVersion(int[] parts, boolean invalid) {
        this.parts = parts;
        this.invalid = invalid;
    }

    public static SemanticVersion parse(String id) {
        if (id == null || id.isBlank()) {
            return INVALID;
        }
        String[] tokens = id.trim().split("\\.");
        int[] numbers = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            try {
                numbers[i] = Integer.parseInt(tokens[i]);
            } catch (NumberFormatException e) {
                return INVALID;
            }
        }
        return new SemanticVersion(numbers, false);
    }

    public boolean isValid() {
        return !invalid;
    }

    public int major() {
        return parts.length > 0 ? parts[0] : 0;
    }

    public int minor() {
        return parts.length > 1 ? parts[1] : 0;
    }

    public int patch() {
        return parts.length > 2 ? parts[2] : 0;
    }

    @Override
    public int compareTo(SemanticVersion other) {
        if (this.invalid || other.invalid) {
            return Boolean.compare(!this.invalid, !other.invalid) == 0
                    ? 0 : (this.invalid ? -1 : 1);
        }
        int max = Math.max(parts.length, other.parts.length);
        for (int i = 0; i < max; i++) {
            int a = i < parts.length ? parts[i] : 0;
            int b = i < other.parts.length ? other.parts[i] : 0;
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SemanticVersion sv && compareTo(sv) == 0;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new int[]{major(), minor(), patch()});
    }

    @Override
    public String toString() {
        if (invalid) {
            return "invalid";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(parts[i]);
        }
        return sb.toString();
    }
}
