package screen;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;

/** Shared look of the menus: colours, fonts, background and buttons. */
public final class Ui {
    /** Colour behind every screen. */
    public static final Color BACKGROUND = new Color(3, 5, 12);
    private static final Color BACKGROUND_CENTER = new Color(8, 17, 32);
    private static final Color BACKGROUND_EDGE = new Color(2, 3, 8);
    private static BufferedImage background;
    /** Colour of the text. */
    public static final Color TEXT = new Color(225, 230, 245);
    /** Colour of the secondary text. */
    public static final Color MUTED_TEXT = new Color(140, 145, 165);
    /** Font of the game title. */
    public static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 56);
    /** Font of the screen titles. */
    public static final Font SUBTITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 28);
    /** Font of the normal text. */
    public static final Font TEXT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
    private static final Font BUTTON_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 17);
    private static final Color BUTTON_COLOR = new Color(40, 90, 160);
    private static final Color SELECTED_COLOR = new Color(35, 140, 100);
    private static final Color LOCKED_COLOR = new Color(55, 55, 68);
    private static final Dimension BUTTON_SIZE = new Dimension(460, 48);
    private static final String BASE_COLOR = "baseColor";
    private static final Border BUTTON_BORDER = BorderFactory.createEmptyBorder(10, 24, 10, 24);
    private static final Border HIGHLIGHT_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(TEXT, 2), BorderFactory.createEmptyBorder(8, 22, 8, 22));

    private Ui() {
    }

    /**
     * Paints the dark radial background, kept for the screen size.
     * @param g graphics drawing on the screen
     * @param width width of the screen, in px
     * @param height height of the screen, in px
     */
    public static void paintBackground(Graphics2D g, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (background == null || background.getWidth() != width || background.getHeight() != height) {
            background = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D bg = background.createGraphics();
            bg.setPaint(new RadialGradientPaint(new Point2D.Float(width / 2f, height / 2f),
                    Math.max(width, height) * 0.7f, new float[] {0f, 1f},
                    new Color[] {BACKGROUND_CENTER, BACKGROUND_EDGE}));
            bg.fillRect(0, 0, width, height);
            bg.dispose();
        }
        g.drawImage(background, 0, 0, null);
    }

    /**
     * Creates a centred label.
     * @param text its text
     * @param font its font
     * @param color its colour
     * @return the label
     */
    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    /**
     * Creates a menu button.
     * @param text its text
     * @param action run when it is clicked
     * @return the button
     */
    public static JButton button(String text, Runnable action) {
        return clickable(base(text, BUTTON_COLOR, TEXT), action);
    }

    /**
     * Creates a green button for the current choice.
     * @param text its text
     * @param action run when it is clicked
     * @return the button
     */
    public static JButton selectedButton(String text, Runnable action) {
        return clickable(base(text, SELECTED_COLOR, TEXT), action);
    }

    /**
     * Creates a greyed button that does nothing.
     * @param text its text
     * @return the button
     */
    public static JButton lockedButton(String text) {
        return base(text, LOCKED_COLOR, MUTED_TEXT);
    }

    /**
     * Creates a menu button that sets a level, such as the volume.
     * @param label its name
     * @param level the level it starts at, in percent
     * @param onChange told the new level each time it changes
     * @return the slider
     */
    public static Slider slider(String label, int level, IntConsumer onChange) {
        Slider slider = style(new Slider(label, level, onChange), BUTTON_COLOR, TEXT);
        slider.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return slider;
    }

    /**
     * Shows or hides the keyboard selection on a button.
     * @param button the button
     * @param highlighted whether it is selected
     */
    public static void highlight(JButton button, boolean highlighted) {
        Color color = (Color) button.getClientProperty(BASE_COLOR);
        button.setBackground(highlighted ? color.brighter() : color);
        button.setBorder(highlighted ? HIGHLIGHT_BORDER : BUTTON_BORDER);
    }

    private static JButton base(String text, Color color, Color textColor) {
        return style(new JButton(text), color, textColor);
    }

    private static <B extends JButton> B style(B button, Color color, Color textColor) {
        button.setUI(new BasicButtonUI());
        button.setFont(BUTTON_FONT);
        button.setForeground(textColor);
        button.setBackground(color);
        button.putClientProperty(BASE_COLOR, color);
        button.setFocusable(false);
        button.setBorder(BUTTON_BORDER);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setPreferredSize(BUTTON_SIZE);
        button.setMaximumSize(BUTTON_SIZE);
        return button;
    }

    private static JButton clickable(JButton button, Runnable action) {
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> action.run());
        return button;
    }
}
