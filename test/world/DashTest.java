package world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import enemy.Spinner;
import enemy.ability.Ability;
import fluid.Droplet;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class DashTest {
    private static final double FRAME = 1 / 60.0;

    private static World world(Progress progress, Consumer<World> layout) {
        return new World(new Level("dash", "", 8000, 8000, layout), progress);
    }

    private static World world() {
        return world(withSplit(), w -> w.addDroplet(1000, 1000, 1));
    }

    private static Progress withSplit() {
        return new Progress(List.of(), List.of("Split"), List.of(), 0);
    }

    private static void run(World world, double seconds, int dx) {
        for (int i = 0; i < Math.round(seconds / FRAME); i++) {
            world.update(FRAME, dx, 0);
        }
    }

    private static void hold(World world, double seconds, int dx) {
        for (int i = 0; i < Math.round(seconds / FRAME); i++) {
            world.holdDash(true);
            world.update(FRAME, dx, 0);
        }
    }

    private static void tear(World world) {
        for (int i = 0; i < 300 && world.droplets().size() == 1; i++) {
            world.holdDash(true);
            world.update(FRAME, 1, 0);
        }
        assertEquals(2, world.droplets().size(), "pulled long enough, the drop tears");
    }

    @Test
    void dashingNeedsTheLightOfSplit() {
        World without = world(new Progress(), w -> w.addDroplet(1000, 1000, 1));
        assertFalse(without.canDash());
        hold(without, 0.5, 1);
        assertFalse(without.player().isAnchored());
        assertEquals(0, without.dashTension(), 1e-9);

        World with = world();
        assertTrue(with.canDash());
        hold(with, 0.5, 1);
        assertTrue(with.player().isAnchored());
        assertTrue(with.dashTension() > 0);
    }

    @Test
    void takingTheLightOfSplitUnlocksTheDashAtOnce() {
        Progress progress = new Progress();
        World world = world(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            w.addLight(new Light("Split", 1000, 1000, false));
        });
        assertFalse(world.canDash());
        run(world, FRAME, 0);
        assertEquals("Split", world.takeNewLight());
        assertTrue(progress.hasAbility(Ability.DASH));
        world.holdDash(true);
        assertTrue(world.player().isAnchored(), "the dash works right after the light");
        assertTrue(progress.takeChanged(), "and the light is waiting to be saved");
    }

    @Test
    void holdingSticksTheBackAndStretchesTheHead() {
        World world = world();
        hold(world, 0.9, 1);
        Droplet player = world.player();
        assertEquals(1000, player.bodyX(), 1e-9, "the back stays on the ground");
        assertTrue(player.x() > 1100);
        assertTrue(world.dashTension() > 0.5 && world.dashTension() < 1);
        assertEquals(1, world.droplets().size());
    }

    @Test
    void lettingGoThrowsTheDropWithoutBreakingIt() {
        World world = world();
        hold(world, 0.9, 1);
        double x = world.player().x();
        world.holdDash(false);
        assertFalse(world.player().isAnchored());
        assertTrue(world.player().vx() > 500, "thrown toward the head");
        assertEquals(0, world.dashTension(), 1e-9);
        for (int i = 0; i < 90; i++) {
            world.holdDash(false);
            world.update(FRAME, 0, 0);
            assertTrue(world.player().distortion() < 1);
        }
        assertTrue(world.player().x() > x + 150);
        assertEquals(1000, world.player().y(), 1e-6);
        assertEquals(1, world.droplets().size(), "the bubble holds together");
    }

    @Test
    void theMoreStretchedTheStrongerTheLaunch() {
        World weak = world();
        hold(weak, 0.5, 1);
        weak.holdDash(false);
        World strong = world();
        hold(strong, 1.0, 1);
        strong.holdDash(false);
        assertTrue(strong.player().speed() > weak.player().speed() + 200);
    }

    @Test
    void pulledTooFarTheDropTearsAndThePlayerKeepsTheHead() {
        World world = world();
        tear(world);
        Droplet head = world.droplets().get(0);
        Droplet back = world.droplets().get(1);
        assertSame(head, world.player());
        assertTrue(head.x() > 1150);
        assertEquals(1000, back.x(), 1e-6, "the back half stays where it was stuck");
        assertEquals(0.5, world.playerWaterShare(), 1e-9);
        assertEquals(head.volume(), back.volume(), 1e-6);

        hold(world, 0.2, 0);
        assertFalse(world.player().isAnchored(), "the key must be let go before the next dash");
        world.holdDash(false);
        world.holdDash(true);
        assertTrue(world.player().isAnchored());
    }

    @Test
    void theOtherHalfCanBeChosen() {
        World single = world();
        assertFalse(single.switchDrop(), "nothing to choose with one drop");

        World world = world();
        tear(world);
        Droplet head = world.player();
        assertTrue(world.switchDrop());
        Droplet back = world.player();
        assertNotSame(head, back);
        assertEquals(1000, back.x(), 1e-6);
        world.holdDash(false);
        double headX = head.x();
        for (int i = 0; i < 30; i++) {
            world.update(FRAME, 0, 1);
        }
        assertTrue(back.y() > 1010, "the keys now steer the back half");
        assertEquals(1000, head.y(), 1e-6, "and no longer the head");
        assertTrue(head.x() >= headX);
        assertTrue(world.switchDrop());
        assertSame(head, world.player(), "choosing again gives the head back");
    }

    @Test
    void choosingAnotherDropLetsGoOfTheGround() {
        World world = world();
        tear(world);
        world.holdDash(false);
        hold(world, 0.4, 1);
        Droplet head = world.player();
        assertTrue(head.isAnchored());
        double speed = head.speed();
        world.switchDrop();
        assertFalse(head.isAnchored());
        assertEquals(speed, head.speed(), 1e-9, "the drop left behind isn't thrown");
        world.holdDash(true);
        assertFalse(world.player().isAnchored(), "the key must be let go first");
    }

    @Test
    void theHalvesMergeBackWhenTheyTouch() {
        World world = world();
        tear(world);
        world.holdDash(false);
        for (int i = 0; i < 240 && world.droplets().size() > 1; i++) {
            world.update(FRAME, -1, 0);
        }
        assertEquals(1, world.droplets().size());
        assertEquals(1, world.playerWaterShare(), 1e-9);
    }

    @Test
    void aPausedDashIsNotThrown() {
        World world = world();
        hold(world, 0.9, 1);
        double speed = world.player().speed();
        world.cancelDash();
        assertFalse(world.player().isAnchored());
        assertEquals(speed, world.player().speed(), 1e-9);
        hold(world, 0.1, 0);
        assertFalse(world.player().isAnchored(), "a key still held doesn't stick the drop again");
        world.holdDash(false);
        world.holdDash(true);
        assertTrue(world.player().isAnchored());
    }

    @Test
    void aThrownDropCannotGoThroughAWall() {
        Progress progress = withSplit();
        World world = world(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            w.addWall(1300, 800, 1300, 1200);
        });
        hold(world, 1.0, 1);
        assertEquals(0, progress.hits());
        world.holdDash(false);
        for (int i = 0; i < 8; i++) {
            world.update(0.05, 0, 0);
        }
        assertEquals(1, progress.hits(), "even with long frames, the wall is hit");
        assertTrue(world.player().x() < 1300);
        assertFalse(world.canDash(), "a dead drop can't dash");
        world.holdDash(true);
        assertEquals(0, world.dashTension(), 1e-9);
    }

    @Test
    void enemiesStayDeadlyForAThrownDrop() {
        Progress progress = withSplit();
        World world = world(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            w.addEnemy(new Spinner(1330, 1000, 60, 90, 0));
        });
        hold(world, 0.9, 1);
        assertEquals(0, progress.hits());
        world.holdDash(false);
        run(world, 1, 0);
        assertEquals(1, progress.hits());
    }

    @Test
    void theStuckBackHoldsAButtonWhileTheHeadReachesAnother() {
        Progress progress = withSplit();
        World world = world(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            int door = w.addDoor(new Door(1090, 800, 1090, 1200, false));
            w.addButton(new DoorButton(door, 1000, 1000, false));
            w.addButton(new DoorButton(door, 1180, 1000, true));
        });
        run(world, FRAME, 0);
        assertTrue(world.obstaclesNear(1090, 1000, 1).isEmpty(), "standing on the hold button opens the door");
        hold(world, 0.9, 1);
        assertEquals(0, progress.hits(), "the head went through the open door");
        assertTrue(world.player().x() > 1116, "and reached the lock button");
        assertTrue(progress.isLocked(0));
        world.cancelDash();
        run(world, FRAME, 0);
        assertTrue(world.obstaclesNear(1090, 1000, 1).isEmpty(), "the door stays open once the back lets go");
    }

    @Test
    void theStuckBackAloneKeepsADoorOpen() {
        World world = world(withSplit(), w -> {
            w.addDroplet(1000, 1000, 1);
            w.addButton(new DoorButton(w.addDoor(new Door(1500, 800, 1500, 1200, false)), 1000, 1000, false));
        });
        hold(world, 0.9, 1);
        assertTrue(world.player().x() > 1100, "the head has left the button");
        assertTrue(world.obstaclesNear(1500, 1000, 1).isEmpty(), "but the back is still on it");
        world.cancelDash();
        run(world, FRAME, 0);
        assertEquals(1, world.obstaclesNear(1500, 1000, 1).size(), "the door closes when the back lets go");
    }

    @Test
    void aTornHalfLeftOnAButtonKeepsTheDoorOpenForTheOther() {
        Progress progress = withSplit();
        World world = world(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            w.addButton(new DoorButton(w.addDoor(new Door(1400, 800, 1400, 1200, false)), 1000, 1000, false));
        });
        tear(world);
        world.holdDash(false);
        run(world, 1.5, 1);
        assertTrue(world.player().x() > 1450, "the head half went through the door");
        assertEquals(0, progress.hits());
        assertEquals(2, world.droplets().size());
        assertTrue(world.obstaclesNear(1400, 1000, 1).isEmpty());
    }
}
