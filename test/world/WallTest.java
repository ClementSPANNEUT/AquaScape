package world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fluid.Droplet;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class WallTest {
    private final Wall wall = new Wall(100, 0, 0, 0);

    @Test
    void theClosestPointStaysOnTheSegment() {
        assertEquals(new Point2D.Double(50, 0), wall.closestPoint(50, 30));
        assertEquals(new Point2D.Double(100, 0), wall.closestPoint(180, 30));
        assertEquals(new Point2D.Double(0, 0), wall.closestPoint(-20, -5));
    }

    @Test
    void aDiscTouchesItWithinItsThickness() {
        assertTrue(wall.touches(50, 8, 6), "8 px away, disc of 6 px, wall 3 px thick on each side");
        assertFalse(wall.touches(50, 10, 6));
        assertFalse(wall.touches(120, 0, 6), "past the end of the wall");
    }

    @Test
    void itsBoundsDontDependOnTheOrderOfItsEnds() {
        assertEquals(0, wall.minX());
        assertEquals(100, wall.maxX());
        assertEquals(0, wall.minY());
        assertEquals(0, wall.maxY());
    }

    @Test
    void itsGlowIsVisibleJustOutsideIt() {
        assertTrue(wall.isVisibleIn(new Rectangle2D.Double(-30, -30, 20, 20)));
        assertFalse(wall.isVisibleIn(new Rectangle2D.Double(500, 500, 10, 10)));
    }

    @Test
    void aWallWithoutLengthIsAPoint() {
        Wall dot = new Wall(40, 40, 40, 40);
        assertEquals(new Point2D.Double(40, 40), dot.closestPoint(90, 10));
        assertTrue(dot.touches(new Droplet(45, 40, 3)));
        assertFalse(dot.touches(new Droplet(60, 40, 3)));
        assertEquals(3, dot.halfThickness());
    }

    @Test
    void itGlowsAndWavesWithTime() {
        Wall bar = new Wall(20, 50, 380, 50);
        int[] now = pixels(bar, 0);
        assertTrue(Arrays.stream(now).anyMatch(rgb -> (rgb & 0xFFFFFF) != 0));
        assertFalse(Arrays.equals(now, pixels(bar, 1.5)), "the bar is not still");
        assertTrue(Arrays.stream(pixels(new Wall(60, 60, 60, 60), 0)).anyMatch(rgb -> (rgb & 0xFFFFFF) != 0));
    }

    private static int[] pixels(Wall wall, double time) {
        BufferedImage image = new BufferedImage(400, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        wall.draw(g, time);
        g.dispose();
        return image.getRGB(0, 0, 400, 100, null, 0, 400);
    }
}
