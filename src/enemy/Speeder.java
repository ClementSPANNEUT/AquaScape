package enemy;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.Random;

/**
 * Enemy that patrols between two stops: it waits 2 s, aims at the other stop, then dashes there at high speed.
 * It looks like a red neon triangle pointing where it will dash, and kills on that exact triangle.
 */
public class Speeder extends Enemy {
    private static final double RADIUS = 30;
    private static final Color COLOR = new Color(255, 70, 95);
    private static final double DASH_SPEED = 950;
    private static final double WAIT_TIME = 1;
    private static final double TIP = 1.0;
    private static final double BACK = 0.5;
    private static final double HALF_WIDTH = 0.45;
    private static final float OUTLINE_WIDTH = 2.5f;
    private static final Color DASH_FILL = new Color(255, 90, 120, 120);
    private static final int CHEVRONS = 2;
    private static final double CHEVRON_GAP = 0.35;
    private static final double CHEVRON_DEPTH = 0.2;
    private static final double CHEVRON_HALF_WIDTH = 0.3;
    private static final BasicStroke CHEVRON_STROKE = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final double AIM_GAP = 0.15;
    private static final double AIM_LENGTH = 0.6;
    private static final BasicStroke AIM_STROKE = new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Color AIM_COLOR = new Color(255, 120, 140, 70);
    private static final double DOT = 0.12;

    private final Random random = new Random();
    private final double[] stopsX;
    private final double[] stopsY;
    private int next = 1;
    private double directionX = 1;
    private double directionY;
    private double waitLeft;
    private double dashLeft;

    /**
     * Creates a speeder standing on its first stop.
     * @param x1 x of the first stop, in world px
     * @param y1 y of the first stop, in world px
     * @param x2 x of the second stop, in world px
     * @param y2 y of the second stop, in world px
     */
    public Speeder(double x1, double y1, double x2, double y2) {
        super(x1, y1, RADIUS, COLOR);
        stopsX = new double[] {x1, x2};
        stopsY = new double[] {y1, y2};
        aimAtNextStop();
        waitLeft = WAIT_TIME;
    }

    @Override
    public void update(double dt, Arena arena) {
        if (dashLeft <= 0) {
            waitLeft -= dt;
            if (waitLeft <= 0) {
                aimAtNextStop();
                dashLeft = Math.hypot(stopsX[next] - x, stopsY[next] - y);
            }
            return;
        }
        double time = Math.min(dt, dashLeft / DASH_SPEED);
        vx = directionX * DASH_SPEED;
        vy = directionY * DASH_SPEED;
        move(time, arena);
        vx = 0;
        vy = 0;
        dashLeft -= DASH_SPEED * time;
        if (dashLeft <= 1e-9) {
            dashLeft = 0;
            waitLeft = WAIT_TIME;
            next = 1 - next;
            aimAtNextStop();
        }
    }

    private void aimAtNextStop() {
        double dx = stopsX[next] - x;
        double dy = stopsY[next] - y;
        double length = Math.hypot(dx, dy);
        if (length > 0) {
            directionX = dx / length;
            directionY = dy / length;
        }
    }

    private boolean isDashing() {
        return dashLeft > 0;
    }

    @Override
    public boolean touches(double otherX, double otherY, double otherRadius) {
        double dx = otherX - x;
        double dy = otherY - y;
        double localX = dx * directionX + dy * directionY;
        double localY = dy * directionX - dx * directionY;
        Shape triangle = triangle();
        if (triangle.contains(localX, localY)) {
            return true;
        }
        double r = radius();
        double[] cornersX = {TIP * r, -BACK * r, -BACK * r};
        double[] cornersY = {0, -HALF_WIDTH * r, HALF_WIDTH * r};
        for (int i = 0; i < 3; i++) {
            int next = (i + 1) % 3;
            if (Line2D.ptSegDist(cornersX[i], cornersY[i], cornersX[next], cornersY[next], localX, localY) < otherRadius) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void draw(Graphics2D g, double time) {
        double charge = isDashing() ? 1 : 1 - waitLeft / WAIT_TIME;
        AffineTransform toWorld = AffineTransform.getTranslateInstance(x, y);
        toWorld.rotate(directionX, directionY);
        Shape body = toWorld.createTransformedShape(triangle());

        drawHalo(g);
        Graphics2D g2 = (Graphics2D) g.create();
        drawChevrons(g2, toWorld, charge);
        g2.setColor(AIM_COLOR);
        g2.setStroke(AIM_STROKE);
        g2.draw(toWorld.createTransformedShape(new Line2D.Double(
                (TIP + AIM_GAP) * radius(), 0, (TIP + AIM_GAP + AIM_LENGTH) * radius(), 0)));
        if (isDashing()) {
            g2.setColor(DASH_FILL);
            g2.fill(body);
        }
        g2.dispose();
        drawNeon(g, body, OUTLINE_WIDTH);
        drawDot(g, x, y, DOT * radius());
    }

    private Shape triangle() {
        double r = radius();
        Path2D.Double path = new Path2D.Double();
        path.moveTo(TIP * r, 0);
        path.lineTo(-BACK * r, -HALF_WIDTH * r);
        path.lineTo(-BACK * r, HALF_WIDTH * r);
        path.closePath();
        return path;
    }

    private void drawChevrons(Graphics2D g, AffineTransform toWorld, double charge) {
        double r = radius();
        g.setStroke(CHEVRON_STROKE);
        for (int i = 0; i < CHEVRONS; i++) {
            double tipX = -(BACK + CHEVRON_GAP * (i + 1)) * r;
            Path2D.Double chevron = new Path2D.Double();
            chevron.moveTo(tipX - CHEVRON_DEPTH * r, -CHEVRON_HALF_WIDTH * r);
            chevron.lineTo(tipX, 0);
            chevron.lineTo(tipX - CHEVRON_DEPTH * r, CHEVRON_HALF_WIDTH * r);
            int alpha = (int) ((40 + 170 * charge) / (i + 1));
            g.setColor(new Color(COLOR.getRed(), COLOR.getGreen(), COLOR.getBlue(), alpha));
            g.draw(toWorld.createTransformedShape(chevron));
        }
    }
}
