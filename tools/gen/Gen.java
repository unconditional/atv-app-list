import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Generates launcher icon PNGs (legacy mipmaps) and TV banner PNGs.
 * Run from the repository root:  java -cp tools/gen Gen
 */
public class Gen {

    private static final String ROOT = "app/src/main/res";

    private static final Color BG_TOP = new Color(0xFF1B2330);
    private static final Color BG_BOTTOM = new Color(0xFF0C0F15);
    private static final Color ACCENT = new Color(0xFF26B0E1);
    private static final Color SOFT = new Color(255, 255, 255, 56);
    private static final Color TEXT = new Color(0xFFF2F4F7);
    private static final Color TEXT_DIM = new Color(0xFFB9BEC6);

    public static void main(String[] args) throws IOException {
        int[] iconSizes = {48, 72, 96, 144, 192};
        String[] densities = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
        for (int i = 0; i < iconSizes.length; i++) {
            File dir = new File(ROOT + "/mipmap-" + densities[i]);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("cannot create " + dir);
            }
            File out = new File(dir, "ic_launcher.png");
            ImageIO.write(drawIcon(iconSizes[i]), "png", out);
            System.out.println("wrote " + out);
        }

        File mdpi = new File(ROOT + "/drawable-mdpi");
        File xhdpi = new File(ROOT + "/drawable-xhdpi");
        //noinspection ResultOfMethodCallIgnored
        mdpi.mkdirs();
        //noinspection ResultOfMethodCallIgnored
        xhdpi.mkdirs();
        File bannerMdpi = new File(mdpi, "banner.png");
        File bannerXhdpi = new File(xhdpi, "banner.png");
        ImageIO.write(drawBanner(320, 180), "png", bannerMdpi);
        ImageIO.write(drawBanner(640, 360), "png", bannerXhdpi);
        System.out.println("wrote " + bannerMdpi);
        System.out.println("wrote " + bannerXhdpi);
    }

    private static BufferedImage drawIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int inset = Math.round(size * 0.06f);
        int w = size - 2 * inset;
        g.setPaint(new GradientPaint(
                inset, inset, BG_TOP,
                size - inset, size - inset, BG_BOTTOM));
        int radius = Math.round(size * 0.20f);
        g.fillRoundRect(inset, inset, w, w, radius, radius);

        int margin = Math.round(size * 0.17f);
        int gap = Math.round(size * 0.05f);
        int cell = (size - 2 * margin - 2 * gap) / 3;
        int cellRadius = Math.round(cell * 0.30f);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int x = margin + col * (cell + gap);
                int y = margin + row * (cell + gap);
                g.setColor((row == 1 && col == 1) ? ACCENT : SOFT);
                g.fillRoundRect(x, y, cell, cell, cellRadius, cellRadius);
            }
        }
        g.dispose();
        return img;
    }

    private static BufferedImage drawBanner(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, BG_TOP, 0, height, BG_BOTTOM));
        g.fillRect(0, 0, width, height);

        float s = height / 360f;
        int glyphSize = Math.round(212 * s);
        int gx = Math.round(64 * s);
        int gy = (height - glyphSize) / 2;

        g.setColor(new Color(0xFF1E2329));
        g.fillRoundRect(gx, gy, glyphSize, glyphSize,
                Math.round(44 * s), Math.round(44 * s));

        int margin = Math.round(glyphSize * 0.17f);
        int gap = Math.round(glyphSize * 0.05f);
        int cell = (glyphSize - 2 * margin - 2 * gap) / 3;
        int cellRadius = Math.round(cell * 0.28f);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int x = gx + margin + col * (cell + gap);
                int y = gy + margin + row * (cell + gap);
                g.setColor((row == 1 && col == 1) ? ACCENT : new Color(255, 255, 255, 52));
                g.fillRoundRect(x, y, cell, cell, cellRadius, cellRadius);
            }
        }

        int tx = gx + glyphSize + Math.round(52 * s);
        int titleSize = Math.round(66 * s);
        int subSize = Math.round(30 * s);
        g.setFont(new Font("Segoe UI", Font.BOLD, titleSize));
        g.setColor(TEXT);
        int titleBaseline = height / 2 - Math.round(8 * s);
        g.drawString("ATV App List", tx, titleBaseline);

        g.setFont(new Font("Segoe UI", Font.PLAIN, subSize));
        g.setColor(TEXT_DIM);
        g.drawString("All the apps on your TV", tx, titleBaseline + Math.round(56 * s));

        g.dispose();
        return img;
    }
}
