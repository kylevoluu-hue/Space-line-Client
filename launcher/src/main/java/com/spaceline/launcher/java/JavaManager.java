package com.spaceline.launcher.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.spaceline.common.util.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects the Java runtimes installed on the machine and selects an appropriate
 * one for a given Minecraft version family.
 *
 * <p>Discovery probes, in order: the running JVM, the {@code JAVA_HOME} and
 * {@code SPACELINE_JAVA_*} environment variables, and the platform's
 * conventional install directories. Each candidate is fingerprinted by running
 * {@code java -version} so the real major version and bitness are known rather
 * than guessed from a path. Only 64-bit runtimes in the supported 21–25 range
 * are offered for launching.
 */
public final class JavaManager {

    private static final Logger LOG = LoggerFactory.getLogger(JavaManager.class);
    private static final Pattern VERSION_LINE =
            Pattern.compile("version \"([0-9]+)(?:\\.([0-9]+))?(?:\\.([0-9]+))?[^\"]*\"");

    private List<JavaRuntime> detected;

    /** Returns all detected runtimes, scanning lazily on first call. */
    public List<JavaRuntime> detectAll() {
        if (detected == null) {
            detected = scan();
        }
        return detected;
    }

    public void rescan() {
        detected = null;
    }

    /** Only the runtimes Space~line will actually launch with (64-bit, 21–25). */
    public List<JavaRuntime> supported() {
        return detectAll().stream().filter(JavaRuntime::isSupported).toList();
    }

    /**
     * Picks the best supported runtime for a family requiring {@code requiredMajor}:
     * the lowest major version that still satisfies the requirement, preferring a
     * lower (i.e. more compatible) version over the newest available.
     */
    public Optional<JavaRuntime> selectFor(int requiredMajor) {
        return supported().stream()
                .filter(r -> r.satisfies(requiredMajor))
                .min(Comparator.comparingInt(JavaRuntime::majorVersion));
    }

    private List<JavaRuntime> scan() {
        Set<Path> executables = new LinkedHashSet<>();

        // 1) The JVM we are running in.
        addExecutable(executables, Path.of(System.getProperty("java.home")));

        // 2) Environment overrides.
        addEnvHome(executables, "JAVA_HOME");
        for (int major = 21; major <= 25; major++) {
            addEnvHome(executables, "SPACELINE_JAVA_" + major);
        }

        // 3) Conventional install roots per platform.
        for (Path root : conventionalRoots()) {
            collectFromRoot(executables, root);
        }

        List<JavaRuntime> runtimes = new ArrayList<>();
        for (Path executable : executables) {
            fingerprint(executable).ifPresent(runtimes::add);
        }
        runtimes.sort(Comparator.comparingInt(JavaRuntime::majorVersion).reversed());
        LOG.info("Detected {} Java runtime(s), {} supported",
                runtimes.size(), runtimes.stream().filter(JavaRuntime::isSupported).count());
        return runtimes;
    }

    private void addEnvHome(Set<Path> out, String envVar) {
        String value = System.getenv(envVar);
        if (value != null && !value.isBlank()) {
            addExecutable(out, Path.of(value));
        }
    }

    private void addExecutable(Set<Path> out, Path home) {
        Path bin = home.resolve("bin").resolve(binaryName());
        if (Files.isExecutable(bin)) {
            out.add(bin);
        }
    }

    private void collectFromRoot(Set<Path> out, Path root) {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (var stream = Files.list(root)) {
            stream.filter(Files::isDirectory).forEach(dir -> {
                addExecutable(out, dir);
                // macOS bundles nest the home under Contents/Home.
                addExecutable(out, dir.resolve("Contents").resolve("Home"));
            });
        } catch (IOException e) {
            LOG.debug("Could not scan Java root {}", root, e);
        }
    }

    private List<Path> conventionalRoots() {
        return switch (Platform.current()) {
            case WINDOWS -> windowsRoots();
            case MACOS -> List.of(
                    Path.of("/Library/Java/JavaVirtualMachines"),
                    Path.of(System.getProperty("user.home"), "Library/Java/JavaVirtualMachines"));
            default -> List.of(
                    Path.of("/usr/lib/jvm"),
                    Path.of("/usr/java"),
                    Path.of(System.getProperty("user.home"), ".sdkman/candidates/java"));
        };
    }

    /** Windows JDK locations, including the per-user installs many JDKs default to. */
    private List<Path> windowsRoots() {
        List<Path> roots = new ArrayList<>(List.of(
                Path.of("C:/Program Files/Java"),
                Path.of("C:/Program Files/Eclipse Adoptium"),
                Path.of("C:/Program Files/Microsoft"),
                Path.of("C:/Program Files/Zulu"),
                Path.of("C:/Program Files/Amazon Corretto"),
                Path.of("C:/Program Files/BellSoft")));

        // Vendors increasingly install per-user under %LOCALAPPDATA%\Programs.
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            Path programs = Path.of(localAppData, "Programs");
            roots.add(programs.resolve("Eclipse Adoptium"));
            roots.add(programs.resolve("Microsoft"));
            roots.add(programs.resolve("Zulu"));
        }
        // JREs bundled by other launchers are usable runtimes too.
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            roots.add(Path.of(appData, ".minecraft", "runtime"));
        }
        return roots;
    }

    private String binaryName() {
        return Platform.current().isWindows() ? "java.exe" : "java";
    }

    /** Runs {@code java -version} and parses the result into a {@link JavaRuntime}. */
    private Optional<JavaRuntime> fingerprint(Path executable) {
        try {
            Process process = new ProcessBuilder(executable.toString(), "-version")
                    .redirectErrorStream(true)
                    .start();
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().reduce("", (a, b) -> a + "\n" + b);
            }
            process.waitFor();

            Matcher matcher = VERSION_LINE.matcher(output);
            if (!matcher.find()) {
                return Optional.empty();
            }
            int major = parseMajor(matcher);
            String full = matcher.group(0).replaceAll(".*\"([^\"]+)\".*", "$1");
            boolean is64 = !output.contains("32-Bit");
            String vendor = output.toLowerCase().contains("openjdk") ? "OpenJDK" : "Oracle/Other";
            Path home = executable.getParent().getParent();
            return Optional.of(new JavaRuntime(home, executable, major, full, vendor, is64));
        } catch (IOException e) {
            LOG.debug("Failed to fingerprint Java at {}", executable, e);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private static int parseMajor(Matcher matcher) {
        int first = Integer.parseInt(matcher.group(1));
        // Legacy "1.8" style would report 1 here; map to the second component.
        if (first == 1 && matcher.group(2) != null) {
            return Integer.parseInt(matcher.group(2));
        }
        return first;
    }
}
