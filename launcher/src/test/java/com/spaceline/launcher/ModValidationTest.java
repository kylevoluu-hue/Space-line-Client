package com.spaceline.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.spaceline.launcher.mods.DependencyChecker;
import com.spaceline.launcher.mods.ModFile;
import com.spaceline.launcher.mods.ModMetadata;
import com.spaceline.launcher.mods.ModValidation;
import com.spaceline.launcher.mods.VersionPredicate;
import org.junit.jupiter.api.Test;

class ModValidationTest {

    private ModFile mod(String id, Map<String, String> depends, List<String> breaks, String mcReq) {
        ModMetadata meta = new ModMetadata(id, id, "1.0.0", depends, breaks, List.of(), mcReq);
        return new ModFile(Path.of(id + ".jar"), meta);
    }

    @Test
    void detectsMissingDependency() {
        ModValidation result = new DependencyChecker().validate(
                List.of(mod("a", Map.of("fabric-api", "*"), List.of(), "*")), "1.21.4");
        assertEquals(1, result.missingDependencies().size());
        assertFalse(result.isClean());
    }

    @Test
    void satisfiedDependencyIsClean() {
        ModValidation result = new DependencyChecker().validate(List.of(
                mod("a", Map.of("fabric-api", "*"), List.of(), "*"),
                mod("fabric-api", Map.of(), List.of(), "*")), "1.21.4");
        assertTrue(result.isClean());
    }

    @Test
    void detectsConflict() {
        ModValidation result = new DependencyChecker().validate(List.of(
                mod("a", Map.of(), List.of("b"), "*"),
                mod("b", Map.of(), List.of(), "*")), "1.21.4");
        assertEquals(1, result.conflicts().size());
    }

    @Test
    void detectsVersionMismatch() {
        ModValidation result = new DependencyChecker().validate(
                List.of(mod("a", Map.of(), List.of(), ">=1.22")), "1.21.4");
        assertEquals(1, result.versionMismatches().size());
    }

    @Test
    void versionPredicateEvaluates() {
        assertTrue(VersionPredicate.matches(">=1.21", "1.21.4"));
        assertFalse(VersionPredicate.matches(">=1.22", "1.21.4"));
        assertTrue(VersionPredicate.matches("1.21.x", "1.21.9"));
        assertFalse(VersionPredicate.matches("1.21.x", "1.22.0"));
        assertTrue(VersionPredicate.matches("*", "1.21.4"));
    }
}
