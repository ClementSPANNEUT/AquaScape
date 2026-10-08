package world;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TileCacheTest {
    private static final int TILE = TileCache.TILE;

    private final List<Rectangle> painted = new ArrayList<>();
    private final TileCache cache = new TileCache((g, area) -> {
        painted.add(area);
        paintLayer(g);
    });

    private static void paintLayer(Graphics2D g) {
        g.setColor(new Color(200, 40, 40));
        g.fill(new Ellipse2D.Double(100, 100, 700, 420));
        g.setColor(new Color(40, 90, 220, 120));
        g.fillRect(300, 0, 150, 2000);
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private BufferedImage throughTheCache(Rectangle view, double scale) {
        BufferedImage image = new BufferedImage((int) (view.width * scale), (int) (view.height * scale),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.scale(scale, scale);
        g.translate(-view.x, -view.y);
        cache.draw(g, view);
        g.dispose();
        return image;
    }

    private static BufferedImage directly(Rectangle view, double scale) {
        BufferedImage image = new BufferedImage((int) (view.width * scale), (int) (view.height * scale),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.scale(scale, scale);
        g.translate(-view.x, -view.y);
        paintLayer(g);
        g.dispose();
        return image;
    }

    @Test
    void theTilesShowTheLayerExactlyAsItIsPainted() {
        Rectangle view = new Rectangle(37, 61, 900, 600);
        assertArrayEquals(pixels(directly(view, 1)), pixels(throughTheCache(view, 1)));
    }

    @Test
    void aTileIsPaintedOnlyOnce() {
        Rectangle view = new Rectangle(0, 0, 3 * TILE, 2 * TILE);
        throughTheCache(view, 1);
        assertEquals(6, painted.size(), "three columns of two tiles");
        throughTheCache(view, 1);
        throughTheCache(new Rectangle(10, 10, 3 * TILE - 20, 2 * TILE - 20), 1);
        assertEquals(6, painted.size(), "nothing is painted again while the view stays on the same tiles");
        for (Rectangle area : painted) {
            assertEquals(0, area.x % TILE);
            assertEquals(0, area.y % TILE);
            assertEquals(TILE, area.width);
            assertEquals(TILE, area.height);
        }
    }

    @Test
    void scrollingOnlyPaintsTheTilesThatComeIntoView() {
        throughTheCache(new Rectangle(0, 0, 3 * TILE, 2 * TILE), 1);
        painted.clear();
        throughTheCache(new Rectangle(40, 0, 3 * TILE, 2 * TILE), 1);
        assertEquals(2, painted.size(), "one more column");
        assertEquals(3 * TILE, painted.get(0).x);
    }

    @Test
    void itWorksLeftOfAndAboveTheOrigin() {
        Rectangle view = new Rectangle(-300, -200, 700, 500);
        assertArrayEquals(pixels(directly(view, 1)), pixels(throughTheCache(view, 1)));
        assertTrue(painted.stream().anyMatch(area -> area.x == -2 * TILE && area.y == -TILE));
    }

    @Test
    void theTilesFollowTheScalingOfTheScreen() {
        Rectangle view = new Rectangle(0, 0, 3 * TILE, 2 * TILE);
        assertArrayEquals(pixels(directly(view, 2)), pixels(throughTheCache(view, 2)), "sharp at twice the size");
        painted.clear();
        throughTheCache(view, 2);
        assertEquals(0, painted.size());
        throughTheCache(view, 1);
        assertEquals(6, painted.size(), "another scale needs its own tiles");
    }

    @Test
    void clearingPaintsTheLayerAgain() {
        Rectangle view = new Rectangle(0, 0, TILE, TILE);
        throughTheCache(view, 1);
        cache.clear();
        assertEquals(0, cache.size());
        throughTheCache(view, 1);
        assertEquals(2, painted.size());
    }

    @Test
    void theTilesNotUsedLatelyAreForgotten() {
        for (int i = 0; i < 400; i++) {
            throughTheCache(new Rectangle(i * 4 * TILE, 0, 2 * TILE, 2 * TILE), 1);
        }
        assertTrue(cache.size() < 100, "kept: " + cache.size());
        painted.clear();
        throughTheCache(new Rectangle(399 * 4 * TILE, 0, 2 * TILE, 2 * TILE), 1);
        assertEquals(0, painted.size(), "the last view is still there");
    }

    @Test
    void aTurnedViewIsPaintedDirectly() {
        BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.rotate(0.3);
        cache.draw(g, new Rectangle(0, 0, 400, 400));
        cache.draw(g, new Rectangle(0, 0, 400, 400));
        g.dispose();
        assertEquals(2, painted.size(), "once per draw, without tiles");
        assertEquals(0, cache.size());
        assertTrue((image.getRGB(275, 294) & 0xFFFFFF) != 0, "the layer shows, turned with the view");
    }
}
