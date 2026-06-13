package com.spaceline.launcher.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JWindow;
import javax.swing.Timer;

/**
 * The animated opening screen shown while the launcher boots.
 *
 * <p>Over an interactive particle field, the black-hole logo fades and zooms in,
 * the <b>Spaceline</b> title appears, and the subheading
 * "Thank you for using Spaceline client. - kyluua" fades in beneath it. After a
 * short hold the whole splash fades out and the callback opens the main window.
 */
public final class SplashScreen extends JWindow {

    private static final int FADE_IN_MS = 700;
    private static final int HOLD_MS = 1500;
    private static final int FADE_OUT_MS = 500;
    private static final int TOTAL_MS = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;

    private final SplashContent content;
    private final long startTime = System.currentTimeMillis();
    private final Runnable onComplete;
    private boolean completed;

    public SplashScreen(BufferedImage logo, Runnable onComplete) {
        this.onComplete = onComplete;
        this.content = new SplashContent(logo);
        setContentPane(content);
        setSize(new Dimension(560, 360));
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0)); // allow rounded/transparent edges
    }

    public void showSplash() {
        content.particles.start();
        setVisible(true);
        Timer timer = new Timer(25, e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            content.setProgress(elapsed);
            content.repaint();
            if (elapsed >= TOTAL_MS && !completed) {
                completed = true;
                ((Timer) e.getSource()).stop();
                content.particles.stop();
                dispose();
                onComplete.run();
            }
        });
        timer.start();
    }

    /** Particle background + animated logo and text. */
    private static final class SplashContent extends javax.swing.JPanel {
        private final ParticlePanel particles = new ParticlePanel(70, new Color(0x6F, 0xB8, 0xFF));
        private final BufferedImage logo;
        private float overallAlpha;
        private float logoScale = 0.82f;
        private float textAlpha;

        SplashContent(BufferedImage logo) {
            this.logo = logo;
            setLayout(null);
            particles.setBounds(0, 0, 560, 360);
            particles.setBackgroundColors(new Color(0x0B, 0x0E, 0x1A), new Color(0x04, 0x05, 0x0C));
            add(particles);
        }

        void setProgress(long elapsed) {
            if (elapsed < FADE_IN_MS) {
                float t = elapsed / (float) FADE_IN_MS;
                overallAlpha = t;
                logoScale = 0.82f + 0.18f * ease(t);
                textAlpha = Math.max(0, (t - 0.4f) / 0.6f);
            } else if (elapsed < FADE_IN_MS + HOLD_MS) {
                overallAlpha = 1f;
                logoScale = 1f;
                textAlpha = 1f;
            } else {
                float t = (elapsed - FADE_IN_MS - HOLD_MS) / (float) FADE_OUT_MS;
                overallAlpha = Math.max(0, 1 - t);
                textAlpha = overallAlpha;
            }
        }

        private static float ease(float t) {
            return 1 - (1 - t) * (1 - t);
        }

        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp(overallAlpha)));

            int cx = getWidth() / 2;

            // Logo.
            if (logo != null) {
                int base = 140;
                int size = Math.round(base * logoScale);
                Image scaled = logo.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                g2.drawImage(scaled, cx - size / 2, 60, null);
            }

            // Title.
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Serif", Font.BOLD, 44));
            drawCentered(g2, "Spaceline", cx, 250);

            // Subheading.
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp(textAlpha)));
            g2.setColor(new Color(0xC8, 0xD2, 0xE6));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            drawCentered(g2, "Thank you for using Spaceline client. - kyluua", cx, 285);
            g2.dispose();
        }

        private void drawCentered(Graphics2D g, String text, int cx, int y) {
            int w = g.getFontMetrics().stringWidth(text);
            g.drawString(text, cx - w / 2, y);
        }

        private static float clamp(float v) {
            return v < 0 ? 0 : v > 1 ? 1 : v;
        }
    }
}
