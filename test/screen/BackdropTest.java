package screen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class BackdropTest {
    private static BufferedImage backdrop(double viewX, double viewY) {
        BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        Backdrop.draw(g, viewX, viewY, 400, 300);
        g.dispose();
        return image;
    }

    @Test
    void theSamePlaceAlwaysShowsTheSameSpecks() {
        BufferedImage first = backdrop(1234, -567);
        assertTrue(Swing.colours(first) > 1);
        assertTrue(Swing.same(first, backdrop(1234, -567)));
    }

    @Test
    void theSpecksMoveWithTheView() {
        assertFalse(Swing.same(backdrop(0, 0), backdrop(200, 50)));
    }
}
