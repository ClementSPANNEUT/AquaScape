package enemy.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fluid.Droplet;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ShootTest {
    private static final double FRAME = 1 / 60.0;

    private final Shoot shoot = new Shoot();
    private final Droplet drop = new Droplet(1000, 1000, 30);

    private boolean charge(double seconds) {
        boolean blownUp = false;
        for (int i = 0; i < Math.round(seconds / FRAME); i++) {
            assertNull(shoot.hold(drop, true, 0, 0, true), "nothing is fired while the key is held");
            blownUp |= shoot.charge(FRAME);
        }
        return blownUp;
    }

    private static int[] pixels(Shoot shoot, Droplet drop) {
        BufferedImage image = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.translate(300 - drop.x(), 300 - drop.y());
        shoot.draw(g, drop);
        g.dispose();
        return image.getRGB(0, 0, 600, 600, null, 0, 600);
    }

    @Test
    void thePowerGrowsWhileTheKeyIsHeld() {
        assertEquals(0, shoot.power(), 1e-9);
        assertFalse(shoot.isCharging());
        assertFalse(charge(Shoot.CHARGE_TIME / 2));
        assertTrue(shoot.isCharging());
        assertEquals(0.5, shoot.power(), 0.02);
    }

    @Test
    void lettingGoFiresABallWhereThePlayerAims() {
        shoot.hold(drop, false, 0, -3, true);
        charge(Shoot.CHARGE_TIME / 2);
        Shot shot = shoot.hold(drop, false, 0, 0, true);
        assertNotNull(shot);
        assertFalse(shoot.isCharging());
        assertEquals(1000, shot.x(), 1e-9);
        assertEquals(1000 - 30 - Shot.RADIUS, shot.y(), 1e-9, "it leaves from the edge of the drop");
        assertEquals(0, shot.vx(), 1e-9);
        assertTrue(shot.vy() < 0, "upward, the last direction aimed at");
        assertNull(shoot.hold(drop, false, 0, 0, true), "one ball per charge");
    }

    @Test
    void theLongerTheChargeTheFasterTheBall() {
        charge(0.1 * Shoot.CHARGE_TIME);
        Shot weak = shoot.hold(drop, false, 1, 0, true);
        charge(0.8 * Shoot.CHARGE_TIME);
        Shot strong = shoot.hold(drop, false, 1, 0, true);
        assertTrue(weak.speed() >= Shot.MIN_SPEED);
        assertTrue(strong.speed() > weak.speed() + 500);
        assertTrue(strong.speed() < Shot.MAX_SPEED);
    }

    @Test
    void theAimCanChangeWhileCharging() {
        shoot.hold(drop, true, 1, 0, true);
        shoot.charge(0.3);
        Shot shot = shoot.hold(drop, false, -2, 0, true);
        assertTrue(shot.vx() < 0);
        assertEquals(1000 - 30 - Shot.RADIUS, shot.x(), 1e-9);
    }

    private double aimAngle() {
        return Math.toDegrees(Math.atan2(shoot.aimY(), shoot.aimX()));
    }

    @Test
    void aTapOnATurnKeyTurnsTheAimByOneStep() {
        charge(0.2);
        assertEquals(0, aimAngle(), 1e-9, "the aim starts to the right");
        shoot.turn(1, FRAME);
        assertEquals(Shoot.AIM_STEP, aimAngle(), 1e-9, "to the right is clockwise on screen, where y goes down");
        shoot.turn(1, FRAME);
        assertEquals(Shoot.AIM_STEP, aimAngle(), 1e-9, "one step per press, not one per frame");
        shoot.turn(0, FRAME);
        shoot.turn(-1, FRAME);
        shoot.turn(0, FRAME);
        shoot.turn(-1, FRAME);
        assertEquals(-Shoot.AIM_STEP, aimAngle(), 1e-9);
        Shot shot = shoot.hold(drop, false, 0, 0, true);
        assertEquals(-Shoot.AIM_STEP, Math.toDegrees(Math.atan2(shot.vy(), shot.vx())), 1e-9, "the ball follows it");
    }

    @Test
    void aTurnKeyHeldKeepsTurningAfterAMoment() {
        charge(0.2);
        for (int i = 0; i < 12; i++) {
            shoot.turn(-1, FRAME);
        }
        assertEquals(-Shoot.AIM_STEP, aimAngle(), 1e-9, "nothing more during the first fifth of a second");
        for (int i = 0; i < 48; i++) {
            shoot.turn(-1, FRAME);
        }
        assertTrue(aimAngle() < -40 && aimAngle() > -90, "then it spins: " + aimAngle());
        double turned = aimAngle();
        shoot.turn(1, FRAME);
        assertEquals(turned + Shoot.AIM_STEP, aimAngle(), 1e-9, "the other key turns back at once");
    }

    @Test
    void theAimOnlyTurnsWhileAShotCharges() {
        shoot.turn(1, FRAME);
        assertEquals(0, aimAngle(), 1e-9);
        charge(0.2);
        assertEquals(0, aimAngle(), 1e-9, "a key held before the charge counts as pressed once it starts");
        shoot.turn(1, FRAME);
        assertEquals(Shoot.AIM_STEP, aimAngle(), 1e-9);
    }

    @Test
    void aStickAimsWhereverItPointsAndTheKeysTurnFromThere() {
        shoot.hold(drop, true, 0, -1, true);
        assertEquals(-90, aimAngle(), 1e-9);
        shoot.turn(1, FRAME);
        assertEquals(-90 + Shoot.AIM_STEP, aimAngle(), 1e-9);
    }

    @Test
    void chargedPastItsMaximumTheShotBlowsUp() {
        assertFalse(charge(Shoot.CHARGE_TIME - 0.1));
        assertTrue(shoot.power() > Shoot.DANGER_POWER);
        assertTrue(charge(0.2), "the drop blows up");
        assertFalse(shoot.isCharging());
        assertNull(shoot.hold(drop, false, 0, 0, true), "and nothing is fired");
    }

    @Test
    void aPlayerWhoHasNotLearntItCannotShoot() {
        shoot.hold(drop, true, 1, 0, false);
        assertFalse(shoot.isCharging());
        assertFalse(shoot.charge(5));
        assertNull(shoot.hold(drop, false, 1, 0, false));
    }

    @Test
    void aCancelledShotIsNotFiredAndNeedsTheKeyLetGo() {
        charge(0.5);
        shoot.cancel();
        assertFalse(shoot.isCharging());
        shoot.hold(drop, true, 1, 0, true);
        assertFalse(shoot.isCharging(), "the key is still held");
        assertNull(shoot.hold(drop, false, 1, 0, true), "nothing is fired when it is let go");
        shoot.hold(drop, true, 1, 0, true);
        assertTrue(shoot.isCharging());
    }

    @Test
    void theChargeShowsAroundTheDrop() {
        int[] idle = pixels(shoot, drop);
        charge(0.3 * Shoot.CHARGE_TIME);
        int[] charging = pixels(shoot, drop);
        assertFalse(Arrays.equals(idle, charging), "a ring and an aim line show while charging");
        charge(0.6 * Shoot.CHARGE_TIME);
        assertFalse(Arrays.equals(charging, pixels(shoot, drop)), "they grow and turn red near the maximum");
    }
}
