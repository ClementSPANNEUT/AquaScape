package enemy;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;


/**
 * Base class of every enemy: a neon shape that moves in an {@link Arena} and kills the drops it touches.
 * <p>Subclasses decide how they move ({@link #update}) and look ({@link #draw}). This class gives them the
 * shared parts in the Template Method style: {@link #move} moves with sub-steps and bounces on the walls, and
 * asks the hook {@link #stopsAtBarriers()} whether the invisible barriers apply.
 */
public abstract class Enemy {
    private static final float[] GLOW_WIDTHS = {13f, 10f, 7.5f, 5f, 3f};
    private static final int[] GLOW_ALPHAS = {12, 20, 32, 55, 95};
    private static final double HALO = 2.4;
    private static final int HALO_ALPHA = 60;
    private static final Color DOT_COLOR = new Color(255, 242, 245);
    private static final Color DOT_GLOW = new Color(255, 200, 210, 70);
    private static final double NEAR_MARGIN = 20;

    /** Centre x, in world px. */
    protected double x;
    /** Centre y, in world px. */
    protected double y;
    /** Velocity along x, in px/s. */
    protected double vx;
    /** Velocity along y, in px/s. */
    protected double vy;
    private final double radius;
    private final Color[] glowColors = new Color[GLOW_WIDTHS.length];
    private final Color coreColor;
    private final Color[] haloColors;

