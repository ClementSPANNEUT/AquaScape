package screen;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.function.IntConsumer;
import javax.swing.JButton;

/**
 * A menu button that sets a level from 0 to 100 %, drawn as a row of ten segments between its name and its value.
 * A click or a drag on the segments picks the level under the mouse; the menu moves it one segment at a time with
 * the arrows or the stick.
 * <p>The pause of the game draws its own buttons, so the rules of a slider are also given on their own: how it is
 * drawn in a box ({@link #draw}), the level under the mouse ({@link #levelAt}) and the level one segment further
 * ({@link #moved}).
 *
 * @serial exclude
 */
public class Slider extends JButton {
    /** How much one segment is worth, in percent. */
    public static final int STEP = 10;
    /** The highest level, in percent. */
    public static final int MAX = 100;
    private static final int SEGMENTS = MAX / STEP;
    private static final int SIDE = 24;
    private static final int BAR_LEFT = 150;
    private static final int VALUE_SPACE = 100;
    private static final int SEGMENT_GAP = 4;
    private static final int SEGMENT_HEIGHT = 16;
    private static final int SEGMENT_ROUNDING = 4;
    private static final int REACH = 12;
    private static final Color EMPTY = new Color(20, 45, 85);
    private static final String DESKTOP_HINTS = "awt.font.desktophints";

    private final String label;
    private final IntConsumer onChange;
    private int level;

    /**
     * Creates a slider.
     * @param label its name, drawn on its left
     * @param level the level it starts at, in percent
     * @param onChange told the new level each time it changes
     */
    public Slider(String label, int level, IntConsumer onChange) {
        this.label = label;
        this.onChange = onChange;
        this.level = Math.max(0, Math.min(MAX, level));
        setText(text());
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pick(e.getX());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                pick(e.getX());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /**
     * Gives the level reached by moving whole segments, stopping at 0 and at 100 %.
     * @param level the level to start from, in percent
     * @param steps how many segments, negative to go down
     * @return the new level, in percent
     */
    public static int moved(int level, int steps) {
        int from = steps > 0 ? level / STEP * STEP : (level + STEP - 1) / STEP * STEP;
        return Math.max(0, Math.min(MAX, from + steps * STEP));
    }

    /**
     * Finds the level under the mouse on a slider drawn in a box.
     * @param box the box of the slider
     * @param x x of the mouse, in the coordinates of the box
     * @return the level, in percent, or -1 when the mouse is beside the segments
     */
    public static int levelAt(Rectangle box, int x) {
        int left = box.x + BAR_LEFT;
        int width = barWidth(box.width);
        if (x < left - REACH || x > left + width + REACH) {
            return -1;
        }
        return Math.max(0, Math.min(MAX, (int) Math.round((x - left) / (double) width * SEGMENTS) * STEP));
    }

    /**
     * Draws a slider in a box, with the font and the colour of the graphics: its name on the left, its segments,
     * lit up to the level, and its value on the right.
     * @param g graphics drawing on the screen
     * @param box the box to draw in
     * @param label the name of the slider
     * @param level its level, in percent
     */
    public static void draw(Graphics2D g, Rectangle box, String label, int level) {
        Color lit = g.getColor();
        FontMetrics metrics = g.getFontMetrics();
        int baseline = box.y + (box.height - metrics.getHeight()) / 2 + metrics.getAscent();
        String value = level + " %";
        g.drawString(label, box.x + SIDE, baseline);
        g.drawString(value, box.x + box.width - SIDE - metrics.stringWidth(value), baseline);

        int pitch = barWidth(box.width) / SEGMENTS;
        int top = box.y + (box.height - SEGMENT_HEIGHT) / 2;
        for (int segment = 0; segment < SEGMENTS; segment++) {
            g.setColor(segment < level / STEP ? lit : EMPTY);
            g.fillRoundRect(box.x + BAR_LEFT + segment * pitch, top, pitch - SEGMENT_GAP, SEGMENT_HEIGHT,
                    SEGMENT_ROUNDING, SEGMENT_ROUNDING);
        }
        g.setColor(lit);
    }

    private static int barWidth(int width) {
        return Math.max(SEGMENTS, width - BAR_LEFT - VALUE_SPACE);
    }

    /** {@return the level, in percent} */
    public int level() {
        return level;
    }

    /**
     * Moves the level by whole segments, stopping at 0 and at 100 %.
     * @param steps how many segments, negative to go down
     */
    public void move(int steps) {
        setLevel(moved(level, steps));
    }

    private void pick(int x) {
        int picked = levelAt(new Rectangle(getWidth(), getHeight()), x);
        if (picked >= 0) {
            setLevel(picked);
        }
    }

    private void setLevel(int next) {
        if (next == level) {
            return;
        }
        level = next;
        setText(text());
        repaint();
        onChange.accept(level);
    }

    private String text() {
        return label + " : " + level + " %";
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (Toolkit.getDefaultToolkit().getDesktopProperty(DESKTOP_HINTS) instanceof Map<?, ?> hints) {
            g2.addRenderingHints(hints);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(getForeground());
        g2.setFont(getFont());
        draw(g2, new Rectangle(getWidth(), getHeight()), label, level);
        g2.dispose();
    }
}
