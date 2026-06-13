package com.spaceline.launcher.mods;

import com.spaceline.launcher.version.SemanticVersion;

/**
 * A small evaluator for the version predicates found in {@code fabric.mod.json}
 * dependency blocks ({@code "*"}, {@code ">=1.21"}, {@code "<1.22"},
 * {@code "1.21.x"}, …). It is intentionally lenient — its job is to power
 * compatibility <em>warnings</em>, not to be a spec-perfect semver resolver.
 */
public final class VersionPredicate {

    private VersionPredicate() {
    }

    public static boolean matches(String predicate, String actualVersion) {
        if (predicate == null || predicate.isBlank() || predicate.equals("*")) {
            return true;
        }
        SemanticVersion actual = SemanticVersion.parse(actualVersion);
        if (!actual.isValid()) {
            return true; // can't reason about it; don't raise a false alarm
        }
        for (String clause : predicate.split("\\s+")) {
            if (!matchesClause(clause.trim(), actual)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesClause(String clause, SemanticVersion actual) {
        if (clause.isEmpty() || clause.equals("*")) {
            return true;
        }
        if (clause.endsWith(".x")) {
            SemanticVersion base = SemanticVersion.parse(clause.substring(0, clause.length() - 2));
            return base.isValid() && actual.major() == base.major() && actual.minor() == base.minor();
        }
        String operator = extractOperator(clause);
        SemanticVersion target = SemanticVersion.parse(clause.substring(operator.length()));
        if (!target.isValid()) {
            return true;
        }
        int cmp = actual.compareTo(target);
        return switch (operator) {
            case ">=" -> cmp >= 0;
            case "<=" -> cmp <= 0;
            case ">" -> cmp > 0;
            case "<" -> cmp < 0;
            case "~", "^" -> actual.major() == target.major() && cmp >= 0;
            default -> cmp == 0;
        };
    }

    private static String extractOperator(String clause) {
        for (String op : new String[]{">=", "<=", ">", "<", "~", "^", "="}) {
            if (clause.startsWith(op)) {
                return op.equals("=") ? "" : op;
            }
        }
        return "";
    }
}
