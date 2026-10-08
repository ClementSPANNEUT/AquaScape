package fluid;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.List;

/**
 * Draws all the water as one neon metaball field: drops close to each other blend together, the surface gets a
 * bright rim and the whole fluid a soft glow. Only the part of the view around the drops is computed.
 */
public class FluidRenderer {
    /** Neon cyan of the water, also used by the HUD. */
    public static final Color NEON = new Color(0, 240, 255);
    private static final int CELL = 3; 
    private static final int GLOW_PAD = 16;
    private static final double COVER_START = 0.05;
    private static final double SURFACE_LOW = 0.45;
    private static final double SURFACE_HIGH = 0.75;
    private static final double RIM_LEVEL = 0.72;
    private static final double RIM_WIDTH = 0.42;
    private static final Color MARKER_HALO = new Color(0, 240, 255, 22);
    private static final Color MARKER_RING = new Color(0, 240, 255, 140);
    private static final Color MARKER_CORE = new Color(230, 254, 255);
    private static final Color ANCHOR_DANGER = new Color(255, 90, 80);
    private static final BasicStroke ANCHOR_STROKE = new BasicStroke(2.5f);
    private static final double ANCHOR_RADIUS = 11;
    private static final double ANCHOR_SAFE_TENSION = 0.6;

    private int gridWidth;
    private int gridHeight;
    private float[] field;
    private float[] cover;
    private float[] near;
    private float[] far;
    private float[] blurTemp;
    private float[] blurSpare;
    private BufferedImage core;
    private BufferedImage glow;
    private int[] corePixels;
    private int[] glowPixels;
    private int originX;
    private int originY;
    private int left;
    private int top;
    private int right;
    private int bottom;

    /** Creates a renderer; its buffers are allocated on the first draw. */
    public FluidRenderer() {
    }

    /**
     * Draws the water seen in the view, with g drawing in the world. The grid is anchored on whole cells of the
     * world, so moving the view doesn't make the water shimmer.
     * @param g graphics drawing in the world
     * @param droplets the drops to draw
     * @param viewX left of the view, in world px
     * @param viewY top of the view, in world px
     * @param width width of the view, in px
     * @param height height of the view, in px
     */
    public void draw(Graphics2D g, List<Droplet> droplets, int viewX, int viewY, int width, int height) {
        if (width <= 0 || height <= 0 || droplets.isEmpty()) {
            return;
        }
        originX = Math.floorDiv(viewX, CELL) * CELL;
        originY = Math.floorDiv(viewY, CELL) * CELL;
        allocate(width / CELL + 2, height / CELL + 2);
        if (!frame(droplets)) {
            return;
        }
        for (int y = top; y <= bottom; y++) {
            Arrays.fill(field, y * gridWidth + left, y * gridWidth + right + 1, 0f);
        }
        for (Droplet droplet : droplets) {
            droplet.addField(field, gridWidth, gridHeight, CELL, originX, originY);
        }
        shadeCore();

        blur(cover, near, 2);
        blur(cover, far, 5);
        int neon = NEON.getRGB() & 0xFFFFFF;
        for (int y = top; y <= bottom; y++) {
            for (int i = y * gridWidth + left; i <= y * gridWidth + right; i++) {
                float alpha = Math.min(1f, 0.9f * near[i] + 1.3f * far[i]) * (1f - 0.55f * cover[i]);
                glowPixels[i] = ((int) (alpha * 170) << 24) | neon;
            }
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int x1 = originX + left * CELL;
        int y1 = originY + top * CELL;
        int x2 = originX + (right + 1) * CELL;
        int y2 = originY + (bottom + 1) * CELL;
        g2.drawImage(glow, x1, y1, x2, y2, left, top, right + 1, bottom + 1, null);
        g2.drawImage(core, x1, y1, x2, y2, left, top, right + 1, bottom + 1, null);
        g2.dispose();
    }

    /**
     * Draws a glowing dot, e.g. to show which drop the player controls.
     * @param g graphics drawing in the world
     * @param x centre x, in world px
     * @param y centre y, in world px
     */
    public static void drawMarker(Graphics2D g, double x, double y) {
        int cx = (int) Math.round(x);
        int cy = (int) Math.round(y);
        g.setColor(MARKER_HALO);
        for (int k = 4; k >= 1; k--) {
            g.fillOval(cx - k * 5, cy - k * 5, k * 10, k * 10);
        }
        g.setColor(MARKER_RING);
        g.fillOval(cx - 6, cy - 6, 12, 12);
        g.setColor(MARKER_CORE);
        g.fillOval(cx - 3, cy - 3, 6, 6);
    }

    /**
     * Draws a ring where the back of a drop is stuck to the ground; it turns from cyan to red as the drop gets
     * close to tearing.
     * @param g graphics drawing in the world
     * @param x centre x, in world px
     * @param y centre y, in world px
     * @param tension how stretched the drop is, from 0 to 1 where it tears
     */
    public static void drawAnchor(Graphics2D g, double x, double y, double tension) {
        double danger = Math.max(0, Math.min(1, (tension - ANCHOR_SAFE_TENSION) / (1 - ANCHOR_SAFE_TENSION)));
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color((int) (NEON.getRed() + (ANCHOR_DANGER.getRed() - NEON.getRed()) * danger),
                (int) (NEON.getGreen() + (ANCHOR_DANGER.getGreen() - NEON.getGreen()) * danger),
                (int) (NEON.getBlue() + (ANCHOR_DANGER.getBlue() - NEON.getBlue()) * danger), 220));
        g2.setStroke(ANCHOR_STROKE);
        g2.draw(new Ellipse2D.Double(x - ANCHOR_RADIUS, y - ANCHOR_RADIUS, 2 * ANCHOR_RADIUS, 2 * ANCHOR_RADIUS));
        g2.fill(new Ellipse2D.Double(x - 3, y - 3, 6, 6));
        g2.dispose();
    }

