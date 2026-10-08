package world;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import fluid.Droplet;

/**
 * A light to reach. The lights of the zones are brought back to the hub; the last one, golden, wins the level.
 */
public class Light {
    private static final double REACH = 40;
    private static final double HALO = 150;
    private static final double SIZE = 18;
    private static final double RING = 42;
    private static final int DASHES = 8;
    private static final double SPIN = 30;
    private static final Color CYAN = new Color(110, 236, 252);
    private static final Color GOLD = new Color(255, 205, 95);
    private static final Color CYAN_CORE = new Color(235, 253, 255);
    private static final Color GOLD_CORE = new Color(255, 248, 225);
    private static final Color OBTAINED_COLOR = new Color(150, 170, 190, 120);
    private static final BasicStroke RING_STROKE = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private final String name;
    private final double x;
    private final double y;
    private final boolean last;
    private final Color[] haloColors;
    private final Color core;
    private final Color glow;
    private final Color ringColor;

    /**
     * Creates a light.
     * @param name name of the light, shown to the player
     * @param x centre x, in world px
     * @param y centre y, in world px
     * @param last whether reaching it wins the level
     */
    public Light(String name, double x, double y, boolean last) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.last = last;
        Color color = last ? GOLD : CYAN;
        haloColors = new Color[] {withAlpha(color, 90), withAlpha(color, 0)};
        core = last ? GOLD_CORE : CYAN_CORE;
        glow = withAlpha(color, 90);
        ringColor = withAlpha(color, 200);
    }

    /** {@return the name of the light} */
    public String name() {
        return name;
    }

    /** {@return the centre x, in world px} */
    public double x() {
        return x;
    }

    /** {@return the centre y, in world px} */
    public double y() {
        return y;
    }

    /** {@return whether reaching it wins the level} */
    public boolean isLast() {
        return last;
    }

    /**
     * Tells whether a drop touches the light.
     * @param droplet the drop
     * @return {@code true} if it touches it
     */
    public boolean isTouchedBy(Droplet droplet) {
        return Math.hypot(droplet.x() - x, droplet.y() - y) < REACH + droplet.radius();
    }

    /**
     * Tells whether the light or its halo shows in an area.
     * @param view area of the world, in world px
     * @return {@code true} if it shows in that area
     */
    public boolean isVisibleIn(Rectangle2D view) {
        return view.intersects(x - HALO, y - HALO, 2 * HALO, 2 * HALO);
    }

    /**
     * Draws the light, pulsing, or dimmed once obtained.
     * @param g graphics drawing in the world
     * @param time game time, in s, for the animation
     * @param obtained whether it was obtained
     */
    public void draw(Graphics2D g, double time, boolean obtained) {
        Graphics2D g2 = (Graphics2D) g.create();
        if (obtained) {
            g2.setColor(OBTAINED_COLOR);
            g2.setStroke(RING_STROKE);
            g2.draw(new Ellipse2D.Double(x - RING, y - RING, 2 * RING, 2 * RING));
            g2.fill(diamond(SIZE));
            g2.dispose();
            return;
        }
        double pulse = 1 + 0.08 * Math.sin(3 * time);
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) (HALO * pulse), new float[] {0f, 1f},
                haloColors));
        g2.fill(new Ellipse2D.Double(x - HALO * pulse, y - HALO * pulse, 2 * HALO * pulse, 2 * HALO * pulse));

        g2.setColor(ringColor);
        g2.setStroke(RING_STROKE);
        double step = 360.0 / DASHES;
        for (int i = 0; i < DASHES; i++) {
            g2.draw(new Arc2D.Double(x - RING, y - RING, 2 * RING, 2 * RING, SPIN * time + i * step, step * 0.55,
                    Arc2D.OPEN));
        }

        g2.setColor(glow);
        g2.fill(diamond(1.8 * SIZE * pulse));
        g2.setColor(core);
        g2.fill(diamond(SIZE));
        g2.dispose();
    }

    private Path2D diamond(double size) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x, y - size);
        path.lineTo(x + 0.7 * size, y);
        path.lineTo(x, y + size);
        path.lineTo(x - 0.7 * size, y);
        path.closePath();
        return path;
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
