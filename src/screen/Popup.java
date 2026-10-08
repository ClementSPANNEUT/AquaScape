package screen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Draws the solid pop-up windows of the game: a dimmed background, a window with a shadow, a title bar and a
 * neon border, and small banners for short messages.
 */
public final class Popup {
    /** Height of the title bar, in px. */
    public static final int HEADER_HEIGHT = 56;
    private static final int RADIUS = 22;
    private static final int SHADOW_OFFSET = 8;
    private static final Color DIM = new Color(2, 4, 10, 220);
    private static final Color SHADOW = new Color(0, 0, 0, 160);
    private static final Color BODY = new Color(14, 22, 40);
    private static final Color HEADER = new Color(26, 56, 98);
    private static final Color BORDER = new Color(110, 230, 250);
    private static final Color TITLE_COLOR = Color.WHITE;
    private static final BasicStroke BORDER_STROKE = new BasicStroke(2.5f);
    private static final BasicStroke HEADER_STROKE = new BasicStroke(1.5f);
    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 24);
    private static final int TOAST_PADDING = 18;
    private static final int TOAST_HEIGHT = 40;

    private Popup() {
    }

    /**
     * Darkens the whole screen behind a pop-up.
     * @param g graphics drawing on the screen
     * @param width width of the screen, in px
     * @param height height of the screen, in px
     */
    public static void dim(Graphics2D g, int width, int height) {
        g.setColor(DIM);
        g.fillRect(0, 0, width, height);
    }

    /**
     * Centres a box in an area.
     * @param width width of the box, in px
     * @param height height of the box, in px
     * @param areaWidth width of the area, in px
     * @param areaHeight height of the area, in px
     * @return the box
     */
    public static Rectangle centered(int width, int height, int areaWidth, int areaHeight) {
        return new Rectangle((areaWidth - width) / 2, Math.max(8, (areaHeight - height) / 2), width, height);
    }

    /**
     * Draws the solid window with its title bar.
     * @param g graphics drawing on the screen
     * @param box where the window goes
     * @param title text of the title bar
     * @return the area under the title bar, for the content
     */
    public static Rectangle draw(Graphics2D g, Rectangle box, String title) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D shape = new RoundRectangle2D.Double(box.x, box.y, box.width, box.height, RADIUS, RADIUS);
        g2.setColor(SHADOW);
        g2.fill(new RoundRectangle2D.Double(box.x + SHADOW_OFFSET, box.y + SHADOW_OFFSET, box.width, box.height,
                RADIUS, RADIUS));
        g2.setColor(BODY);
        g2.fill(shape);
        g2.clip(shape);
        g2.setColor(HEADER);
        g2.fillRect(box.x, box.y, box.width, HEADER_HEIGHT);
        g2.setClip(null);
        g2.setColor(BORDER);
        g2.setStroke(HEADER_STROKE);
        g2.draw(new Line2D.Double(box.x, box.y + HEADER_HEIGHT, box.x + box.width, box.y + HEADER_HEIGHT));
        g2.setStroke(BORDER_STROKE);
        g2.draw(shape);
        g2.setColor(TITLE_COLOR);
        g2.setFont(TITLE_FONT);
        int textWidth = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, box.x + (box.width - textWidth) / 2, box.y + HEADER_HEIGHT / 2 + 9);
        g2.dispose();
        return new Rectangle(box.x, box.y + HEADER_HEIGHT, box.width, box.height - HEADER_HEIGHT);
    }

    /**
     * Draws a short message in a rounded banner.
     * @param g graphics drawing on the screen
     * @param text the message
     * @param font font of the message
     * @param color colour of the border and the text
     * @param areaWidth width of the screen, to centre the banner
     * @param top top of the banner, in px
     */
    public static void toast(Graphics2D g, String text, Font font, Color color, int areaWidth, int top) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(font);
        int width = g2.getFontMetrics().stringWidth(text) + 2 * TOAST_PADDING;
        RoundRectangle2D shape = new RoundRectangle2D.Double((areaWidth - width) / 2.0, top, width, TOAST_HEIGHT,
                TOAST_HEIGHT, TOAST_HEIGHT);
        g2.setColor(SHADOW);
        g2.fill(new RoundRectangle2D.Double(shape.getX() + 4, top + 5, width, TOAST_HEIGHT, TOAST_HEIGHT, TOAST_HEIGHT));
        g2.setColor(BODY);
        g2.fill(shape);
        g2.setColor(color);
        g2.setStroke(BORDER_STROKE);
        g2.draw(shape);
        g2.drawString(text, (float) shape.getX() + TOAST_PADDING, top + TOAST_HEIGHT / 2f + 6);
        g2.dispose();
    }
}
