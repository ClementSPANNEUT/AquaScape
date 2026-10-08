package world;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * A door across a corridor: deadly and solid while closed, a dotted line once open. The golden gate opens with
 * the lights instead of a button.
 */
public class Door {
    private static final float[] GLOW_WIDTHS = {30f, 20f, 12f, 7f};
    private static final int[] GLOW_ALPHAS = {18, 34, 70, 150};
    private static final float CORE_WIDTH = 4f;
    private static final Color DOOR_COLOR = new Color(165, 130, 255);
    private static final Color GATE_COLOR = new Color(255, 200, 90);
    private static final BasicStroke OPEN_STROKE = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
            10f, new float[] {6f, 10f}, 0f);

    private final Wall wall;
    private final Line2D.Double line;
    private final boolean gate;
    private final Color[] glowColors = new Color[GLOW_WIDTHS.length];
    private final Color coreColor;
    private final Color openColor;
    private boolean open;

    /**
     * Creates a closed door.
     * @param x1 x of the first end, in world px
     * @param y1 y of the first end, in world px
     * @param x2 x of the second end, in world px
     * @param y2 y of the second end, in world px
     * @param gate whether it is the golden gate
     */
    public Door(double x1, double y1, double x2, double y2, boolean gate) {
        this.wall = new Wall(x1, y1, x2, y2);
        this.line = new Line2D.Double(x1, y1, x2, y2);
        this.gate = gate;
        Color color = gate ? GATE_COLOR : DOOR_COLOR;
        for (int i = 0; i < GLOW_WIDTHS.length; i++) {
            glowColors[i] = new Color(color.getRed(), color.getGreen(), color.getBlue(), GLOW_ALPHAS[i]);
        }
        coreColor = new Color((color.getRed() + 255) / 2, (color.getGreen() + 255) / 2, (color.getBlue() + 255) / 2);
        openColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 90);
    }

    /** {@return the wall the door makes while closed} */
    public Wall wall() {
        return wall;
    }

    /** {@return whether it is the golden gate} */
    public boolean isGate() {
        return gate;
    }

    /** {@return whether it is open} */
    public boolean isOpen() {
        return open;
    }

    /**
     * Opens or closes the door.
     * @param open whether it is open
     */
    public void setOpen(boolean open) {
        this.open = open;
    }
    /**
     * Tells whether the door or its glow shows in an area.
     * @param view area of the world, in world px
     * @return {@code true} if it shows in that area
     */
    public boolean isVisibleIn(Rectangle2D view) {
        double margin = GLOW_WIDTHS[0];
        return view.intersects(wall.minX() - margin, wall.minY() - margin,
                wall.maxX() - wall.minX() + 2 * margin, wall.maxY() - wall.minY() + 2 * margin);
    }

    /**
     * Draws the door.
     * @param g graphics drawing in the world
     * @param time game time, in s, for the pulse
     */
    public void draw(Graphics2D g, double time) {
        Graphics2D g2 = (Graphics2D) g.create();
        if (open) {
            g2.setColor(openColor);
            g2.setStroke(OPEN_STROKE);
            g2.draw(line);
        } else {
            float pulse = (float) (1 + 0.15 * Math.sin(4 * time));
            for (int i = 0; i < GLOW_WIDTHS.length; i++) {
                g2.setColor(glowColors[i]);
                g2.setStroke(new BasicStroke(GLOW_WIDTHS[i] * pulse, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(line);
            }
            g2.setColor(coreColor);
            g2.setStroke(new BasicStroke(CORE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(line);
        }
        g2.dispose();
    }
}
