package com.spaceline.launcher.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * A 3D, 360°-rotatable preview of a Minecraft skin.
 *
 * <p>Builds the player from textured boxes (head, body, arms, legs), rotates them
 * about the vertical axis (plus a slight tilt), projects orthographically and
 * paints each visible face by mapping its texture region onto the projected
 * parallelogram. Faces are drawn far-to-near (painter's algorithm). Orthographic
 * rotation keeps every face a parallelogram, so Java2D's affine image mapping is
 * exact. Drag to spin, or enable auto-rotate.
 */
public final class SkinModelView extends JPanel {

    private BufferedImage skin;
    private double yaw = 0.5;      // radians
    private double pitch = -0.18;  // slight downward tilt
    private final Timer spinner;
    private boolean autoRotate = true;

    public SkinModelView() {
        setOpaque(true);
        setBackground(new Color(0x12, 0x16, 0x22));
        this.spinner = new Timer(33, e -> {
            if (autoRotate) {
                yaw += 0.03;
                repaint();
            }
        });
        DragRotate drag = new DragRotate();
        addMouseListener(drag);
        addMouseMotionListener(drag);
    }

    public void setSkin(BufferedImage skin) {
        this.skin = skin;
        repaint();
    }

    public void setAutoRotate(boolean autoRotate) {
        this.autoRotate = autoRotate;
    }

    public void start() {
        spinner.start();
    }

    public void stop() {
        spinner.stop();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (skin == null) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        double scale = Math.min(getWidth(), getHeight()) / 42.0;
        double cx = getWidth() / 2.0;
        double cy = getHeight() / 2.0 + 4 * scale;

        List<Face> faces = new ArrayList<>();
        // Head, body, arms, legs as textured boxes (centre x,y,z; half extents).
        addBox(faces, 0, 28, 0, 4, 4, 4, UV.HEAD);
        addBox(faces, 0, 18, 0, 4, 6, 2, UV.BODY);
        addBox(faces, -6, 18, 0, 2, 6, 2, UV.RIGHT_ARM);
        addBox(faces, 6, 18, 0, 2, 6, 2, UV.LEFT_ARM);
        addBox(faces, -2, 6, 0, 2, 6, 2, UV.RIGHT_LEG);
        addBox(faces, 2, 6, 0, 2, 6, 2, UV.LEFT_LEG);

        // Project + sort far-to-near.
        for (Face f : faces) {
            f.project(yaw, pitch, scale, cx, cy);
        }
        faces.sort((a, b) -> Double.compare(a.depth, b.depth));

        for (Face f : faces) {
            try {
                f.draw(g, skin);
            } catch (RuntimeException ignored) {
                // A degenerate face (edge-on) is simply skipped.
            }
        }
        g.dispose();
    }

    private void addBox(List<Face> out, double cx, double cy, double cz,
                        double hx, double hy, double hz, int[][] uv) {
        // Corner offsets per face: {TL, TR, BR, BL}.
        double[][] front = {{-hx, hy, hz}, {hx, hy, hz}, {hx, -hy, hz}, {-hx, -hy, hz}};
        double[][] back = {{hx, hy, -hz}, {-hx, hy, -hz}, {-hx, -hy, -hz}, {hx, -hy, -hz}};
        double[][] left = {{hx, hy, hz}, {hx, hy, -hz}, {hx, -hy, -hz}, {hx, -hy, hz}};
        double[][] right = {{-hx, hy, -hz}, {-hx, hy, hz}, {-hx, -hy, hz}, {-hx, -hy, -hz}};
        double[][] top = {{-hx, hy, -hz}, {hx, hy, -hz}, {hx, hy, hz}, {-hx, hy, hz}};
        double[][] bottom = {{-hx, -hy, hz}, {hx, -hy, hz}, {hx, -hy, -hz}, {-hx, -hy, -hz}};
        double[][][] geo = {top, bottom, right, front, left, back};
        for (int i = 0; i < 6; i++) {
            out.add(new Face(geo[i], cx, cy, cz, uv[i]));
        }
    }

    /** One textured quad. */
    private static final class Face {
        final double[][] offsets; // 4 corners as offsets from centre
        final double cx, cy, cz;
        final int[] uv;           // {u, v, w, h} into the 64x64 texture
        final double[] sx = new double[4];
        final double[] sy = new double[4];
        double depth;

        Face(double[][] offsets, double cx, double cy, double cz, int[] uv) {
            this.offsets = offsets;
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.uv = uv;
        }

        void project(double yaw, double pitch, double scale, double ox, double oy) {
            double sumZ = 0;
            double cosY = Math.cos(yaw), sinY = Math.sin(yaw);
            double cosX = Math.cos(pitch), sinX = Math.sin(pitch);
            for (int i = 0; i < 4; i++) {
                double x = cx + offsets[i][0];
                double y = cy + offsets[i][1];
                double z = cz + offsets[i][2];
                // Rotate about Y, then X.
                double x1 = x * cosY + z * sinY;
                double z1 = -x * sinY + z * cosY;
                double y1 = y * cosX - z1 * sinX;
                double z2 = y * sinX + z1 * cosX;
                sx[i] = ox + x1 * scale;
                sy[i] = oy - y1 * scale;
                sumZ += z2;
            }
            depth = sumZ / 4.0;
        }

        void draw(Graphics2D g, BufferedImage skin) {
            BufferedImage tex = skin.getSubimage(uv[0], uv[1], uv[2], uv[3]);
            // Affine mapping (0,0)->TL, (w,0)->TR, (0,h)->BL.
            double w = uv[2], h = uv[3];
            double m00 = (sx[1] - sx[0]) / w;
            double m10 = (sy[1] - sy[0]) / w;
            double m01 = (sx[3] - sx[0]) / h;
            double m11 = (sy[3] - sy[0]) / h;
            AffineTransform at = new AffineTransform(m00, m10, m01, m11, sx[0], sy[0]);

            Shape oldClip = g.getClip();
            Path2D quad = new Path2D.Double();
            quad.moveTo(sx[0], sy[0]);
            quad.lineTo(sx[1], sy[1]);
            quad.lineTo(sx[2], sy[2]);
            quad.lineTo(sx[3], sy[3]);
            quad.closePath();
            g.clip(quad);
            AffineTransform oldTx = g.getTransform();
            g.transform(at);
            g.drawImage(tex, 0, 0, null);
            g.setTransform(oldTx);
            g.setClip(oldClip);
        }
    }

    /** Standard 64x64 skin UV rectangles, per face order: top,bottom,right,front,left,back. */
    private static final class UV {
        static final int[][] HEAD = {
                {8, 0, 8, 8}, {16, 0, 8, 8}, {0, 8, 8, 8}, {8, 8, 8, 8}, {16, 8, 8, 8}, {24, 8, 8, 8}};
        static final int[][] BODY = {
                {20, 16, 8, 4}, {28, 16, 8, 4}, {16, 20, 4, 12}, {20, 20, 8, 12}, {28, 20, 4, 12}, {32, 20, 8, 12}};
        static final int[][] RIGHT_ARM = {
                {44, 16, 4, 4}, {48, 16, 4, 4}, {40, 20, 4, 12}, {44, 20, 4, 12}, {48, 20, 4, 12}, {52, 20, 4, 12}};
        static final int[][] LEFT_ARM = {
                {36, 48, 4, 4}, {40, 48, 4, 4}, {32, 52, 4, 12}, {36, 52, 4, 12}, {40, 52, 4, 12}, {44, 52, 4, 12}};
        static final int[][] RIGHT_LEG = {
                {4, 16, 4, 4}, {8, 16, 4, 4}, {0, 20, 4, 12}, {4, 20, 4, 12}, {8, 20, 4, 12}, {12, 20, 4, 12}};
        static final int[][] LEFT_LEG = {
                {20, 48, 4, 4}, {24, 48, 4, 4}, {16, 52, 4, 12}, {20, 52, 4, 12}, {24, 52, 4, 12}, {28, 52, 4, 12}};
    }

    private final class DragRotate extends java.awt.event.MouseAdapter {
        private int lastX;
        private int lastY;

        @Override
        public void mousePressed(java.awt.event.MouseEvent e) {
            lastX = e.getX();
            lastY = e.getY();
            autoRotate = false;
        }

        @Override
        public void mouseDragged(java.awt.event.MouseEvent e) {
            yaw += (e.getX() - lastX) * 0.01;
            pitch += (e.getY() - lastY) * 0.01;
            pitch = Math.max(-0.8, Math.min(0.8, pitch));
            lastX = e.getX();
            lastY = e.getY();
            repaint();
        }
    }
}
