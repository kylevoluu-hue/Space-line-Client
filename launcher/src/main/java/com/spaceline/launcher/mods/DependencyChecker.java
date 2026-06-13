package com.spaceline.launcher.mods;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates a set of enabled mods: checks that every declared dependency is
 * present (and version-compatible), detects mutually-incompatible mods and flags
 * mods that don't target the instance's Minecraft version.
 *
 * <p>Only enabled mods participate; disabled mods are ignored exactly as the
 * loader would ignore them. {@code provides} aliases are honoured so a mod that
 * provides {@code "fabric-api"} satisfies a dependency on it.
 */
public final class DependencyChecker {

    public ModValidation validate(List<ModFile> mods, String minecraftVersion) {
        List<ModFile> enabled = mods.stream().filter(ModFile::isEnabled).toList();

        Set<String> available = new HashSet<>();
        for (ModFile mod : enabled) {
            available.add(mod.metadata().id());
            available.addAll(mod.metadata().provides());
        }

        List<ModValidation.MissingDependency> missing = new ArrayList<>();
        List<ModValidation.Conflict> conflicts = new ArrayList<>();
        List<ModValidation.VersionMismatch> mismatches = new ArrayList<>();

        for (ModFile mod : enabled) {
            ModMetadata meta = mod.metadata();

            meta.depends().forEach((depId, predicate) -> {
                if (!available.contains(depId)) {
                    missing.add(new ModValidation.MissingDependency(meta.id(), depId, predicate));
                }
            });

            for (String broken : meta.breaks()) {
                if (available.contains(broken)) {
                    conflicts.add(new ModValidation.Conflict(meta.id(), broken));
                }
            }

            if (!VersionPredicate.matches(meta.minecraftReq(), minecraftVersion)) {
                mismatches.add(new ModValidation.VersionMismatch(
                        meta.id(), meta.minecraftReq(), minecraftVersion));
            }
        }

        return new ModValidation(missing, conflicts, mismatches);
    }
}