    private boolean frame(List<Droplet> droplets) {
        left = gridWidth;
        top = gridHeight;
        right = -1;
        bottom = -1;
        for (Droplet droplet : droplets) {
            Rectangle2D bounds = droplet.fieldBounds();
            int minX = cell(bounds.getMinX() - originX) - GLOW_PAD;
            int maxX = cell(bounds.getMaxX() - originX) + GLOW_PAD;
            int minY = cell(bounds.getMinY() - originY) - GLOW_PAD;
            int maxY = cell(bounds.getMaxY() - originY) + GLOW_PAD;
            if (maxX < 0 || minX >= gridWidth || maxY < 0 || minY >= gridHeight) {
                continue;
            }
            left = Math.min(left, Math.max(0, minX));
            right = Math.max(right, Math.min(gridWidth - 1, maxX));
            top = Math.min(top, Math.max(0, minY));
            bottom = Math.max(bottom, Math.min(gridHeight - 1, maxY));
        }
        return left <= right;
    }

    private static int cell(double position) {
        return (int) Math.floor(position / CELL);
    }

  
    private void shadeCore() {
        for (int y = top; y <= bottom; y++) {
            for (int i = y * gridWidth + left; i <= y * gridWidth + right; i++) {
                double influence = field[i];
                corePixels[i] = 0;
                cover[i] = 0;
                if (influence < COVER_START) {
                    continue;
                }
                cover[i] = (float) smooth((influence - COVER_START) / (SURFACE_HIGH - COVER_START));
                if (influence <= SURFACE_LOW) {
                    continue;
                }
                double surface = smooth((influence - SURFACE_LOW) / (SURFACE_HIGH - SURFACE_LOW));
                double depth = Math.min(1, Math.max(0, (influence - SURFACE_HIGH) / 1.6));
                double rim = smooth(1 - Math.abs(influence - RIM_LEVEL) / RIM_WIDTH) * surface;
                double alpha = surface * (70 + 95 * depth);
                alpha += (255 - alpha) * rim;
                corePixels[i] = ((int) alpha << 24)
                        | (mix(NEON.getRed(), 0.8 * rim) << 16)
                        | (mix(NEON.getGreen(), 0.8 * rim) << 8)
                        | mix(NEON.getBlue(), 0.8 * rim);
            }
        }
    }

    private static double smooth(double t) {
        double c = Math.max(0, Math.min(1, t));
        return c * c * (3 - 2 * c);
    }

    private static int mix(int channel, double amount) {
        return (int) (channel + (255 - channel) * amount);
    }

    private void allocate(int width, int height) {
        if (width == gridWidth && height == gridHeight) {
            return;
        }
        gridWidth = width;
        gridHeight = height;
        int size = width * height;
        field = new float[size];
        cover = new float[size];
        near = new float[size];
        far = new float[size];
        blurTemp = new float[size];
        blurSpare = new float[size];
        core = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        glow = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        corePixels = ((DataBufferInt) core.getRaster().getDataBuffer()).getData();
        glowPixels = ((DataBufferInt) glow.getRaster().getDataBuffer()).getData();
    }

    private void blur(float[] source, float[] out, int radius) {
        boxBlur(source, out, radius);
        boxBlur(out, blurSpare, radius);
        boxBlur(blurSpare, out, radius);
    }

    private void boxBlur(float[] source, float[] out, int radius) {
        float scale = 1f / (2 * radius + 1);
        for (int y = top; y <= bottom; y++) {
            int row = y * gridWidth;
            float sum = 0;
            for (int x = left; x <= left + radius && x <= right; x++) {
                sum += source[row + x];
            }
            for (int x = left; x <= right; x++) {
                blurTemp[row + x] = sum * scale;
                if (x + radius + 1 <= right) {
                    sum += source[row + x + radius + 1];
                }
                if (x - radius >= left) {
                    sum -= source[row + x - radius];
                }
            }
        }
        for (int x = left; x <= right; x++) {
            float sum = 0;
            for (int y = top; y <= top + radius && y <= bottom; y++) {
                sum += blurTemp[y * gridWidth + x];
            }
            for (int y = top; y <= bottom; y++) {
                out[y * gridWidth + x] = sum * scale;
                if (y + radius + 1 <= bottom) {
                    sum += blurTemp[(y + radius + 1) * gridWidth + x];
                }
                if (y - radius >= top) {
                    sum -= blurTemp[(y - radius) * gridWidth + x];
                }
            }
        }
    }
}
