package screen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import world.Plan;

/**
 * Coordinate grid drawn over the world in the px of a zone's plan, the coordinates its code uses: the lines turn
 * and stretch with the plan, and every fifth line is labelled. A compass shows where the plan's x and y point.
 */
public final class Grid {
    private static final double MIN_SPACING = 50;
    private static final int[] STEPS = {1, 2, 5};
    private static final int MAJOR_EVERY = 5;
    private static final int LABEL_MARGIN = 60;
    private static final double ARROW_LENGTH = 34;
    private static final double ARROW_HEAD = 7;
    private static final Color MINOR_COLOR = new Color(130, 170, 230, 26);
    private static final Color MAJOR_COLOR = new Color(130, 180, 250, 75);
    private static final Color AXIS_COLOR = new Color(255, 205, 80, 190);
    private static final Color LABEL_COLOR = new Color(200, 215, 245, 190);
    private static final Color COMPASS_BACK = new Color(5, 8, 18, 200);
    private static final Font LABEL_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private static final Font COMPASS_FONT = new Font(Font.MONOSPACED, Font.BOLD, 13);
    private static final BasicStroke LINE = new BasicStroke(1f);
    private static final BasicStroke AXIS = new BasicStroke(2f);

    private Grid() {
    }

    /**
     * Draws the grid of a plan over the view.
     * @param g graphics drawing in the world
     * @param view the visible area, in world px
     * @param plan the plan whose px are shown
     */
    public static void draw(Graphics2D g, Rectangle view, Plan plan) {
        Graphics2D g2 = (Graphics2D) g.create();
        double step = step(plan.scale());
        double[] box = planBox(view, plan);
        long firstX = (long) Math.floor(box[0] / step);
        long lastX = (long) Math.ceil(box[2] / step);
        long firstY = (long) Math.floor(box[1] / step);
        long lastY = (long) Math.ceil(box[3] / step);
        for (long i = firstX; i <= lastX; i++) {
            style(g2, i == 0, i % MAJOR_EVERY == 0);
            line(g2, plan, i * step, box[1], i * step, box[3]);
        }
        for (long j = firstY; j <= lastY; j++) {
            style(g2, j == 0, j % MAJOR_EVERY == 0);
            line(g2, plan, box[0], j * step, box[2], j * step);
        }

        g2.setFont(LABEL_FONT);
        g2.setColor(LABEL_COLOR);
        Rectangle shown = new Rectangle(view.x - LABEL_MARGIN, view.y - LABEL_MARGIN,
                view.width + 2 * LABEL_MARGIN, view.height + 2 * LABEL_MARGIN);
        for (long i = Math.floorDiv(firstX, MAJOR_EVERY) * MAJOR_EVERY; i <= lastX; i += MAJOR_EVERY) {
            for (long j = Math.floorDiv(firstY, MAJOR_EVERY) * MAJOR_EVERY; j <= lastY; j += MAJOR_EVERY) {
                double x = plan.x(i * step, j * step);
                double y = plan.y(i * step, j * step);
                if (shown.contains(x, y)) {
                    g2.drawString(Math.round(i * step) + ", " + Math.round(j * step), (float) x + 4, (float) y - 4);
                }
            }
        }
        g2.dispose();
    }

    /**
     * Draws the name of the plan and two arrows showing where its x and y grow on screen.
     * @param g graphics drawing on the screen
     * @param plan the plan shown by the grid
     * @param centerX x of the compass centre, in screen px
     * @param centerY y of the compass centre, in screen px
     */
    public static void drawCompass(Graphics2D g, Plan plan, int centerX, int centerY) {
        Graphics2D g2 = (Graphics2D) g.create();
        int radius = (int) (ARROW_LENGTH + 18);
        g2.setColor(COMPASS_BACK);
        g2.fillOval(centerX - radius, centerY - radius, 2 * radius, 2 * radius);
        g2.setFont(COMPASS_FONT);
        g2.setColor(LABEL_COLOR);
        int nameWidth = g2.getFontMetrics().stringWidth(plan.name());
        g2.drawString(plan.name(), centerX - nameWidth / 2, centerY - radius - 6);
        g2.setColor(AXIS_COLOR);
        g2.setStroke(AXIS);
        arrow(g2, centerX, centerY, plan.direction(1, 0), "x");
        arrow(g2, centerX, centerY, plan.direction(0, 1), "y");
        g2.dispose();
    }

    private static void arrow(Graphics2D g, double x, double y, double[] direction, String label) {
        double tipX = x + direction[0] * ARROW_LENGTH;
        double tipY = y + direction[1] * ARROW_LENGTH;
        g.draw(new Line2D.Double(x, y, tipX, tipY));
        Path2D.Double head = new Path2D.Double();
        head.moveTo(tipX, tipY);
        head.lineTo(tipX - direction[0] * ARROW_HEAD - direction[1] * ARROW_HEAD / 2,
                tipY - direction[1] * ARROW_HEAD + direction[0] * ARROW_HEAD / 2);
        head.lineTo(tipX - direction[0] * ARROW_HEAD + direction[1] * ARROW_HEAD / 2,
                tipY - direction[1] * ARROW_HEAD - direction[0] * ARROW_HEAD / 2);
        head.closePath();
        g.fill(head);
        int width = g.getFontMetrics().stringWidth(label);
        g.drawString(label, (float) (tipX + direction[0] * 10 - width / 2.0), (float) (tipY + direction[1] * 10 + 5));
    }

    private static double step(double scale) {
        for (double decade = 1; ; decade *= 10) {
            for (int step : STEPS) {
                if (step * decade * scale >= MIN_SPACING) {
                    return step * decade;
                }
            }
        }
    }

    private static double[] planBox(Rectangle view, Plan plan) {
        double[] corners = {view.x, view.y, view.x + view.width, view.y, view.x, view.y + view.height,
            view.x + view.width, view.y + view.height};
        double[] box = {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (int i = 0; i < corners.length; i += 2) {
            double x = plan.planX(corners[i], corners[i + 1]);
            double y = plan.planY(corners[i], corners[i + 1]);
            box[0] = Math.min(box[0], x);
            box[1] = Math.min(box[1], y);
            box[2] = Math.max(box[2], x);
            box[3] = Math.max(box[3], y);
        }
        return box;
    }

    private static void line(Graphics2D g, Plan plan, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(plan.x(x1, y1), plan.y(x1, y1), plan.x(x2, y2), plan.y(x2, y2)));
    }

    private static void style(Graphics2D g, boolean axis, boolean major) {
        g.setStroke(axis ? AXIS : LINE);
        g.setColor(axis ? AXIS_COLOR : major ? MAJOR_COLOR : MINOR_COLOR);
    }
}