    /**
     * Creates an enemy and prepares its neon colours.
     * @param x centre x, in world px
     * @param y centre y, in world px
     * @param radius size used for collisions and drawing, in px
     * @param color main neon colour; the glow, core and halo colours derive from it
     */
    protected Enemy(double x, double y, double radius, Color color) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        for (int i = 0; i < GLOW_WIDTHS.length; i++) {
            glowColors[i] = withAlpha(color, GLOW_ALPHAS[i]);
        }
        coreColor = new Color((3 * color.getRed() + 2 * 255) / 5, (3 * color.getGreen() + 2 * 255) / 5,
                (3 * color.getBlue() + 2 * 255) / 5);
        haloColors = new Color[] {withAlpha(color, HALO_ALPHA), withAlpha(color, 0)};
    }

    /** {@return the centre x, in world px} */
    public double x() {
        return x;
    }

    /** {@return the centre y, in world px} */
    public double y() {
        return y;
    }

    /** {@return the collision radius, in px} */
    public double radius() {
        return radius;
    }


    /**
     * Moves the enemy by one frame.
     * @param dt time since the last frame, in s
     * @param arena the world the enemy moves in
     */
    public abstract void update(double dt, Arena arena);

    /**
     * Draws the enemy.
     * @param g graphics already drawing in world coordinates
     * @param time game time, in s, for the animations
     */
    public abstract void draw(Graphics2D g, double time);


    /**
     * Tells whether a round object overlaps the enemy. The default shape is a disc of {@link #radius()};
     * subclasses with another shape override it.
     * @param otherX centre x of the object, in world px
     * @param otherY centre y of the object, in world px
     * @param otherRadius radius of the object, in px
     * @return {@code true} if they overlap
     */
    public boolean touches(double otherX, double otherY, double otherRadius) {
        double dx = otherX - x;
        double dy = otherY - y;
        double reach = radius + otherRadius;
        return dx * dx + dy * dy < reach * reach;
    }


    /**
     * Tells whether the enemy or its glow reaches into an area, so what is off screen is not drawn.
     * @param view area of the world, in world px
     * @return {@code true} if part of the enemy shows in it
     */
    public boolean isVisibleIn(Rectangle2D view) {
        double reach = Math.max(radius * HALO, radius + GLOW_WIDTHS[0]);
        return view.intersects(x - reach, y - reach, 2 * reach, 2 * reach);
    }

    /**
     * Moves along the current velocity. It moves in sub-steps of half a radius, so a fast enemy can't go
     * through a thin wall, and bounces on the edges of the world, on the obstacles and, when {@link
     * #stopsAtBarriers()} says so, on the barriers.
     * @param dt time to move for, in s
     * @param arena the world the enemy moves in
     */
    protected void move(double dt, Arena arena) {
        double travel = Math.hypot(vx, vy) * dt;
        double reach = radius + travel + NEAR_MARGIN;
        List<? extends Obstacle> obstacles = arena.obstaclesNear(x, y, reach);
        List<? extends Obstacle> barriers = stopsAtBarriers() ? arena.barriersNear(x, y, reach) : List.of();
        int steps = (int) Math.max(1, Math.ceil(travel / (radius / 2)));
        double step = dt / steps;
        for (int i = 0; i < steps; i++) {
            x += vx * step;
            y += vy * step;
            bounceOnEdges(arena.width(), arena.height());
            bounceOn(obstacles);
            bounceOn(barriers);
        }
    }

    /**
     * Hook of {@link #move}: tells whether the invisible barriers stop this enemy. Enemies that never leave
     * their path don't need them.
     * @return {@code false} by default
     */
    protected boolean stopsAtBarriers() {
        return false;
    }

    private void bounceOn(List<? extends Obstacle> obstacles) {
        for (Obstacle obstacle : obstacles) {
            if (obstacle.touches(x, y, radius)) {
                bounceOn(obstacle);
            }
        }
    }

    /**
     * Bounces the velocity off a surface: the part going into the surface is mirrored, so the speed is kept.
     * @param normalX x of the unit normal of the surface, pointing away from it
     * @param normalY y of the unit normal of the surface
     */
    protected void reflect(double normalX, double normalY) {
        double toward = vx * normalX + vy * normalY;
        if (toward < 0) {
            vx -= 2 * toward * normalX;
            vy -= 2 * toward * normalY;
        }
    }

    private void bounceOnEdges(int width, int height) {
        if (x < radius && vx < 0 || x > width - radius && vx > 0) {
            vx = -vx;
        }
        if (y < radius && vy < 0 || y > height - radius && vy > 0) {
            vy = -vy;
        }
        x = Math.max(radius, Math.min(width - radius, x));
        y = Math.max(radius, Math.min(height - radius, y));
    }

    private void bounceOn(Obstacle obstacle) {
        Point2D closest = obstacle.closestPoint(x, y);
        double dx = x - closest.getX();
        double dy = y - closest.getY();
        double distance = Math.hypot(dx, dy);
        if (distance == 0) {
            vx = -vx;
            vy = -vy;
            return;
        }
        double normalX = dx / distance;
        double normalY = dy / distance;
        double contact = radius + obstacle.halfThickness();
        x = closest.getX() + normalX * contact;
        y = closest.getY() + normalY * contact;
        reflect(normalX, normalY);
    }

    /**
     * Draws a soft radial halo around the enemy.
     * @param g graphics drawing in the world
     */
    protected void drawHalo(Graphics2D g) {
        double reach = radius * HALO;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) reach, new float[] {0f, 1f}, haloColors));
        g2.fill(new Ellipse2D.Double(x - reach, y - reach, 2 * reach, 2 * reach));
        g2.dispose();
    }

    /**
     * Draws a shape as a neon tube: wide faint strokes for the glow, then a bright core.
     * @param g graphics drawing in the world
     * @param shape outline to draw
     * @param width width of the core stroke, in px
     */
    protected void drawNeon(Graphics2D g, Shape shape, float width) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        for (int i = 0; i < GLOW_WIDTHS.length; i++) {
            g2.setColor(glowColors[i]);
            g2.setStroke(new BasicStroke(width + GLOW_WIDTHS[i], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(shape);
        }
        g2.setColor(coreColor);
        g2.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(shape);
        g2.dispose();
    }

    /**
     * Draws a small glowing white dot, the eye of the enemy.
     * @param g graphics drawing in the world
     * @param dotX centre x of the dot, in world px
     * @param dotY centre y of the dot, in world px
     * @param r radius of the dot, in px
     */
    protected void drawDot(Graphics2D g, double dotX, double dotY, double r) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(DOT_GLOW);
        g2.fill(new Ellipse2D.Double(dotX - 2.5 * r, dotY - 2.5 * r, 5 * r, 5 * r));
        g2.setColor(DOT_COLOR);
        g2.fill(new Ellipse2D.Double(dotX - r, dotY - r, 2 * r, 2 * r));
        g2.dispose();
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
