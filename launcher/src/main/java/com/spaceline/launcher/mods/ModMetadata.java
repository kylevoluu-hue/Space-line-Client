package com.spaceline.launcher.mods;

import java.util.List;
import java.util.Map;

/**
 * The subset of a Fabric mod's {@code fabric.mod.json} that the launcher needs
 * for dependency checking, conflict detection and version validation.
 *
 * @param id           the mod id (unique key within an instance)
 * @param name         human-readable name
 * @param version      the mod's own version string
 * @param depends      hard dependencies: id -> version predicate (e.g. "*", ">=1.0")
 * @param breaks       mods this one is known to be incompatible with
 * @param provides     alternate ids this mod also satisfies
 * @param minecraftReq the Minecraft version predicate declared under depends.minecraft
 */
public record ModMetadata(
        String id,
        String name,
        String version,
        Map<String, String> depends,
        List<String> breaks,
        List<String> provides,
        String minecraftReq) {

    public static ModMetadata unknown(String fileName) {
        return new ModMetadata(fileName, fileName, "0.0.0",
                Map.of(), List.of(), List.of(), "*");
    }
}
