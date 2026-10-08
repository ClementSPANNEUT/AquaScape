package world;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Draws a layer of the world that never changes, such as the tunnel, from square image tiles instead of drawing
 * it again at every frame: a tile is painted the first time it shows, then only copied to the screen, which costs
 * far less than shapes with smooth edges and glows.
 * <p>The tiles lie on a fixed grid of the world, so scrolling only paints the few tiles that come into view. They
 * are painted at the resolution of the screen, whatever its scaling. Only the tiles used lately are kept; the
 * tiles that came out empty hold no image, and those of one plain colour share the same one.
 */
public final class TileCache {
    /** Paints the layer. */
    public interface Painter {
        /**
         * Paints the part of the layer that shows in an area.
         * @param g graphics drawing in the world, clipped to the area
         * @param area the area to paint, in world px
         */
        void paint(Graphics2D g, Rectangle area);
    }

    /** Side of a tile, in world px. */
    public static final int TILE = 256;
    private static final int MIN_TILES = 48;

    private record Tile(BufferedImage image) {
    }

    private final Painter painter;
    private final Map<Integer, BufferedImage> plainTiles = new HashMap<>();
    private final Map<Long, Tile> tiles = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Tile> eldest) {
            return size() > capacity;
        }
    };
    private int capacity = MIN_TILES;
    private double scale;

    /**
     * Creates an empty cache.
     * @param painter what paints the layer, tile by tile
     */
    public TileCache(Painter painter) {
        this.painter = painter;
    }

    /** Forgets every tile, when the layer has changed. */
    public void clear() {
        tiles.clear();
        plainTiles.clear();
    }

    /** {@return how many tiles are kept, for the tests} */
    int size() {
        return tiles.size();
    }

    /**
     * Draws the layer over the view, painting the tiles it doesn't hold yet. If the graphics turn or stretch the
     * world unevenly, tiles can't be used and the layer is painted directly.
     * @param g graphics drawing in the world
     * @param view the visible area, in world px
     */
    public void draw(Graphics2D g, Rectangle2D view) {
        AffineTransform transform = g.getTransform();
        if (transform.getShearX() != 0 || transform.getShearY() != 0 || transform.getScaleX() <= 0
                || transform.getScaleX() != transform.getScaleY()) {
            Rectangle area = view.getBounds();
            Graphics2D direct = (Graphics2D) g.create();
            direct.clip(area);
            painter.paint(direct, area);
            direct.dispose();
            return;
        }
        if (transform.getScaleX() != scale) {
            scale = transform.getScaleX();
            clear();
        }
        int firstColumn = (int) Math.floor(view.getMinX() / TILE);
        int lastColumn = (int) Math.floor((view.getMaxX() - 1e-9) / TILE);
        int firstRow = (int) Math.floor(view.getMinY() / TILE);
        int lastRow = (int) Math.floor((view.getMaxY() - 1e-9) / TILE);
        capacity = Math.max(capacity, 2 * (lastColumn - firstColumn + 1) * (lastRow - firstRow + 1));

        Graphics2D plain = (Graphics2D) g.create();
        plain.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        for (int row = firstRow; row <= lastRow; row++) {
            for (int column = firstColumn; column <= lastColumn; column++) {
                long key = ((long) row << 32) ^ (column & 0xFFFFFFFFL);
                Tile tile = tiles.get(key);
                if (tile == null) {
                    tile = paint(g, column, row);
                    tiles.put(key, tile);
                }
                if (tile.image != null) {
                    plain.drawImage(tile.image, column * TILE, row * TILE, TILE, TILE, null);
                }
            }
        }
        plain.dispose();
    }

    private Tile paint(Graphics2D screen, int column, int row) {
        int size = (int) Math.ceil(TILE * scale);
        BufferedImage image = screen.getDeviceConfiguration().createCompatibleImage(size, size, Transparency.TRANSLUCENT);
        Rectangle area = new Rectangle(column * TILE, row * TILE, TILE, TILE);
        Graphics2D g = image.createGraphics();
        g.setRenderingHints(screen.getRenderingHints());
        g.scale(size / (double) TILE, size / (double) TILE);
        g.translate(-area.x, -area.y);
        g.clip(area);
        painter.paint(g, area);
        g.dispose();

        int first = image.getRGB(0, 0);
        int[] pixels = image.getRGB(0, 0, size, size, null, 0, size);
        for (int pixel : pixels) {
            if (pixel != first) {
                return new Tile(image);
            }
        }
        return new Tile(first >>> 24 == 0 ? null : plainTiles.computeIfAbsent(first, colour -> image));
    }
}
