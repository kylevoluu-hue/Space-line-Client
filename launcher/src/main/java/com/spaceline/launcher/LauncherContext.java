package com.spaceline.launcher;

import com.spaceline.common.ui.ThemeManager;
import com.spaceline.common.util.SpaceLinePaths;
import com.spaceline.launcher.account.AccountManager;
import com.spaceline.launcher.account.AccountStore;
import com.spaceline.launcher.account.MicrosoftAuthenticator;
import com.spaceline.launcher.browser.ModBrowser;
import com.spaceline.launcher.instance.InstanceManager;
import com.spaceline.launcher.java.JavaManager;
import com.spaceline.launcher.launch.FabricMetaService;
import com.spaceline.launcher.launch.GameLauncher;
import com.spaceline.launcher.launch.VersionInstaller;
import com.spaceline.launcher.process.ProcessController;
import com.spaceline.launcher.skin.SkinManager;
import com.spaceline.launcher.version.VersionRepository;
import com.spaceline.launcher.version.adapter.VersionAdapterRegistry;

/**
 * The launcher's composition root: constructs and wires together every
 * subsystem from a single {@link SpaceLinePaths} data root.
 *
 * <p>Keeping all wiring here (rather than scattering {@code new} calls through the
 * UI) makes the launcher testable and lets the GUI, a CLI or an automated test
 * drive the exact same configured services.
 */
public final class LauncherContext {

    private final SpaceLinePaths paths;
    private final AccountManager accountManager;
    private final JavaManager javaManager;
    private final VersionRepository versionRepository;
    private final VersionAdapterRegistry adapters;
    private final InstanceManager instanceManager;
    private final ProcessController processController;
    private final FabricMetaService fabricMeta;
    private final VersionInstaller versionInstaller;
    private final GameLauncher gameLauncher;
    private final ModBrowser modBrowser;
    private final SkinManager skinManager;
    private final ThemeManager themeManager;

    public LauncherContext() {
        this(new SpaceLinePaths());
    }

    public LauncherContext(SpaceLinePaths paths) {
        this.paths = paths;
        this.accountManager = new AccountManager(new AccountStore(paths.accounts()));
        this.javaManager = new JavaManager();
        this.versionRepository = new VersionRepository();
        this.adapters = VersionAdapterRegistry.withDefaults();
        this.instanceManager = new InstanceManager(paths);
        this.processController = new ProcessController();
        this.fabricMeta = new FabricMetaService();
        this.versionInstaller = new VersionInstaller(paths, fabricMeta);
        this.gameLauncher = new GameLauncher(paths, adapters, versionRepository, versionInstaller,
                javaManager, instanceManager, processController);
        this.modBrowser = ModBrowser.withDefaults();
        this.skinManager = new SkinManager(paths.skins(), paths.capes());
        this.themeManager = new ThemeManager(paths.themes());
    }

    /** Loads persisted state. Call once at startup. */
    public void initialize() {
        accountManager.load();
        themeManager.loadUserThemes();
    }

    public SpaceLinePaths paths() {
        return paths;
    }

    public AccountManager accounts() {
        return accountManager;
    }

    public JavaManager java() {
        return javaManager;
    }

    public VersionRepository versions() {
        return versionRepository;
    }

    public VersionAdapterRegistry adapters() {
        return adapters;
    }

    public InstanceManager instances() {
        return instanceManager;
    }

    public ProcessController processes() {
        return processController;
    }

    public FabricMetaService fabric() {
        return fabricMeta;
    }

    public GameLauncher launcher() {
        return gameLauncher;
    }

    public ModBrowser modBrowser() {
        return modBrowser;
    }

    public SkinManager skins() {
        return skinManager;
    }

    public ThemeManager themes() {
        return themeManager;
    }

    /**
     * Builds a Microsoft authenticator. The Azure public-client id is taken from
     * the {@code spaceline.msaClientId} system property or {@code SPACELINE_MSA_CLIENT_ID}
     * environment variable, defaulting to the well-known Minecraft device-code
     * client id so sign-in works out of the box for personal use. Register your
     * own Azure app for production deployments.
     */
    public MicrosoftAuthenticator microsoftAuthenticator() {
        String clientId = System.getProperty("spaceline.msaClientId",
                System.getenv().getOrDefault("SPACELINE_MSA_CLIENT_ID", "00000000402b5328"));
        return new MicrosoftAuthenticator(clientId);
    }
}
