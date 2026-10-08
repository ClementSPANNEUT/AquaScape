package world;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import enemy.ability.Target;
import fluid.Droplet;

/**
 * A button that opens a door while a drop is on it, or when a ball hits it. A hold button keeps the door open only
 * while it is pressed; a lock button opens it for good.
 */
public class DoorButton implements Target {
    private static final double RADIUS = 34;
    private static final double LOCK_DOT = 0.35;
    private static final BasicStroke RING_STROKE = new BasicStroke(3f);
    private static final Color RING_COLOR = new Color(190, 165, 255);
    private static final Color IDLE_FILL = new Color(80, 60, 150, 170);
    private static final Color PRESSED_FILL = new Color(170, 140, 255, 220);
    private static final Color GLOW = new Color(170, 140, 255, 60);
    private static final Color DOT_COLOR = new Color(240, 235, 255);

    private final int door;
    private final double x;
    private final double y;
    private final boolean locks;
    private boolean pressed;
    private boolean struck;

    /**
     * Creates a button.
     * @param door index of the door it opens
     * @param x centre x, in world px
     * @param y centre y, in world px
     * @param locks whether it locks the door open
     */
    public DoorButton(int door, double x, double y, boolean locks) {
        this.door = door;
        this.x = x;
        this.y = y;
        this.locks = locks;
    }

    /** {@return the index of the door it opens} */
    public int door() {
        return door;
    }

    /** {@return whether it locks the door open} */
    public boolean locks() {
        return locks;
    }

    /** {@return whether a drop is on it} */
    public boolean isPressed() {
        return pressed;
    }

    /**
     * Tells whether a round object lies on the button.
     * @param otherX centre x of the object, in world px
     * @param otherY centre y of the object, in world px
     * @param otherRadius radius of the object, in px
     * @return {@code true} if they overlap
     */
    @Override
    public boolean isUnder(double otherX, double otherY, double otherRadius) {
        return Math.hypot(otherX - x, otherY - y) < RADIUS + otherRadius;
    }

    /** Hits the button, as a ball flying over it does: it counts as pressed at the next {@link #press}. */
    @Override
    public void strike() {
        struck = true;
    }

    /**
     * Checks whether the button is pressed: a drop is on it, by its head or, while it is anchored, by its back
     * stuck to the ground, or a ball has just {@linkplain #strike() hit} it.
     * @param droplets the drops of the world
     */
    public void press(List<Droplet> droplets) {
        pressed = struck;
        struck = false;
        if (pressed) {
            return;
        }
        for (Droplet droplet : droplets) {
            if (droplet.isWithin(x, y, RADIUS)) {
                pressed = true;
                return;
            }
        }
    }

    /**
     * Tells whether the button shows in an area.
     * @param view area of the world, in world px
     * @return {@code true} if it shows in that area
     */
    public boolean isVisibleIn(Rectangle2D view) {
        return view.intersects(x - 2 * RADIUS, y - 2 * RADIUS, 4 * RADIUS, 4 * RADIUS);
    }

    /**
     * Draws the button.
     * @param g graphics drawing in the world
     * @param doorOpen whether its door is open, to light it up
     */
    public void draw(Graphics2D g, boolean doorOpen) {
        Graphics2D g2 = (Graphics2D) g.create();
        if (pressed || locks && doorOpen) {
            g2.setColor(GLOW);
            g2.fill(circle(1.6 * RADIUS));
        }
        g2.setColor(pressed || locks && doorOpen ? PRESSED_FILL : IDLE_FILL);
        g2.fill(circle(RADIUS));
        g2.setColor(RING_COLOR);
        g2.setStroke(RING_STROKE);
        g2.draw(circle(RADIUS));
        if (locks) {
            g2.setColor(DOT_COLOR);
            g2.fill(circle(LOCK_DOT * RADIUS));
        } else {
            g2.draw(circle(0.55 * RADIUS));
        }
        g2.dispose();
    }

    private Ellipse2D circle(double r) {
        return new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r);
    }
}
