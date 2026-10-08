package screen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/**
 * Faint specks of dust far behind the level, in layers that scroll slower than the walls (parallax): they show
 * the view moving even where the level is empty. Each speck comes from a hash of its tile, so the same place
 * always shows the same specks.
 */
public final class Backdrop {
    private static final int TILE = 140;
    private static final double[] DEPTHS = {0.3, 0.6};
    private static final Color[] COLORS = {new Color(90, 130, 210, 45), new Color(120, 180, 240, 80)};
    private static final double[] SIZES = {1.5, 2.5}; 

    private Backdrop() {
    }

    /**
     * Draws the specks behind the view.
     * @param g graphics drawing on the screen
     * @param viewX left of the view in the world, in px
     * @param viewY top of the view in the world, in px
     * @param width width of the view, in px
     * @param height height of the view, in px
     */
    public static void draw(Graphics2D g, double viewX, double viewY, int width, int height) {
        Ellipse2D.Double speck = new Ellipse2D.Double();
        for (int layer = 0; layer < DEPTHS.length; layer++) {
            double offsetX = viewX * DEPTHS[layer];
            double offsetY = viewY * DEPTHS[layer];
            int firstColumn = (int) Math.floor(offsetX / TILE);
            int lastColumn = (int) Math.floor((offsetX + width) / TILE);
            int firstRow = (int) Math.floor(offsetY / TILE);
            int lastRow = (int) Math.floor((offsetY + height) / TILE);
            g.setColor(COLORS[layer]);
            for (int row = firstRow; row <= lastRow; row++) {
                for (int column = firstColumn; column <= lastColumn; column++) {
                    long hash = hash(layer, column, row);
                    double x = (column + (hash & 0xFF) / 256.0) * TILE - offsetX;
                    double y = (row + (hash >>> 8 & 0xFF) / 256.0) * TILE - offsetY;
                    double size = SIZES[layer] * (1 + (hash >>> 16 & 0x3) / 3.0);
                    speck.setFrame(x - size / 2, y - size / 2, size, size);
                    g.fill(speck);
                }
            }
        }
    }

    private static long hash(int layer, int column, int row) {
        long h = layer * 0x9E3779B97F4A7C15L + column * 0xBF58476D1CE4E5B9L + row * 0x94D049BB133111EBL;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }
}
