import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import javax.imageio.ImageIO;

/**
 * Renders the Space~line "black hole" app icon (with the Spaceline wordmark)
 * to branding/spaceline.png and a multi-size branding/spaceline.ico.
 * Uses Java2D so we get real font rendering. Run headless.
 */
public class IconGenerator {

    static final int[] SIZES = {256, 128, 64, 48, 32, 16};

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Paths.get("branding"));

        // Largest, with wordmark, becomes the standalone PNG.
        BufferedImage big = render(256);
        ImageIO.write(big, "png", new File("branding/spaceline.png"));

        // Pack an ICO with PNG payloads at every size.
        byte[][] pngs = new byte[SIZES.length][];
        for (int i = 0; i < SIZES.length; i++) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(render(SIZES[i]), "png", bos);
            pngs[i] = bos.toByteArray();
        }
        writeIco(Paths.get("branding/spaceline.ico"), pngs);
        System.out.println("Wrote branding/spaceline.png and branding/spaceline.ico");
    }

    static BufferedImage render(int s) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        boolean withText = s >= 64;

        // Rounded-square background with a vertical navy gradient, used as a clip.
        float arc = s * 0.30f;
        RoundRectangle2D bg = new RoundRectangle2D.Float(0, 0, s, s, arc, arc);
        g.setClip(bg);
        g.setPaint(new GradientPaint(0, 0, new Color(0x12, 0x18, 0x2A),
                0, s, new Color(0x06, 0x09, 0x12)));
        g.fill(bg);

        // Stars.
        g.setColor(new Color(255, 255, 255, 200));
        double[][] stars = {{0.18, 0.20}, {0.80, 0.16}, {0.86, 0.74}, {0.16, 0.72}, {0.55, 0.12}, {0.40, 0.86}};
        for (double[] st : stars) {
            double sx = st[0] * s, sy = st[1] * s, rad = Math.max(0.6, s * 0.008);
            g.fill(new Ellipse2D.Double(sx - rad, sy - rad, rad * 2, rad * 2));
        }

        double cx = s * 0.5;
        double cy = withText ? s * 0.43 : s * 0.5;
        double ring = s * 0.30;

        // Glowing, multi-hue accretion ring drawn as conic-emulated arc segments
        // over a few passes (wide+faint for glow, narrow+bright for the core ring).
        float[][] passes = {{0.17f, 0.10f}, {0.11f, 0.20f}, {0.055f, 0.85f}};
        for (float[] pass : passes) {
            g.setStroke(new BasicStroke(s * pass[0], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int a = 0; a < 360; a += 4) {
                float hue = hueAt(a);
                Color c = Color.getHSBColor(hue, 0.55f, 1.0f);
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (pass[1] * 255)));
                Arc2D arcSeg = new Arc2D.Double(cx - ring, cy - ring, ring * 2, ring * 2, a, 4.5, Arc2D.OPEN);
                g.draw(arcSeg);
            }
        }

        // Event horizon (black core) with a thin bright rim.
        double core = s * 0.255;
        g.setColor(new Color(255, 255, 255, 70));
        g.setStroke(new BasicStroke((float) (s * 0.012)));
        g.draw(new Ellipse2D.Double(cx - core, cy - core, core * 2, core * 2));
        g.setColor(Color.BLACK);
        g.fill(new Ellipse2D.Double(cx - core, cy - core, core * 2, core * 2));

        // Wordmark.
        if (withText) {
            String text = "Spaceline";
            Font font = new Font("Serif", Font.BOLD, Math.round(s * 0.135f));
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(text);
            int tx = (int) (cx - tw / 2.0);
            int ty = (int) (s * 0.90);
            // Cyan glow behind the white text, then a crisp white wordmark.
            g.setColor(new Color(0x37, 0xE0, 0xFF, 90));
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    g.drawString(text, tx + dx, ty + dy);
                }
            }
            g.setColor(Color.WHITE);
            g.drawString(text, tx, ty);
        }

        g.dispose();
        return img;
    }

    /** Maps a degree to a hue that cycles cyan -> magenta -> blue -> cyan. */
    static float hueAt(int deg) {
        float t = (deg % 360) / 360f;
        // cyan(0.5) -> magenta(0.83) -> blue(0.66) -> cyan(0.5)
        if (t < 1f / 3) return lerp(0.50f, 0.83f, t * 3);
        if (t < 2f / 3) return lerp(0.83f, 0.66f, (t - 1f / 3) * 3);
        return lerp(0.66f, 0.50f, (t - 2f / 3) * 3);
    }

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    static void writeIco(Path out, byte[][] pngs) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(bos);
        writeLE16(d, 0);
        writeLE16(d, 1);
        writeLE16(d, pngs.length);
        int offset = 6 + 16 * pngs.length;
        for (int i = 0; i < pngs.length; i++) {
            int sz = SIZES[i];
            d.writeByte(sz >= 256 ? 0 : sz);
            d.writeByte(sz >= 256 ? 0 : sz);
            d.writeByte(0);
            d.writeByte(0);
            writeLE16(d, 1);
            writeLE16(d, 32);
            writeLE32(d, pngs[i].length);
            writeLE32(d, offset);
            offset += pngs[i].length;
        }
        for (byte[] png : pngs) {
            d.write(png);
        }
        Files.write(out, bos.toByteArray());
    }

    static void writeLE16(DataOutputStream d, int v) throws IOException {
        d.writeByte(v & 0xFF);
        d.writeByte((v >> 8) & 0xFF);
    }

    static void writeLE32(DataOutputStream d, int v) throws IOException {
        d.writeByte(v & 0xFF);
        d.writeByte((v >> 8) & 0xFF);
        d.writeByte((v >> 16) & 0xFF);
        d.writeByte((v >> 24) & 0xFF);
    }
}
