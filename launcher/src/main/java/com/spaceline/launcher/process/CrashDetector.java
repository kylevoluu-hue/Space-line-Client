package com.spaceline.launcher.process;

import java.util.regex.Pattern;

/**
 * Heuristically classifies why a game process ended, by combining its exit code
 * with telltale signatures scanned from the recent log output.
 *
 * <p>This drives crash detection and the auto-restart policy: a clean exit
 * (code 0, user quit) should never trigger a restart, whereas an OutOfMemory or
 * an explicit crash report should be surfaced to the error reporter.
 */
public final class CrashDetector {

    private static final Pattern OOM =
            Pattern.compile("OutOfMemoryError|GC overhead limit", Pattern.CASE_INSENSITIVE);
    private static final Pattern CRASH_REPORT =
            Pattern.compile("Minecraft Crash Report|---- Minecraft Crash", Pattern.CASE_INSENSITIVE);
    private static final Pattern MIXIN =
            Pattern.compile("Mixin apply failed|MixinApplyError|mixin\\.injection", Pattern.CASE_INSENSITIVE);

    public enum Outcome {
        CLEAN,
        USER_TERMINATED,
        OUT_OF_MEMORY,
        MOD_ERROR,
        CRASH,
        UNKNOWN_NONZERO
    }

    public record Result(Outcome outcome, int exitCode, String summary) {
        public boolean isCrash() {
            return outcome != Outcome.CLEAN && outcome != Outcome.USER_TERMINATED;
        }
    }

    /**
     * @param exitCode       the process exit code
     * @param userRequested  whether the launcher itself asked the process to stop
     * @param recentLog      a window of recent log text to scan for signatures
     */
    public Result classify(int exitCode, boolean userRequested, String recentLog) {
        if (userRequested) {
            return new Result(Outcome.USER_TERMINATED, exitCode, "Stopped by user");
        }
        if (exitCode == 0) {
            return new Result(Outcome.CLEAN, exitCode, "Exited normally");
        }
        String log = recentLog == null ? "" : recentLog;
        if (OOM.matcher(log).find()) {
            return new Result(Outcome.OUT_OF_MEMORY, exitCode,
                    "Ran out of memory — try increasing the instance heap size");
        }
        if (MIXIN.matcher(log).find()) {
            return new Result(Outcome.MOD_ERROR, exitCode,
                    "A mod failed to apply — check for incompatible or outdated mods");
        }
        if (CRASH_REPORT.matcher(log).find()) {
            return new Result(Outcome.CRASH, exitCode, "Minecraft produced a crash report");
        }
        return new Result(Outcome.UNKNOWN_NONZERO, exitCode, "Exited with code " + exitCode);
    }
}
