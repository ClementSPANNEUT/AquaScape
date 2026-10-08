package world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fluid.Droplet;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class GoalTest {
    private final Goal goal = new Goal(100, 100, 200, 100, 0.5);

    private static int[] pixels(Goal goal) {
        BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        goal.draw(g);
        g.dispose();
        return image.getRGB(0, 0, 400, 300, null, 0, 400);
    }

    @Test
    void onlyTheDropsWhoseCentreIsInsideCount() {
        Droplet inside = new Droplet(200, 150, 20);
        Droplet outside = new Droplet(50, 50, 20);
        goal.measure(List.of(outside), inside.volume());
        assertFalse(goal.isFilled());
        goal.measure(List.of(inside, outside), 2 * inside.volume());
        assertTrue(goal.isFilled(), "half the water is enough");
    }

    @Test
    void itShowsOnlyNearItsArea() {
        assertTrue(goal.isVisibleIn(new Rectangle2D.Double(250, 150, 500, 500)));
        assertFalse(goal.isVisibleIn(new Rectangle2D.Double(1000, 1000, 50, 50)));
    }

    @Test
    void aFilledBasinIsDrawnDifferently() {
        Droplet drop = new Droplet(200, 150, 20);
        goal.measure(List.of(), drop.volume());
        int[] empty = pixels(goal);
        goal.measure(List.of(drop), drop.volume());
        assertFalse(Arrays.equals(empty, pixels(goal)));
    }
}
