package com.spaceline.launcher.gui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;

/**
 * The Credits page: attribution for the client's author and a per-OS guide to
 * installing / sharing the launcher.
 */
public final class CreditsDialog extends JDialog {

    private static final String REPO = "https://github.com/kylevoluu-hue/space-line-client";

    public CreditsDialog(Frame owner) {
        super(owner, "Credits", true);
        setMinimumSize(new Dimension(560, 520));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JEditorPane pane = new JEditorPane("text/html", html());
        pane.setEditable(false);
        pane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                browse(e.getURL() != null ? e.getURL().toString() : REPO);
            }
        });
        add(new JScrollPane(pane), BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton repo = new JButton("Open GitHub");
        repo.addActionListener(e -> browse(REPO));
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        buttons.add(repo);
        buttons.add(close);
        add(buttons, BorderLayout.SOUTH);
    }

    private static String html() {
        return """
                <html><body style='font-family:sans-serif; margin:10px;'>
                <h1 style='margin-bottom:0;'>Spaceline Client</h1>
                <p style='color:#888; margin-top:2px;'>Thank you for using Spaceline client.</p>

                <h3>Created by</h3>
                <p><b>kyluua</b> (@kyluua) — creator &amp; maintainer of Spaceline Client.</p>

                <h3>Built with</h3>
                <ul>
                  <li>Fabric — mod loader</li>
                  <li>Modrinth &amp; CurseForge — mod / pack / shader sources</li>
                  <li>Mojang — Minecraft version &amp; asset metadata</li>
                  <li>FlatLaf — launcher look-and-feel</li>
                </ul>

                <h3>Install &amp; share (all platforms)</h3>
                <p>The launcher runs on <b>Windows, macOS and Linux</b> (Java 21–25).</p>
                <ul>
                  <li><b>Windows:</b> build a shareable installer with
                      <code>gradlew.bat :launcher:packageInstaller</code> →
                      <code>Spaceline-1.0.0.exe</code>.</li>
                  <li><b>macOS / Linux:</b> build a runnable bundle with
                      <code>./gradlew :launcher:installDist</code> and run
                      <code>launcher/build/install/launcher/bin/launcher</code>,
                      or package natively with <code>jpackage</code>.</li>
                  <li><b>Any OS:</b> <code>./gradlew :launcher:run</code> launches it directly.</li>
                </ul>

                <h3>License</h3>
                <p>MIT — open source &amp; fork-able.
                   <a href='%s'>github.com/kylevoluu-hue/space-line-client</a></p>
                </body></html>
                """.formatted(REPO);
    }

    private static void browse(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ignored) {
            // No browser available; ignore.
        }
    }
}
