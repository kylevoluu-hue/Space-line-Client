package com.spaceline.launcher.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.Timer;

import com.spaceline.common.ui.TextColorStyle;

/**
 * A label that paints its text using a {@link TextColorStyle}, supporting solid,
 * per-character gradient and animated chrome/RGB colouring. Used for the title
 * wordmark and the appearance preview so the user sees their colour choice live.
 */
public final class AnimatedTitleLabel extends JComponent {

    private String text;
    private TextColorStyle style;
    private final Timer timer;

    public AnimatedTitleLabel(String text, Font font, TextColorStyle style) {
        this.text = text;
        this.style = style;
        setFont(font);
        this.timer = new Timer(40, e -> repaint());
        timer.start();
    }

    public void setText(String text) {
        this.text = text;
        revalidate();
        repaint();
    }

    public void setStyle(TextColorStyle style) {
        this.style = style;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Graphics g = getGraphics();
        java.awt.FontMetrics fm = (g != null ? g.getFontMetrics(getFont())
                : getFontMetrics(getFont()));
        int w = fm.stringWidth(text == null ? "" : text) + 8;
        return new Dimension(w, fm.getHeight() + 6);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(getFont());
        java.awt.FontMetrics fm = g.getFontMetrics();
        long now = System.currentTimeMillis();
        int x = 4;
        int y = fm.getAscent() + 2;
        int len = Math.max(1, text.length() - 1);
        for (int i = 0; i < text.length(); i++) {
            float t = text.length() == 1 ? 0f : (float) i / len;
            g.setColor(new Color(style.colorAt(t, now), true));
            String ch = String.valueOf(text.charAt(i));
            g.drawString(ch, x, y);
            x += fm.stringWidth(ch);
        }
        g.dispose();
    }
}
