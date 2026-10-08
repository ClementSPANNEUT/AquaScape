package world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fluid.Droplet;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class DoorButtonTest {
    private static int[] pixels(DoorButton button, boolean doorOpen) {
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        button.draw(g, doorOpen);
        g.dispose();
        return image.getRGB(0, 0, 200, 200, null, 0, 200);
    }

    @Test
    void aDropOnTheButtonPressesIt() {
        DoorButton button = new DoorButton(2, 100, 100, false);
        button.press(List.of(new Droplet(500, 500, 20), new Droplet(120, 100, 20)));
        assertTrue(button.isPressed());
        button.press(List.of(new Droplet(500, 500, 20)));
        assertFalse(button.isPressed(), "it springs back once the drop leaves");
        assertEquals(2, button.door());
        assertFalse(button.locks());
    }

    @Test
    void itShowsOnlyNearItself() {
        DoorButton button = new DoorButton(0, 100, 100, true);
        assertTrue(button.isVisibleIn(new Rectangle2D.Double(150, 150, 10, 10)));
        assertFalse(button.isVisibleIn(new Rectangle2D.Double(400, 400, 10, 10)));
    }

    @Test
    void itLightsUpWhenPressedOrWhenItsLockedDoorIsOpen() {
        DoorButton hold = new DoorButton(0, 100, 100, false);
        int[] idle = pixels(hold, false);
        hold.press(List.of(new Droplet(100, 100, 10)));
        assertFalse(Arrays.equals(idle, pixels(hold, false)));

        DoorButton lock = new DoorButton(0, 100, 100, true);
        assertFalse(Arrays.equals(pixels(lock, false), pixels(lock, true)));
        assertFalse(Arrays.equals(idle, pixels(lock, false)), "a lock button has a dot, a hold button a ring");
    }
}
