package screen;

import fluid.Droplet;
import world.World;

/**
 * Follows the player's drop without pinning it to the middle of the screen: it aims between the drop's centre
 * and its soft body, which trails behind, and further ahead the faster the drop goes, then glides there. It
 * shows at most a margin past the edges of the world, so they stay visible, and centres a world smaller than
 * the screen.
 */
public class Camera {
    private static final double CENTER_WEIGHT = 0.6; 
    private static final double LOOK_AHEAD = 0.8; 
    private static final double MAX_LEAD = 0.25; 
    private static final double FOLLOW_RATE = 5;
    private static final double MARGIN = 150;

    private double x; 
    private double y;
    private boolean ready;

    /** Creates a camera; it snaps onto the drop on its first follow. */
    public Camera() {
    }

    /** {@return the left of the view, in world px} */
    public double x() {
        return x;
    }

    /** {@return the top of the view, in world px} */
    public double y() {
        return y;
    }

    /**
     * Moves the view straight onto its target, for a new try. If the screen has no size yet, it does so at the
     * next follow.
     * @param world the world whose player is followed
     * @param viewWidth width of the view, in px
     * @param viewHeight height of the view, in px
     */
    public void snap(World world, int viewWidth, int viewHeight) {
        if (viewWidth <= 0 || viewHeight <= 0) {
            ready = false;
            return;
        }
        x = targetX(world, viewWidth);
        y = targetY(world, viewHeight);
        ready = true;
    }

    /**
     * Glides the view toward its target.
     * @param dt time since the last frame, in s
     * @param world the world whose player is followed
     * @param viewWidth width of the view, in px
     * @param viewHeight height of the view, in px
     */
    public void follow(double dt, World world, int viewWidth, int viewHeight) {
        if (!ready) {
            snap(world, viewWidth, viewHeight);
            return;
        }
        double catchUp = 1 - Math.exp(-FOLLOW_RATE * dt);
        x += (targetX(world, viewWidth) - x) * catchUp;
        y += (targetY(world, viewHeight) - y) * catchUp;
    }

    private static double targetX(World world, int viewWidth) {
        Droplet drop = world.player();
        double focus = drop.x() * CENTER_WEIGHT + drop.bodyX() * (1 - CENTER_WEIGHT) + lead(drop.vx(), viewWidth);
        return keepInside(focus - viewWidth / 2.0, viewWidth, world.width());
    }

    private static double targetY(World world, int viewHeight) {
        Droplet drop = world.player();
        double focus = drop.y() * CENTER_WEIGHT + drop.bodyY() * (1 - CENTER_WEIGHT) + lead(drop.vy(), viewHeight);
        return keepInside(focus - viewHeight / 2.0, viewHeight, world.height());
    }

    private static double lead(double speed, int view) {
        double limit = MAX_LEAD * view;
        return Math.max(-limit, Math.min(limit, speed * LOOK_AHEAD));
    }

    private static double keepInside(double position, int view, int world) {
        if (world + 2 * MARGIN <= view) {
            return (world - view) / 2.0;
        }
        return Math.max(-MARGIN, Math.min(world - view + MARGIN, position));
    }
}
