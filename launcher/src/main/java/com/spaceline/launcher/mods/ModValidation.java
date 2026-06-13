package com.spaceline.launcher.mods;

import java.util.List;

/**
 * The result of validating a set of mods against each other and the target
 * Minecraft version: missing dependencies, declared conflicts and version
 * mismatches. The launcher surfaces these before launch so the user can fix
 * their setup rather than hitting an opaque crash.
 */
public record ModValidation(
        List<MissingDependency> missingDependencies,
        List<Conflict> conflicts,
        List<VersionMismatch> versionMismatches) {

    public boolean isClean() {
        return missingDependencies.isEmpty() && conflicts.isEmpty() && versionMismatches.isEmpty();
    }

    /** A mod requires another mod that is absent or disabled. */
    public record MissingDependency(String modId, String requiredId, String requiredVersion) {
        @Override
        public String toString() {
            return modId + " requires '" + requiredId + "' " + requiredVersion + " which is not present";
        }
    }

    /** Two enabled mods declare incompatibility with each other. */
    public record Conflict(String modId, String conflictsWithId) {
        @Override
        public String toString() {
            return modId + " is incompatible with " + conflictsWithId;
        }
    }

    /** A mod does not declare support for the instance's Minecraft version. */
    public record VersionMismatch(String modId, String required, String actual) {
        @Override
        public String toString() {
            return modId + " targets Minecraft " + required + " but the instance is " + actual;
        }
    }
}
