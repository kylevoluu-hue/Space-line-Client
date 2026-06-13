package com.spaceline.launcher.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * An animated, interactive "constellation" particle background.
 *
 * <p>Particles drift slowly and are linked with faint lines when close, forming a
 * subtle space-y network. Moving the mouse gently repels nearby particles, so the
 * field feels alive — and because it's a background component painted behind the
 * UI, it never intercepts clicks meant for buttons. Colour, density and the
 * effect itself are configurable and can be toggled off.
 */
public final class ParticlePanel extends JPanel {

    private static final Random RNG = new Random();

    private final int count;
    private Color particleColor;
    private boolean drawBackground = true;
    private boolean particlesActive = true;
    private java.awt.image.BufferedImage backgroundImage;
    private Color backgroundTop = new Color(0x0E, 0x12, 0x1E);
    private Color backgroundBottom = new Color(0x05, 0x07, 0x0F);

    private float[] px;
    private float[] py;
    private float[] vx;
    private float[] vy;
    private int mouseX = -1000;
    private int mouseY = -1000;
    private final Timer timer;

    public ParticlePanel(int count, Color particleColor) {
        this.count = count;
        this.particleColor = particleColor;
        setOpaque(true);
        initParticles();

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouseX = -1000;
                mouseY = -1000;
            }
        };
        addMouseMotionListener(mouse);
        addMouseListener(mouse);

        // ~33 FPS; cheap enough to leave running.
        this.timer = new Timer(30, e -> {
            step();
            repaint();
        });
    }

    public void setParticleColor(Color color) {
        this.particleColor = color;
    }

    public void setDrawBackground(boolean drawBackground) {
        this.drawBackground = drawBackground;
    }

    public void setBackgroundColors(Color top, Color bottom) {
        this.backgroundTop = top;
        this.backgroundBottom = bottom;
    }

    public void setBackgroundImage(java.awt.image.BufferedImage image) {
        this.backgroundImage = image;
        repaint();
    }

    /** Toggles the particle/line overlay while keeping the background. */
    public void setParticlesActive(boolean active) {
        this.particlesActive = active;
        if (active) {
            start();
        } else {
            stop();
        }
        repaint();
    }

    public void start() {
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    public void stop() {
        timer.stop();
    }

    private void initParticles() {
        px = new float[count];
        py = new float[count];
        vx = new float[count];
        vy = new float[count];
        for (int i = 0; i < count; i++) {
            px[i] = RNG.nextFloat();   // stored as 0..1, scaled to size on paint
            py[i] = RNG.nextFloat();
            vx[i] = (RNG.nextFloat() - 0.5f) * 0.0015f;
            vy[i] = (RNG.nextFloat() - 0.5f) * 0.0015f;
        }
    }

    private void step() {
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());
        for (int i = 0; i < count; i++) {
            px[i] += vx[i];
            py[i] += vy[i];
            // Wrap around the edges.
            if (px[i] < 0) px[i] += 1;
            if (px[i] > 1) px[i] -= 1;
            if (py[i] < 0) py[i] += 1;
            if (py[i] > 1) py[i] -= 1;

            // Gentle repulsion from the cursor.
            float dx = px[i] * w - mouseX;
            float dy = py[i] * h - mouseY;
            float dist2 = dx * dx + dy * dy;
            if (dist2 < 90 * 90 && dist2 > 1) {
                float force = 0.00008f * (1 - (float) Math.sqrt(dist2) / 90);
                px[i] += dx / w * force * 60;
                py[i] += dy / h * force * 60;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        if (drawBackground) {
            if (backgroundImage != null) {
                drawCover(g, backgroundImage, w, h);
                // Dark scrim so foreground UI stays readable over any image.
                g.setColor(new Color(0, 0, 0, 130));
                g.fillRect(0, 0, w, h);
            } else {
                g.setPaint(new java.awt.GradientPaint(0, 0, backgroundTop, 0, h, backgroundBottom));
                g.fillRect(0, 0, w, h);
            }
        }

        if (!particlesActive) {
            g.dispose();
            return;
        }

        // Connecting lines for nearby particles.
        int linkColor = particleColor.getRGB() & 0x00FFFFFF;
        for (int i = 0; i < count; i++) {
            int xi = (int) (px[i] * w);
            int yi = (int) (py[i] * h);
            for (int j = i + 1; j < count; j++) {
                int xj = (int) (px[j] * w);
                int yj = (int) (py[j] * h);
                int dx = xi - xj;
                int dy = yi - yj;
                int d2 = dx * dx + dy * dy;
                if (d2 < 120 * 120) {
                    int alpha = (int) (60 * (1 - Math.sqrt(d2) / 120));
                    g.setColor(new Color(linkColor | (alpha << 24), true));
                    g.drawLine(xi, yi, xj, yj);
                }
            }
        }

        // The particles themselves.
        g.setColor(particleColor);
        for (int i = 0; i < count; i++) {
            int xi = (int) (px[i] * w);
            int yi = (int) (py[i] * h);
            g.fillOval(xi - 2, yi - 2, 4, 4);
        }
        g.dispose();
    }

    /** Draws {@code img} scaled to cover the area, centred (like CSS cover). */
    private static void drawCover(Graphics2D g, java.awt.image.BufferedImage img, int w, int h) {
        double scale = Math.max(w / (double) img.getWidth(), h / (double) img.getHeight());
        int dw = (int) (img.getWidth() * scale);
        int dh = (int) (img.getHeight() * scale);
        g.drawImage(img, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
    }
}
