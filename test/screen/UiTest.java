package screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;

class UiTest {
    private static int brightness(int rgb) {
        Color color = new Color(rgb);
        return color.getRed() + color.getGreen() + color.getBlue();
    }

    @Test
    void theBackgroundIsLighterInTheMiddle() {
        BufferedImage image = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        Ui.paintBackground(g, 0, 0);
        assertEquals(0, image.getRGB(150, 100) & 0xFFFFFF, "nothing to paint on an empty screen");
        Ui.paintBackground(g, 300, 200);
        Ui.paintBackground(g, 300, 200);
        g.dispose();
        assertTrue(brightness(image.getRGB(150, 100)) > brightness(image.getRGB(0, 0)));
    }

    @Test
    void aButtonRunsItsActionWhenClicked() {
        int[] clicks = new int[1];
        JButton button = Ui.button("Jouer", () -> clicks[0]++);
        button.doClick(0);
        assertEquals(1, clicks[0]);
        assertEquals("Jouer", button.getText());
    }

    @Test
    void theHighlightLightensTheButtonThenGoesAway() {
        JButton button = Ui.selectedButton("HD", () -> {
        });
        Color base = button.getBackground();
        Ui.highlight(button, true);
        assertNotEquals(base, button.getBackground());
        Ui.highlight(button, false);
        assertEquals(base, button.getBackground());
        assertNotEquals(Ui.button("x", () -> {
        }).getBackground(), base, "the current choice is green");
    }

    @Test
    void aLockedButtonDoesNothing() {
        JButton button = Ui.lockedButton("Bientôt");
        assertEquals(0, button.getActionListeners().length);
        assertEquals(Ui.MUTED_TEXT, button.getForeground());
    }

    @Test
    void aLabelHasItsFontAndColour() {
        JLabel label = Ui.label("Aquascape", Ui.TITLE_FONT, Ui.TEXT);
        assertEquals("Aquascape", label.getText());
        assertEquals(Ui.TITLE_FONT, label.getFont());
        assertEquals(Ui.TEXT, label.getForeground());
    }
}
