package enemy.ability;

import fluid.Droplet;
import fluid.FluidRenderer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/**
 * The Shoot ability: while its key is held, a shot charges, and its power grows with the time; letting go fires a
 * {@link Shot} from the drop, in the direction aimed at. Held longer than {@link #CHARGE_TIME}, the charge goes
 * past its maximum and blows the drop up.
 * <p>The world tells it the key and the aim once a frame with {@link #hold}, then lets the time pass with
 * {@link #charge}. The aim is either given as a direction, by a stick, or {@linkplain #turn turned} step by
 * step, by two keys; an arrow shows it while the shot charges.
 */
public final class Shoot {
    /** How long the key can be held, in s: the power grows with it, and at the end the drop blows up. */
    public static final double CHARGE_TIME = 4;
    /** Power from which the charge is shown as about to blow up, from 0 to 1. */
    public static final double DANGER_POWER = 0.8;
    /** Angle the aim turns by at each step, in degrees. */
    public static final double AIM_STEP = 2;
    private static final double TURN_REPEAT_DELAY = 0.3;
    private static final double TURN_REPEAT_INTERVAL = 1.0 / 45;
    private static final double ARROW_HEAD = 13;
    private static final double ARROW_HALF_WIDTH = 7;
    private static final double GAUGE_GAP = 12;
    private static final double AIM_START = 14;
    private static final double AIM_LENGTH = 70;
    private static final double AIM_GROWTH = 150;
    private static final Color DANGER = new Color(255, 90, 80);
    private static final Color GAUGE_TRACK = new Color(140, 150, 175, 90);
    private static final BasicStroke GAUGE_STROKE = new BasicStroke(5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
    private static final BasicStroke AIM_STROKE = new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
            10f, new float[] {2f, 10f}, 0f);

    private boolean charging;
    private boolean armed = true;
    private double heldTime;
    private double aimX = 1;
    private double aimY;
    private int turning;
    private double turnRepeat;

    /** Creates the ability, aiming to the right and with nothing charged. */
    public Shoot() {
    }

    /**
     * Tells the ability whether the shoot key is held and where the player aims, once a frame. Holding the key
     * starts a charge; letting go fires it.
     * @param drop the player's drop, which the ball leaves from
     * @param keyHeld whether the shoot key is held
     * @param directionX x of the direction aimed at, of any length; 0 with directionY to keep the last one
     * @param directionY y of the direction aimed at
     * @param allowed whether the player can shoot; a charge never starts otherwise
     * @return the ball fired when the key has just been let go, or {@code null}
     */
    public Shot hold(Droplet drop, boolean keyHeld, double directionX, double directionY, boolean allowed) {
        double length = Math.hypot(directionX, directionY);
        if (length > 0) {
            aimX = directionX / length;
            aimY = directionY / length;
        }
        if (keyHeld) {
            if (!charging && armed && allowed) {
                charging = true;
                heldTime = 0;
            }
            return null;
        }
        armed = true;
        if (!charging) {
            return null;
        }
        double reach = drop.radius() + Shot.RADIUS;
        Shot shot = new Shot(drop.x() + aimX * reach, drop.y() + aimY * reach, aimX, aimY, power());
        charging = false;
        heldTime = 0;
        return shot;
    }

    /**
     * Lets the time pass on the charge.
     * @param dt time since the last frame, in s
     * @return {@code true} if the charge has gone past its maximum: the drop blows up, and the charge is lost
     */
    public boolean charge(double dt) {
        if (!charging) {
            return false;
        }
        heldTime += dt;
        if (heldTime < CHARGE_TIME) {
            return false;
        }
        cancel();
        return true;
    }

    /**
     * Turns the aim of the shot being charged, once a frame: one {@linkplain #AIM_STEP step} when a turn key is
     * pressed, then more steps while it stays held. Nothing turns when no shot is charging.
     * @param direction -1 to turn left (counter-clockwise on screen), 1 to turn right, 0 when no turn key is held
     * @param dt time since the last frame, in s
     */
    public void turn(int direction, double dt) {
        if (!charging || direction == 0) {
            turning = 0;
            return;
        }
        if (direction != turning) {
            turning = direction;
            turnRepeat = TURN_REPEAT_DELAY;
            rotate(direction);
            return;
        }
        turnRepeat -= dt;
        while (turnRepeat <= 0) {
            rotate(direction);
            turnRepeat += TURN_REPEAT_INTERVAL;
        }
    }

    private void rotate(int steps) {
        double angle = Math.atan2(aimY, aimX) + Math.toRadians(steps * AIM_STEP);
        aimX = Math.cos(angle);
        aimY = Math.sin(angle);
    }

    /** {@return x of the unit direction aimed at} */
    public double aimX() {
        return aimX;
    }

    /** {@return y of the unit direction aimed at} */
    public double aimY() {
        return aimY;
    }

    /** Drops the charge without firing it; the shoot key must be let go before the next shot. */
    public void cancel() {
        charging = false;
        heldTime = 0;
        armed = false;
    }

    /** {@return whether a shot is being charged} */
    public boolean isCharging() {
        return charging;
    }

    /** {@return the power of the shot being charged, from 0 to 1 where the drop blows up; 0 when none is} */
    public double power() {
        return charging ? Math.min(1, heldTime / CHARGE_TIME) : 0;
    }

    /**
     * Draws the shot being charged around the drop: a ring that fills up with the power, and an arrow that shows
     * where the ball will go. Both turn red once the power passes {@link #DANGER_POWER}. Nothing is drawn when no
     * shot is charging.
     * @param g graphics drawing in the world
     * @param drop the player's drop
     */
    public void draw(Graphics2D g, Droplet drop) {
        if (!charging) {
            return;
        }
        double power = power();
        double x = drop.x();
        double y = drop.y();
        double ring = drop.radius() + GAUGE_GAP;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(GAUGE_STROKE);
        g2.setColor(GAUGE_TRACK);
        g2.draw(new Ellipse2D.Double(x - ring, y - ring, 2 * ring, 2 * ring));
        g2.setColor(power >= DANGER_POWER ? DANGER : FluidRenderer.NEON);
        g2.draw(new Arc2D.Double(x - ring, y - ring, 2 * ring, 2 * ring, 90, -360 * power, Arc2D.OPEN));

        double from = ring + AIM_START;
        double to = from + AIM_LENGTH + AIM_GROWTH * power;
        g2.setStroke(AIM_STROKE);
        g2.draw(new Line2D.Double(x + aimX * from, y + aimY * from, x + aimX * to, y + aimY * to));
        double tipX = x + aimX * (to + ARROW_HEAD);
        double tipY = y + aimY * (to + ARROW_HEAD);
        double baseX = x + aimX * to;
        double baseY = y + aimY * to;
        Path2D head = new Path2D.Double();
        head.moveTo(tipX, tipY);
        head.lineTo(baseX - aimY * ARROW_HALF_WIDTH, baseY + aimX * ARROW_HALF_WIDTH);
        head.lineTo(baseX + aimY * ARROW_HALF_WIDTH, baseY - aimX * ARROW_HALF_WIDTH);
        head.closePath();
        g2.fill(head);
        g2.dispose();
    }
}
