package enemy;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
 * Enemy shaped like a bar that turns around its centre at a constant speed. A dashed circle shows the area it
 * sweeps.
 */
public class Spinner extends Enemy {
    private static final Color COLOR = new Color(255, 70, 95);
    private static final float BAR_WIDTH = 6f;
    private static final double HALF_THICKNESS = 6;
    private static final double DOT = 5;
    private static final Color SWEEP_COLOR = new Color(255, 90, 120, 45);
    private static final BasicStroke SWEEP_STROKE = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
            10f, new float[] {6f, 9f}, 0f);

    private final double halfLength;
    private final double speed;
    private double angle;

    /**
     * Creates a spinner.
     * @param x centre x, in world px
     * @param y centre y, in world px
     * @param halfLength length of each arm of the bar, in px
     * @param angleDegrees starting angle of the bar, in degrees
     * @param degreesPerSecond turning speed; a negative speed turns the other way
     */
    public Spinner(double x, double y, double halfLength, double angleDegrees, double degreesPerSecond) {
        super(x, y, halfLength, COLOR);
        this.halfLength = halfLength;
        this.angle = Math.toRadians(angleDegrees);
        this.speed = Math.toRadians(degreesPerSecond);
    }

    @Override
    public void update(double dt, Arena arena) {
        angle += speed * dt;
    }

    @Override
    public boolean touches(double otherX, double otherY, double otherRadius) {
        double cos = Math.cos(angle) * halfLength;
        double sin = Math.sin(angle) * halfLength;
        return Line2D.ptSegDist(x - cos, y - sin, x + cos, y + sin, otherX, otherY) < otherRadius + HALF_THICKNESS;
    }

    @Override
    public void draw(Graphics2D g, double time) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(SWEEP_COLOR);
        g2.setStroke(SWEEP_STROKE);
        g2.draw(new Ellipse2D.Double(x - halfLength, y - halfLength, 2 * halfLength, 2 * halfLength));
        g2.dispose();
        double cos = Math.cos(angle) * halfLength;
        double sin = Math.sin(angle) * halfLength;
        drawNeon(g, new Line2D.Double(x - cos, y - sin, x + cos, y + sin), BAR_WIDTH);
        drawDot(g, x, y, DOT);
    }
}
