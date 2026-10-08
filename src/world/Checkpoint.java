package world;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import fluid.Droplet;

/**
 * A checkpoint: the player restarts from the last one touched. It shows as a diamond, outlined until it is
 * reached, then filled.
 */
public class Checkpoint {
    private static final double REACH = 45;
    private static final double SIZE = 14;
    private static final BasicStroke OUTLINE = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Color IDLE_COLOR = new Color(235, 240, 250);
    private static final Color REACHED_COLOR = new Color(120, 240, 255);
    private static final Color REACHED_GLOW = new Color(120, 240, 255, 50);

    private final double x;
    private final double y;
    private final String area;
    private boolean reached;

    /**
     * Creates a checkpoint.
     * @param x centre x, in world px
     * @param y centre y, in world px
     * @param area name of the area it belongs to, for the checkpoint map
     */
    public Checkpoint(double x, double y, String area) {
        this.x = x;
        this.y = y;
        this.area = area;
    }

    /** {@return the name of its area} */
    public String area() {
        return area;
    }

    /** {@return the centre x, in world px} */
    public double x() {
        return x;
    }

    /** {@return the centre y, in world px} */
    public double y() {
        return y;
    }

    /** {@return whether it was reached} */
    public boolean isReached() {
        return reached;
    }

    /** Marks the checkpoint as reached. */
    public void reach() {
        reached = true;
    }

    /**
     * Tells whether a drop touches the checkpoint.
     * @param droplet the drop
     * @return {@code true} if it touches it
     */
    public boolean isTouchedBy(Droplet droplet) {
        return Math.hypot(droplet.x() - x, droplet.y() - y) < REACH + droplet.radius();
    }

    /**
     * Tells whether the checkpoint shows in an area.
     * @param view area of the world, in world px
     * @return {@code true} if it shows in that area
     */
    public boolean isVisibleIn(Rectangle2D view) {
        return view.intersects(x - 3 * SIZE, y - 3 * SIZE, 6 * SIZE, 6 * SIZE);
    }

    /**
     * Draws the diamond.
     * @param g graphics drawing in the world
     */
    public void draw(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        if (reached) {
            g2.setColor(REACHED_GLOW);
            g2.fill(diamond(2.2 * SIZE));
            g2.setColor(REACHED_COLOR);
            g2.fill(diamond(SIZE));
        } else {
            g2.setColor(IDLE_COLOR);
            g2.setStroke(OUTLINE);
            g2.draw(diamond(SIZE));
        }
        g2.dispose();
    }

    private Path2D diamond(double size) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x, y - size);
        path.lineTo(x + 0.62 * size, y);
        path.lineTo(x, y + size);
        path.lineTo(x - 0.62 * size, y);
        path.closePath();
        return path;
    }
}
