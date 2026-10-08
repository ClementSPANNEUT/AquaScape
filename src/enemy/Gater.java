package enemy;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

/**
 * Enemy that closes a corridor in rhythm: two jaws come out of the walls, stay open for 1.6 s, close in 0.35 s,
 * stay shut for 0.9 s and open again in 0.45 s. Each gater starts at a random moment of its cycle.
 */
public class Gater extends Enemy {
    private static final Color COLOR = new Color(255, 70, 95);
    private static final double DEPTH = 44;
    private static final double OPEN_SHARE = 0.14;
    private static final double CLOSED_SHARE = 0.5;
    private static final double OPEN_TIME = 1.6;
    private static final double CLOSING_TIME = 0.35;
    private static final double CLOSED_TIME = 0.9;
    private static final double OPENING_TIME = 0.45;
    private static final double PERIOD = OPEN_TIME + CLOSING_TIME + CLOSED_TIME + OPENING_TIME;
    private static final float OUTLINE_WIDTH = 2.5f;
    private static final Color FILL = new Color(255, 90, 120, 90);
    private static final Color RAIL_COLOR = new Color(255, 90, 120, 50);
    private static final BasicStroke RAIL_STROKE = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
            10f, new float[] {5f, 8f}, 0f);

    private final double startX;
    private final double startY;
    private final double alongX;
    private final double alongY;
    private final double length;
    private double clock;

    /**
     * Creates a gater across a corridor; the jaws come out of both ends of the segment.
     * @param x1 x of the first wall end, in world px
     * @param y1 y of the first wall end, in world px
     * @param x2 x of the second wall end, in world px
     * @param y2 y of the second wall end, in world px
     */
    public Gater(double x1, double y1, double x2, double y2) {
        super((x1 + x2) / 2, (y1 + y2) / 2, Math.hypot(x2 - x1, y2 - y1) / 2, COLOR);
        startX = x1;
        startY = y1;
        length = Math.hypot(x2 - x1, y2 - y1);
        alongX = (x2 - x1) / length;
        alongY = (y2 - y1) / length;
        clock = PERIOD * new Random().nextDouble();
    }

    @Override
    public void update(double dt, Arena arena) {
        clock = (clock + dt) % PERIOD;
    }

    private double reach() {
        double t = clock;
        if (t < OPEN_TIME) {
            return OPEN_SHARE * length;
        }
        t -= OPEN_TIME;
        if (t < CLOSING_TIME) {
            return (OPEN_SHARE + (CLOSED_SHARE - OPEN_SHARE) * t / CLOSING_TIME) * length;
        }
        t -= CLOSING_TIME;
        if (t < CLOSED_TIME) {
            return CLOSED_SHARE * length;
        }
        t -= CLOSED_TIME;
        return (CLOSED_SHARE - (CLOSED_SHARE - OPEN_SHARE) * t / OPENING_TIME) * length;
    }

    @Override
    public boolean touches(double otherX, double otherY, double otherRadius) {
        double dx = otherX - startX;
        double dy = otherY - startY;
        double along = dx * alongX + dy * alongY;
        double across = Math.abs(dy * alongX - dx * alongY);
        double reach = reach();
        return distanceToJaw(along, across, 0, reach) < otherRadius
                || distanceToJaw(along, across, length - reach, length) < otherRadius;
    }

    private static double distanceToJaw(double along, double across, double from, double to) {
        double outsideAlong = Math.max(0, Math.max(from - along, along - to));
        double outsideAcross = Math.max(0, across - DEPTH / 2);
        return Math.hypot(outsideAlong, outsideAcross);
    }

    @Override
    public void draw(Graphics2D g, double time) {
        AffineTransform toWorld = AffineTransform.getTranslateInstance(startX, startY);
        toWorld.rotate(alongX, alongY);
        double reach = reach();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(RAIL_COLOR);
        g2.setStroke(RAIL_STROKE);
        g2.draw(toWorld.createTransformedShape(new Line2D.Double(0, 0, length, 0)));
        Shape first = toWorld.createTransformedShape(jaw(0, reach));
        Shape second = toWorld.createTransformedShape(jaw(length - reach, reach));
        g2.setColor(FILL);
        g2.fill(first);
        g2.fill(second);
        g2.dispose();
        drawNeon(g, first, OUTLINE_WIDTH);
        drawNeon(g, second, OUTLINE_WIDTH);
    }

    private static Shape jaw(double from, double size) {
        return new RoundRectangle2D.Double(from, -DEPTH / 2, size, DEPTH, 14, 14);
    }
}
