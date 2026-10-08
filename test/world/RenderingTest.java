package world;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import zone.Levels;

class RenderingTest {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private static int litPixels(World world, double centerX, double centerY) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle view = new Rectangle((int) centerX - WIDTH / 2, (int) centerY - HEIGHT / 2, WIDTH, HEIGHT);
        g.translate(-view.x, -view.y);
        world.draw(g, view);
        g.dispose();
        int lit = 0;
        for (int y = 0; y < HEIGHT; y += 4) {
            for (int x = 0; x < WIDTH; x += 4) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    lit++;
                }
            }
        }
        return lit;
    }

    @Test
    void everyPartOfTheWorldCanBeDrawn() {
        World world = new World(Levels.get(Levels.WORLD));
        for (int frame = 0; frame < 30; frame++) {
            world.update(1 / 60.0, 1, 0);
        }
        assertTrue(litPixels(world, world.player().x(), world.player().y()) > 0, "around the player");
        for (Light light : world.lights()) {
            assertTrue(litPixels(world, light.x(), light.y()) > 0, "around the light " + light.name());
        }
        for (Checkpoint checkpoint : world.checkpoints()) {
            assertTrue(litPixels(world, checkpoint.x(), checkpoint.y()) > 0, "around a checkpoint of " + checkpoint.area());
        }
    }

    @Test
    void theTestRoomCanBeDrawn() {
        World world = new World(Levels.get(Levels.TEST));
        assertTrue(litPixels(world, 1500, 1500) > 0);
    }
}
