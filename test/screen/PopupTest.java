package screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class PopupTest {
    @Test
    void centresABoxOnTheScreen() {
        assertEquals(new Rectangle(250, 150, 300, 200), Popup.centered(300, 200, 800, 500));
    }

    @Test
    void keepsATallBoxBelowTheTopOfTheScreen() {
        assertEquals(8, Popup.centered(300, 900, 800, 500).y);
    }

    @Test
    void theContentGoesUnderTheTitleBar() {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        Rectangle box = new Rectangle(100, 100, 400, 300);
        Rectangle content = Popup.draw(g, box, "Pause");
        g.dispose();
        assertEquals(new Rectangle(100, 100 + Popup.HEADER_HEIGHT, 400, 300 - Popup.HEADER_HEIGHT), content);
    }
}
