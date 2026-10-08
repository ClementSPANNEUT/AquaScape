package enemy;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.Random;

/**
 * Enemy that crosses the rooms in a straight line at a constant speed and bounces on the walls, on the
 * invisible barriers and on the other bouncers. It looks like a red neon ring with a turning dashed inner ring
 * and a white dot.
 */
public class Bouncer extends Enemy {
    private static final double RADIUS = 22;
    private static final Color COLOR = new Color(255, 70, 95);
    private static final double SPEED = 170;
    private static final float RING_WIDTH = 2.5f;
    private static final double INNER_RING = 0.5;
    private static final int DASHES = 4;
    private static final double DASH_SHARE = 0.6;
    private static final BasicStroke DASH_STROKE = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Color DASH_COLOR = new Color(255, 185, 195, 220);
    private static final double DOT = 0.16;
    private static final double SPIN = 90;

    private final Random random = new Random();
    private final double phase = 360 * random.nextDouble();

    /**
     * Creates a bouncer moving in a direction.
     * @param x centre x, in world px
     * @param y centre y, in world px
     * @param directionX x of the starting direction, of any length
     * @param directionY y of the starting direction
     */
    public Bouncer(double x, double y, double directionX, double directionY) {
        super(x, y, RADIUS, COLOR);
        double length = Math.hypot(directionX, directionY);
        vx = SPEED * directionX / length;
        vy = SPEED * directionY / length;
    }

    @Override
    public void update(double dt, Arena arena) {
        bounceOnBouncers(arena);
        move(dt, arena);
    }

    @Override
    protected boolean stopsAtBarriers() {
        return true;
    }

    private void bounceOnBouncers(Arena arena) {
        for (Enemy other : arena.enemies()) {
            if (other == this || !(other instanceof Bouncer) || !touches(other.x, other.y, other.radius())) {
                continue;
            }
            double dx = x - other.x;
            double dy = y - other.y;
            double distance = Math.hypot(dx, dy);
            if (distance == 0) {
                continue;
            }
            double normalX = dx / distance;
            double normalY = dy / distance;
            double overlap = radius() + other.radius() - distance;
            x += normalX * overlap;
            y += normalY * overlap;
            reflect(normalX, normalY);
            other.reflect(-normalX, -normalY);
        }
    }

    @Override
    public void draw(Graphics2D g, double time) {
        drawHalo(g);
        drawNeon(g, circle(radius()), RING_WIDTH);
        drawDashes(g, time);
        drawDot(g, x, y, DOT * radius());
    }

    private void drawDashes(Graphics2D g, double time) {
        double r = INNER_RING * radius();
        double step = 360.0 / DASHES;
        double start = phase + SPIN * time;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(DASH_COLOR);
        g2.setStroke(DASH_STROKE);
        for (int i = 0; i < DASHES; i++) {
            g2.draw(new Arc2D.Double(x - r, y - r, 2 * r, 2 * r, start + i * step, step * DASH_SHARE, Arc2D.OPEN));
        }
        g2.dispose();
    }

    private Ellipse2D circle(double r) {
        return new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r);
    }
}
