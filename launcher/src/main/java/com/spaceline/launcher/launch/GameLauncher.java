package com.spaceline.launcher.launch;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.spaceline.common.util.SpaceLinePaths;
import com.spaceline.launcher.account.Account;
import com.spaceline.launcher.instance.Instance;
import com.spaceline.launcher.instance.InstanceManager;
import com.spaceline.launcher.instance.ModLoader;
import com.spaceline.launcher.java.JavaManager;
import com.spaceline.launcher.java.JavaRuntime;
import com.spaceline.launcher.mods.ModManager;
import com.spaceline.launcher.mods.ModValidation;
import com.spaceline.launcher.process.GameProcess;
import com.spaceline.launcher.process.ProcessController;
import com.spaceline.launcher.version.MinecraftVersion;
import com.spaceline.launcher.version.VersionRepository;
import com.spaceline.launcher.version.adapter.VersionAdapter;
import com.spaceline.launcher.version.adapter.VersionAdapterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrates a full launch end-to-end and is the implementation behind the
 * launcher's "Play" button.
 *
 * <p>The pipeline: pick the {@link VersionAdapter} → verify the loader is
 * supported and a suitable Java runtime exists → install/verify the version's
 * files → validate the instance's mods → build the command via
 * {@link LaunchCommandBuilder} → hand it to the {@link ProcessController}. Each
 * step fails fast with an actionable message rather than producing a half-started
 * game.
 */
public final class GameLauncher {

    private static final Logger LOG = LoggerFactory.getLogger(GameLauncher.class);

    private final SpaceLinePaths paths;
    private final VersionAdapterRegistry adapters;
    private final VersionRepository versions;
    private final VersionInstaller installer;
    private final JavaManager javaManager;
    private final InstanceManager instanceManager;
    private final ProcessController processController;

    public GameLauncher(SpaceLinePaths paths, VersionAdapterRegistry adapters,
                        VersionRepository versions, VersionInstaller installer,
                        JavaManager javaManager, InstanceManager instanceManager,
                        ProcessController processController) {
        this.paths = paths;
        this.adapters = adapters;
        this.versions = versions;
        this.installer = installer;
        this.javaManager = javaManager;
        this.instanceManager = instanceManager;
        this.processController = processController;
    }

    /** The result of pre-launch mod validation, surfaced to the UI as warnings. */
    public record PreflightWarnings(ModValidation modValidation) {
        public boolean hasWarnings() {
            return modValidation != null && !modValidation.isClean();
        }
    }

    /**
     * Launches {@code instance} with {@code account}.
     *
     * @throws LaunchException with a user-facing message if any precondition fails
     */
    public GameProcess launch(Instance instance, Account account) throws LaunchException {
        try {
            MinecraftVersion version = resolveVersion(instance.minecraftVersion());
            VersionAdapter adapter = adapters.require(version);
            adapter.verifyLoaderSupported(instance.loader().id());

            JavaRuntime java = selectJava(instance, adapter);
            LOG.info("Launching '{}' ({} {}) with {}", instance.id(), version.id(),
                    instance.loader().id(), java.describe());

            GameInstallation installation = installer.install(
                    version.id(), version.manifestUrl(), instance.loader(),
                    instance.fabricLoaderVersion());

            validateMods(instance);

            List<String> command = new LaunchCommandBuilder(adapter)
                    .build(installation, account, instance, java, instanceManager.gameDir(instance.id()));

            Path gameDir = instanceManager.gameDir(instance.id());
            return processController.start(instance.id(), command, gameDir,
                    Map.of(), instance.autoRestartOnCrash());
        } catch (LaunchException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new LaunchException("Failed to launch instance '" + instance.id() + "': "
                    + e.getMessage(), e);
        }
    }

    private MinecraftVersion resolveVersion(String id) throws IOException, LaunchException {
        for (MinecraftVersion version : versions.supportedVersions()) {
            if (version.id().equals(id)) {
                return version;
            }
        }
        throw new LaunchException("Minecraft version '" + id
                + "' is not an installable stable release (>= " + MinecraftVersion.MINIMUM_SUPPORTED + ")");
    }

    private JavaRuntime selectJava(Instance instance, VersionAdapter adapter) throws LaunchException {
        if (instance.javaPathOverride() != null && !instance.javaPathOverride().isBlank()) {
            return javaManager.detectAll().stream()
                    .filter(r -> r.executable().toString().equals(instance.javaPathOverride()))
                    .findFirst()
                    .orElseThrow(() -> new LaunchException(
                            "Configured Java runtime not found: " + instance.javaPathOverride()));
        }
        return javaManager.selectFor(adapter.requiredJavaMajor())
                .orElseThrow(() -> new LaunchException(
                        "No installed Java " + adapter.requiredJavaMajor()
                                + "+ runtime found (Space~line supports Java 21–25). "
                                + "Install a JDK or set SPACELINE_JAVA_" + adapter.requiredJavaMajor() + "."));
    }

    private void validateMods(Instance instance) {
        if (instance.loader() != ModLoader.FABRIC) {
            return;
        }
        ModManager mods = new ModManager(instanceManager.modsDir(instance.id()));
        ModValidation validation = mods.validate(instance.minecraftVersion());
        if (!validation.isClean()) {
            validation.missingDependencies().forEach(d -> LOG.warn("Mod issue: {}", d));
            validation.conflicts().forEach(c -> LOG.warn("Mod issue: {}", c));
            validation.versionMismatches().forEach(m -> LOG.warn("Mod issue: {}", m));
        }
    }

    /** Thrown when a launch cannot proceed, carrying a user-facing message. */
    public static final class LaunchException extends Exception {
        public LaunchException(String message) {
            super(message);
        }

        public LaunchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
