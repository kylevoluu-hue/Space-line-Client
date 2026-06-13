package com.spaceline.launcher.gui;

import java.awt.EventQueue;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.spaceline.launcher.LauncherContext;
import com.spaceline.launcher.SpaceLineLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graphical entry point for the Space~line launcher.
 *
 * <p>Boots the shared {@link LauncherContext} and shows the {@link LauncherFrame}
 * window. If command-line arguments are supplied (e.g. {@code status}), or the
 * environment is headless, it transparently delegates to the diagnostic CLI in
 * {@link SpaceLineLauncher} instead — so the same jar serves both the GUI and
 * scriptable use.
 */
public final class SpaceLineLauncherApp {

    private static final Logger LOG = LoggerFactory.getLogger(SpaceLineLauncherApp.class);

    public static void main(String[] args) {
        boolean headless = java.awt.GraphicsEnvironment.isHeadless()
                || "true".equalsIgnoreCase(System.getProperty("spaceline.headless"));
        if (args.length > 0 || headless) {
            SpaceLineLauncher.main(args);
            return;
        }

        LOG.info("Starting Space~line launcher GUI");
        FlatDarkLaf.setup();
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("ProgressBar.arc", 8);
        UIManager.put("TextComponent.arc", 8);

        LauncherContext context = new LauncherContext();
        context.initialize();

        EventQueue.invokeLater(() -> {
            LauncherFrame frame = new LauncherFrame(context);
            frame.setVisible(true);
        });
    }

    private SpaceLineLauncherApp() {
    }
}
