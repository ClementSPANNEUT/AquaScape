package screen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import world.Plan;

class GridTest {
    private static final Rectangle VIEW = new Rectangle(-100, -100, 640, 480);

    private static BufferedImage grid(Plan plan) {
        BufferedImage image = new BufferedImage(VIEW.width, VIEW.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.translate(-VIEW.x, -VIEW.y);
        Grid.draw(g, VIEW, plan);
        g.dispose();
        return image;
    }

    private static BufferedImage compass(Plan plan) {
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        Grid.drawCompass(g, plan, 100, 110);
        g.dispose();
        return image;
    }

    private static boolean isGold(int rgb) {
        Color color = new Color(rgb);
        return color.getRed() > 120 && color.getRed() > color.getBlue() + 60;
    }

    @Test
    void theAxesOfThePlanAreGold() {
        BufferedImage image = grid(new Plan("monde", 0, 0, 0, 0, 0, 1));
        assertTrue(isGold(image.getRGB(-VIEW.x, 300)) || isGold(image.getRGB(-VIEW.x - 1, 300)), "x = 0");
        assertTrue(isGold(image.getRGB(300, -VIEW.y)) || isGold(image.getRGB(300, -VIEW.y - 1)), "y = 0");
        assertFalse(isGold(image.getRGB(300, 300)));
    }

    @Test
    void theGridTurnsAndStretchesWithThePlan() {
        BufferedImage flat = grid(new Plan("a", 0, 0, 0, 0, 0, 5.6));
        BufferedImage turned = grid(new Plan("b", 0, 0, 0, 0, 40, 5.6));
        BufferedImage overview = grid(new Plan("c", 0, 0, 0, 0, 0, 16));
        assertTrue(Swing.colours(flat) > 3, "lines and labels");
        assertFalse(Swing.same(flat, turned));
        assertFalse(Swing.same(flat, overview));
    }

    @Test
    void anyScaleGetsReadableLines() {
        for (double scale : new double[] {0.05, 0.5, 3, 100}) {
            assertTrue(Swing.colours(grid(new Plan("s", 0, 0, 0, 0, 0, scale))) > 2, "scale " + scale);
        }
    }

    @Test
    void theCompassFollowsTheAngleOfThePlan() {
        BufferedImage straight = compass(new Plan("Hub", 0, 0, 0, 0, 0, 16));
        BufferedImage turned = compass(new Plan("Lumière", 0, 0, 0, 0, 142, 6));
        assertTrue(Swing.colours(straight) > 3);
        assertFalse(Swing.same(straight, turned));
    }
}
