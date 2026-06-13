package com.spaceline.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-1 / SHA-256 helpers used to verify downloaded versions, libraries and
 * mod files against the hashes published by Mojang, Fabric, Modrinth and
 * CurseForge. Verifying integrity is mandatory before a downloaded artifact is
 * trusted on the classpath.
 */
public final class Checksums {

    private Checksums() {
    }

    public static String sha1(Path file) throws IOException {
        return digest(file, "SHA-1");
    }

    public static String sha256(Path file) throws IOException {
        return digest(file, "SHA-256");
    }

    public static boolean matches(Path file, String expectedHex, String algorithm) throws IOException {
        if (expectedHex == null || expectedHex.isBlank()) {
            return true; // No published hash to verify against.
        }
        return digest(file, algorithm).equalsIgnoreCase(expectedHex.trim());
    }

    private static String digest(Path file, String algorithm) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    md.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM is missing required digest: " + algorithm, e);
        }
    }
}
