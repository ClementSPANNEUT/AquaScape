package enemy.ability;

import enemy.Arena;
import enemy.Enemy;
import enemy.Obstacle;
import fluid.FluidRenderer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * A ball fired by the {@link Shoot} ability: it flies straight, slows down and dies out, or stops on the first
 * wall, closed door or enemy. The stronger the shot, the faster and the further it goes. It hits the
 * {@link Target targets} it flies over.
 */
public class Shot {
    /** Radius of the ball, in px: small enough to go through a narrow slit. */
    public static final double RADIUS = 4;
    /** Speed of the weakest shot, in px/s. */
    public static final double MIN_SPEED = 350;
    /** Speed of the strongest shot, in px/s. */
    public static final double MAX_SPEED = 1500;
    private static final double DRAG = 1.4;
    private static final double FADE_SPEED = 100;
    private static final double TRAIL_TIME = 0.04;
    private static final Color CORE = new Color(235, 254, 255);
    private static final Color TRAIL = alpha(FluidRenderer.NEON, 70);
    private static final Color GLOW = alpha(FluidRenderer.NEON, 45);
    private static final Color BALL = alpha(FluidRenderer.NEON, 230);
    private static final BasicStroke TRAIL_STROKE = new BasicStroke((float) RADIUS, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND);

    private double x;
    private double y;
    private double vx;
    private double vy;
    private boolean alive = true;

    /**
     * Fires a ball.
     * @param x centre x, in world px
     * @param y centre y, in world px
     * @param directionX x of the direction it is fired in, of any length
     * @param directionY y of the direction
     * @param power power of the shot, from 0 (the weakest) to 1 (the strongest)
     */
    public Shot(double x, double y, double directionX, double directionY, double power) {
        this.x = x;
        this.y = y;
        double length = Math.hypot(directionX, directionY);
        double speed = MIN_SPEED + Math.max(0, Math.min(1, power)) * (MAX_SPEED - MIN_SPEED);
        vx = length == 0 ? speed : directionX / length * speed;
        vy = length == 0 ? 0 : directionY / length * speed;
    }

    /** {@return the centre x, in world px} */
    public double x() {
        return x;
    }

    /** {@return the centre y, in world px} */
    public double y() {
        return y;
    }

    /** {@return the velocity along x, in px/s} */
    public double vx() {
        return vx;
    }

    /** {@return the velocity along y, in px/s} */
    public double vy() {
        return vy;
    }

    /** {@return the speed, in px/s} */
    public double speed() {
        return Math.hypot(vx, vy);
    }

    /** {@return whether the ball is still flying} */
    public boolean isAlive() {
        return alive;
    }

    /**
     * Moves the ball by one frame, in steps of half its radius so it can't jump over a thin wall. The air slows
     * it down; it stops when it leaves the world, touches an obstacle or an enemy, or gets too slow, and hits
     * the targets on its way.
     * @param dt time since the last frame, in s
     * @param arena the world the ball flies in
     * @param targets what the ball can hit
     */
    public void update(double dt, Arena arena, List<? extends Target> targets) {
        List<? extends Obstacle> near = arena.obstaclesNear(x, y, RADIUS + speed() * dt);
        int steps = (int) Math.max(1, Math.ceil(speed() * dt / (RADIUS / 2)));
        double step = dt / steps;
        for (int i = 0; i < steps && alive; i++) {
            vx /= 1 + DRAG * step;
            vy /= 1 + DRAG * step;
            x += vx * step;
            y += vy * step;
            if (speed() < FADE_SPEED || isStopped(arena, near)) {
                alive = false;
            } else {
                strike(targets);
            }
        }
    }

    private boolean isStopped(Arena arena, List<? extends Obstacle> near) {
        if (x < 0 || x > arena.width() || y < 0 || y > arena.height()) {
            return true;
        }
        for (Obstacle obstacle : near) {
            if (obstacle.touches(x, y, RADIUS)) {
                return true;
            }
        }
        for (Enemy enemy : arena.enemies()) {
            double reach = enemy.radius() + RADIUS;
            if (Math.abs(enemy.x() - x) < reach && Math.abs(enemy.y() - y) < reach && enemy.touches(x, y, RADIUS)) {
                return true;
            }
        }
        return false;
    }

    private void strike(List<? extends Target> targets) {
        for (Target target : targets) {
            if (target.isUnder(x, y, RADIUS)) {
                target.strike();
            }
        }
    }

    /**
     * Tells whether the ball or its trail shows in an area.
     * @param view area of the world, in world px
     * @return {@code true} if it shows in that area
     */
    public boolean isVisibleIn(Rectangle2D view) {
        double reach = 3 * RADIUS + speed() * TRAIL_TIME;
        return view.intersects(x - reach, y - reach, 2 * reach, 2 * reach);
    }

    /**
     * Draws the ball: a bright core, a glow and a short trail behind it.
     * @param g graphics drawing in the world
     */
    public void draw(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(TRAIL);
        g2.setStroke(TRAIL_STROKE);
        g2.draw(new Line2D.Double(x, y, x - vx * TRAIL_TIME, y - vy * TRAIL_TIME));
        g2.setColor(GLOW);
        g2.fill(disc(3 * RADIUS));
        g2.setColor(BALL);
        g2.fill(disc(RADIUS));
        g2.setColor(CORE);
        g2.fill(disc(0.5 * RADIUS));
        g2.dispose();
    }

    private Ellipse2D disc(double radius) {
        return new Ellipse2D.Double(x - radius, y - radius, 2 * radius, 2 * radius);
    }

    private static Color alpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
