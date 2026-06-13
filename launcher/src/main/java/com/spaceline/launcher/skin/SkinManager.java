package com.spaceline.launcher.skin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.spaceline.common.net.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages custom skins and capes and resolves public skins by username.
 *
 * <p>Two responsibilities:
 * <ul>
 *   <li><b>Custom uploads</b> — copies user-provided skin/cape PNGs into the
 *       per-account skin directory for preview and per-account assignment.</li>
 *   <li><b>Offline / username skin loading</b> — given a username, resolves the
 *       account's UUID via Mojang's public API and returns the public skin
 *       texture URL, so an offline profile can still display the real skin when
 *       one exists.</li>
 * </ul>
 */
public final class SkinManager {

    private static final Logger LOG = LoggerFactory.getLogger(SkinManager.class);
    private static final String UUID_API = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String PROFILE_API =
            "https://sessionserver.mojang.com/session/minecraft/profile/";

    private final Path skinsDir;
    private final Path capesDir;

    public SkinManager(Path skinsDir, Path capesDir) {
        this.skinsDir = skinsDir;
        this.capesDir = capesDir;
    }

    /** Imports a skin PNG, naming it after the account it belongs to. */
    public Path importSkin(String accountId, Path pngFile) throws IOException {
        return importImage(skinsDir, accountId, pngFile);
    }

    public Path importCape(String accountId, Path pngFile) throws IOException {
        return importImage(capesDir, accountId, pngFile);
    }

    public Optional<Path> assignedSkin(String accountId) {
        Path candidate = skinsDir.resolve(accountId + ".png");
        return Files.exists(candidate) ? Optional.of(candidate) : Optional.empty();
    }

    /**
     * Resolves the public skin texture URL for a username (offline skin loader),
     * or empty if the user has no account / no custom skin.
     */
    public Optional<String> publicSkinUrl(String username) {
        try {
            JsonObject idResponse = Http.getJson(UUID_API + username).getAsJsonObject();
            if (!idResponse.has("id")) {
                return Optional.empty();
            }
            String uuid = idResponse.get("id").getAsString();
            JsonObject profile = Http.getJson(PROFILE_API + uuid).getAsJsonObject();
            JsonArray properties = profile.getAsJsonArray("properties");
            for (var element : properties) {
                JsonObject property = element.getAsJsonObject();
                if ("textures".equals(property.get("name").getAsString())) {
                    String decoded = new String(java.util.Base64.getDecoder()
                            .decode(property.get("value").getAsString()));
                    JsonObject textures = com.spaceline.common.config.Json.parseObject(decoded)
                            .getAsJsonObject("textures");
                    if (textures.has("SKIN")) {
                        return Optional.of(textures.getAsJsonObject("SKIN").get("url").getAsString());
                    }
                }
            }
            return Optional.empty();
        } catch (IOException | RuntimeException e) {
            LOG.debug("Could not resolve public skin for {}", username, e);
            return Optional.empty();
        }
    }

    private static Path importImage(Path dir, String accountId, Path pngFile) throws IOException {
        if (!pngFile.getFileName().toString().toLowerCase().endsWith(".png")) {
            throw new IllegalArgumentException("Skins/capes must be PNG files");
        }
        Files.createDirectories(dir);
        Path target = dir.resolve(accountId + ".png");
        Files.copy(pngFile, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }
}
