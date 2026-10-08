package world;

import enemy.Obstacle;
import fluid.Droplet;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Neon wall: a glowing bar that kills the drops touching it, and that enemies can't go through. Its ends are in
 * world px.
 */
public class Wall implements Obstacle {
    private static final double HALF_THICKNESS = 3; // px, the white core the drops must not touch
    private static final double WAVE_AMPLITUDE = 2.5; // px, the bar is not perfectly straight
    private static final double WAVE_LENGTH = 280; // px
    private static final double WAVE_SPEED = 0.8; // rad/s
    private static final Color[] GLOW_COLORS = {
        new Color(60, 80, 255, 20),
        new Color(70, 90, 255, 34),
        new Color(80, 100, 255, 55),
        new Color(100, 120, 255, 95),
        new Color(160, 175, 255, 220),
        new Color(250, 250, 255),
    };
    private static final float[] GLOW_WIDTHS = {46f, 34f, 24f, 15f, 9f, 5f};

    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;

    /**
     * Creates a wall between two points.
     * @param x1 x of the first end, in world px
     * @param y1 y of the first end, in world px
     * @param x2 x of the second end, in world px
     * @param y2 y of the second end, in world px
     */
    public Wall(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    /**
     * Tells whether a drop touches the wall.
     * @param droplet the drop
     * @return {@code true} if it touches it
     */
    public boolean touches(Droplet droplet) {
        return touches(droplet.x(), droplet.y(), droplet.radius());
    }

    @Override
    public boolean touches(double x, double y, double radius) {
        double reach = radius + HALF_THICKNESS;
        if (x < Math.min(x1, x2) - reach || x > Math.max(x1, x2) + reach
                || y < Math.min(y1, y2) - reach || y > Math.max(y1, y2) + reach) {
            return false;
        }
        return distanceTo(x, y) < reach;
    }

    @Override
    public Point2D closestPoint(double px, double py) {
        double t = closestShare(px, py);
        return new Point2D.Double(x1 + t * (x2 - x1), y1 + t * (y2 - y1));
    }

    @Override
    public double halfThickness() {
        return HALF_THICKNESS;
    }

    /** {@return the smallest x of its ends} */
    public double minX() {
        return Math.min(x1, x2);
    }

    /** {@return the largest x of its ends} */
    public double maxX() {
        return Math.max(x1, x2);
    }

    /** {@return the smallest y of its ends} */
    public double minY() {
        return Math.min(y1, y2);
    }

    /** {@return the largest y of its ends} */
    public double maxY() {
        return Math.max(y1, y2);
    }

    /**
     * Tells whether its glow reaches into an area of the world.
     * @param view area of the world, in world px
     * @return {@code true} if it shows in that area
     */
    public boolean isVisibleIn(Rectangle2D view) {
        double margin = GLOW_WIDTHS[0] / 2 + WAVE_AMPLITUDE;
        return view.intersects(Math.min(x1, x2) - margin, Math.min(y1, y2) - margin,
                Math.abs(x2 - x1) + 2 * margin, Math.abs(y2 - y1) + 2 * margin);
    }

   
    private double distanceTo(double px, double py) {
        double t = closestShare(px, py);
        return Math.hypot(px - (x1 + t * (x2 - x1)), py - (y1 + t * (y2 - y1)));
    }


    private double closestShare(double px, double py) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;
        double t = lengthSquared == 0 ? 0 : ((px - x1) * dx + (py - y1) * dy) / lengthSquared;
        return Math.max(0, Math.min(1, t));
    }

    /**
     * Draws the wall: wide faint strokes for the glow, then a white core; the line waves slowly.
     * @param g graphics drawing in the world
     * @param time game time, in s, for the waves
     */
    public void draw(Graphics2D g, double time) {
        Path2D line = wavyLine(time);
        Graphics2D g2 = (Graphics2D) g.create();
        // no snapping to whole pixels: the gentle waves would turn into steps
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        for (int i = 0; i < GLOW_COLORS.length; i++) {
            g2.setColor(GLOW_COLORS[i]);
            g2.setStroke(new BasicStroke(GLOW_WIDTHS[i], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(line);
        }
        g2.dispose();
    }

    private Path2D wavyLine(double time) {
        double length = Math.hypot(x2 - x1, y2 - y1);
        double normalX = length == 0 ? 0 : -(y2 - y1) / length;
        double normalY = length == 0 ? 0 : (x2 - x1) / length;
        int points = Math.max(2, (int) (length / 4) + 1);
        Path2D path = new Path2D.Double();
        for (int i = 0; i < points; i++) {
            double t = i / (double) (points - 1);
            double wave = WAVE_AMPLITUDE * Math.sin(Math.PI * t)
                    * Math.sin(2 * Math.PI * t * length / WAVE_LENGTH + WAVE_SPEED * time);
            double px = x1 + (x2 - x1) * t + normalX * wave;
            double py = y1 + (y2 - y1) * t + normalY * wave;
            if (i == 0) {
                path.moveTo(px, py);
            } else {
                path.lineTo(px, py);
            }
        }
        return path;
    }
}
