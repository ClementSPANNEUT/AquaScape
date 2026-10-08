package world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import enemy.Spinner;
import enemy.ability.Ability;
import enemy.ability.Shoot;
import enemy.ability.Shot;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class ShootTest {
    private static final double FRAME = 1 / 60.0;

    private static World world(Progress progress, Consumer<World> layout) {
        return new World(new Level("shoot", "", 8000, 8000, layout), progress);
    }

    private static World world() {
        return world(withShoot(), w -> w.addDroplet(1000, 1000, 1));
    }

    private static Progress withShoot() {
        return new Progress(List.of(), List.of("Split", "Shoot"), List.of(), 0);
    }

    private static void charge(World world, double seconds, double aimX, double aimY) {
        for (int i = 0; i < Math.round(seconds / FRAME); i++) {
            world.holdShot(true, aimX, aimY);
            world.update(FRAME, 0, 0);
        }
    }

    private static void run(World world, double seconds) {
        for (int i = 0; i < Math.round(seconds / FRAME); i++) {
            world.holdShot(false, 0, 0);
            world.update(FRAME, 0, 0);
        }
    }

    private static int[] pixels(World world) {
        BufferedImage image = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.translate(-600, -600);
        world.draw(g, new Rectangle(600, 600, 800, 800));
        g.dispose();
        return image.getRGB(0, 0, 800, 800, null, 0, 800);
    }

    @Test
    void shootingNeedsTheLightOfShoot() {
        World without = world(new Progress(List.of(), List.of("Split", "Dash"), List.of(), 0), w -> w.addDroplet(1000, 1000, 1));
        assertFalse(without.canShoot());
        charge(without, 0.5, 1, 0);
        assertFalse(without.isChargingShot());
        run(without, FRAME);
        assertTrue(without.shots().isEmpty());

        World with = world();
        assertTrue(with.canShoot());
        charge(with, 0.5, 1, 0);
        assertTrue(with.isChargingShot());
        assertEquals(0.5 / Shoot.CHARGE_TIME, with.shotPower(), 0.02);
    }

    @Test
    void takingTheLightOfShootUnlocksTheShootAtOnce() {
        Progress progress = new Progress(List.of(), List.of("Split"), List.of(), 0);
        World world = world(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            w.addLight(new Light("Shoot", 1000, 1000, false));
        });
        assertFalse(world.canShoot());
        world.update(FRAME, 0, 0);
        assertEquals("Shoot", world.takeNewLight());
        assertTrue(progress.hasAbility(Ability.SHOOT));
        assertTrue(world.canShoot());
    }

    @Test
    void lettingGoFiresABallWhereThePlayerAims() {
        World world = world();
        charge(world, 0.6, 0, 1);
        world.holdShot(false, 0, 1);
        assertFalse(world.isChargingShot());
        assertEquals(0, world.shotPower(), 1e-9);
        assertEquals(1, world.shots().size());
        Shot shot = world.shots().get(0);
        assertEquals(1000, shot.x(), 1e-6);
        assertTrue(shot.y() > 1030 && shot.vy() > 0, "it leaves downward from the edge of the drop");
        world.update(FRAME, 0, 0);
        assertTrue(shot.y() > 1040, "and flies on");
        assertEquals(1, world.droplets().size(), "the drop loses no water");
        assertEquals(1, world.playerWaterShare(), 1e-9);
    }

    @Test
    void theLongerTheKeyIsHeldTheFurtherTheBallGoes() {
        World weak = world();
        charge(weak, 0.15, 1, 0);
        weak.holdShot(false, 1, 0);
        Shot slow = weak.shots().get(0);
        World strong = world();
        charge(strong, 0.85 * Shoot.CHARGE_TIME, 1, 0);
        strong.holdShot(false, 1, 0);
        Shot fast = strong.shots().get(0);
        assertTrue(fast.speed() > slow.speed() + 600);
        run(weak, 3);
        run(strong, 3);
        assertTrue(weak.shots().isEmpty() && strong.shots().isEmpty(), "both balls died out");
        assertTrue(fast.x() > slow.x() + 400);
    }

    @Test
    void withoutANewAimTheBallGoesWhereThePlayerLastAimed() {
        World world = world();
        world.holdShot(false, -1, 0);
        charge(world, 0.3, 0, 0);
        world.holdShot(false, 0, 0);
        assertTrue(world.shots().get(0).vx() < 0);
    }

    @Test
    void theAimTurnsStepByStepWhileTheShotCharges() {
        World world = world();
        world.turnAim(1, FRAME);
        charge(world, 0.2, 0, 0);
        world.turnAim(1, FRAME);
        world.turnAim(0, FRAME);
        world.turnAim(1, FRAME);
        world.holdShot(false, 0, 0);
        Shot shot = world.shots().get(0);
        assertEquals(2 * Shoot.AIM_STEP, Math.toDegrees(Math.atan2(shot.vy(), shot.vx())), 1e-9,
                "two presses while charging, and the one before the charge doesn't count");
    }

    @Test
    void heldPastTheMaximumTheShotBlowsTheDropUp() {
        Progress progress = withShoot();
        World world = world(progress, w -> w.addDroplet(1000, 1000, 1));
        charge(world, Shoot.CHARGE_TIME - 0.1, 1, 0);
        assertFalse(world.isPlayerDead());
        assertTrue(world.shotPower() > Shoot.DANGER_POWER);
        int[] charging = pixels(world);
        charge(world, 0.2, 1, 0);
        assertTrue(world.isPlayerDead());
        assertTrue(world.hasExploded());
        assertEquals(1, progress.hits());
        assertTrue(world.droplets().isEmpty());
        assertTrue(world.shots().isEmpty(), "no ball leaves");
        assertFalse(world.canShoot());
        assertFalse(Arrays.equals(charging, pixels(world)), "the blast replaces the drop");
        charge(world, 0.5, 1, 0);
        assertEquals(1, progress.hits(), "it only dies once");
    }

    @Test
    void aDropKilledByAWallHasNotExploded() {
        World world = world(withShoot(), w -> {
            w.addDroplet(1000, 1000, 1);
            w.addWall(1020, 800, 1020, 1200);
        });
        world.update(FRAME, 0, 0);
        assertTrue(world.isPlayerDead());
        assertFalse(world.hasExploded());
    }

    @Test
    void aPausedShotIsNotFired() {
        World world = world();
        charge(world, 0.6, 1, 0);
        world.cancelShot();
        assertFalse(world.isChargingShot());
        charge(world, 0.2, 1, 0);
        assertFalse(world.isChargingShot(), "a key still held doesn't charge again");
        world.holdShot(false, 1, 0);
        assertTrue(world.shots().isEmpty());
        charge(world, 0.2, 1, 0);
        assertTrue(world.isChargingShot());
    }

    @Test
    void theBallStopsOnWallsDoorsAndEnemies() {
        World walled = world(withShoot(), w -> {
            w.addDroplet(1000, 1000, 1);
            w.addWall(1400, 800, 1400, 1200);
        });
        charge(walled, 0.6 * Shoot.CHARGE_TIME, 1, 0);
        walled.holdShot(false, 1, 0);
        Shot atWall = walled.shots().get(0);
        run(walled, 1);
        assertFalse(atWall.isAlive());
        assertTrue(atWall.x() < 1400 && atWall.x() > 1380);

        World guarded = world(withShoot(), w -> {
            w.addDroplet(1000, 1000, 1);
            w.addEnemy(new Spinner(1400, 1000, 80, 90, 0));
        });
        charge(guarded, 0.6 * Shoot.CHARGE_TIME, 1, 0);
        guarded.holdShot(false, 1, 0);
        Shot atEnemy = guarded.shots().get(0);
        run(guarded, 1);
        assertFalse(atEnemy.isAlive());
        assertTrue(atEnemy.x() < 1420);

        World closed = world(withShoot(), w -> {
            w.addDroplet(1000, 1000, 1);
            w.addDoor(new Door(1400, 800, 1400, 1200, false));
        });
        charge(closed, 0.6 * Shoot.CHARGE_TIME, 1, 0);
        closed.holdShot(false, 1, 0);
        Shot atDoor = closed.shots().get(0);
        run(closed, 1);
        assertTrue(atDoor.x() < 1400, "a closed door stops it too");
    }

    @Test
    void theBallPressesTheButtonItHits() {
        Progress progress = withShoot();
        World world = world(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            w.addWall(1200, 800, 1200, 985);
            w.addWall(1200, 1015, 1200, 1200);
            int door = w.addDoor(new Door(900, 1300, 1100, 1300, false));
            w.addButton(new DoorButton(door, 1500, 1000, true));
        });
        assertEquals(1, world.obstaclesNear(1000, 1300, 1).stream().filter(wall -> wall.minY() == 1300).count());
        charge(world, 0.6 * Shoot.CHARGE_TIME, 1, 0);
        world.holdShot(false, 1, 0);
        run(world, 1);
        assertEquals(0, progress.hits(), "the drop stayed behind the slit, too narrow for it");
        assertTrue(progress.isLocked(0), "the ball went through and locked the door open");
        assertEquals(0, world.obstaclesNear(1000, 1300, 1).stream().filter(wall -> wall.minY() == 1300).count());
    }

    @Test
    void aBallOnlyHoldsAButtonWhileItFliesOverIt() {
        World world = world(withShoot(), w -> {
            w.addDroplet(1000, 1000, 1);
            w.addButton(new DoorButton(w.addDoor(new Door(900, 1300, 1100, 1300, false)), 1300, 1000, false));
        });
        charge(world, 0.2 * Shoot.CHARGE_TIME, 1, 0);
        world.holdShot(false, 1, 0);
        boolean opened = false;
        for (int i = 0; i < 180; i++) {
            world.update(FRAME, 0, 0);
            opened |= world.obstaclesNear(1000, 1300, 1).isEmpty();
        }
        assertTrue(opened, "the door opened as the ball passed");
        assertEquals(1, world.obstaclesNear(1000, 1300, 1).size(), "and closed again behind it");
    }

    @Test
    void theChargeAndTheBallsAreDrawn() {
        World world = world();
        int[] idle = pixels(world);
        charge(world, 0.6, 1, 0);
        int[] charging = pixels(world);
        assertFalse(Arrays.equals(idle, charging), "the gauge and the aim show around the drop");
        world.holdShot(false, 1, 0);
        world.update(FRAME, 0, 0);
        int[] fired = pixels(world);
        assertFalse(Arrays.equals(charging, fired));
        assertFalse(Arrays.equals(idle, fired), "the ball shows");
    }

    @Test
    void theOldestBallsGiveWayToTheNewOnes() {
        World world = world();
        for (int i = 0; i < 40; i++) {
            world.holdShot(true, 1, 0);
            world.holdShot(false, 1, 0);
        }
        assertTrue(world.shots().size() <= 24);
        assertFalse(world.shots().isEmpty());
    }
}
