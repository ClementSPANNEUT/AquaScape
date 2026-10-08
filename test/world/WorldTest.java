package world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import enemy.Bouncer;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import zone.HubZone;
import zone.Levels;
import zone.LightZone;
import zone.SplitZone;

class WorldTest {
    private static final int SIZE = 20_000;

    private static World world(Progress progress, Consumer<World> layout) {
        return new World(new Level("test", "", SIZE, SIZE, layout), progress);
    }

    private static void run(World world, double seconds) {
        for (int i = 0; i < Math.round(seconds * 60); i++) {
            world.update(1 / 60.0, 0, 0);
        }
    }

    private static boolean hasClosedDoor(World world) {
        return !world.obstaclesNear(0, 0, 1).isEmpty();
    }

    @Test
    void thePlayerStartsOnTheCurrentCheckpoint() {
        Progress progress = new Progress(List.of(), List.of(), List.of(), 1);
        World world = world(progress, w -> {
            w.addCheckpoint(500, 500);
            w.addCheckpoint(3000, 800);
        });
        assertEquals(3000, world.player().x());
        assertEquals(800, world.player().y());
        assertTrue(world.checkpoints().get(1).isReached());
        assertFalse(world.checkpoints().get(0).isReached());
    }

    @Test
    void touchingACheckpointMakesItTheRestartPoint() {
        Progress progress = new Progress();
        World world = world(progress, w -> {
            w.addDroplet(3000, 800, 1);
            w.addCheckpoint(500, 500);
            w.addCheckpoint(3000, 800);
        });
        run(world, 0.1);
        assertEquals(1, progress.checkpoint());
    }

    @Test
    void checkpointsGetTheNameOfTheirArea() {
        World world = world(new Progress(), w -> {
            w.setArea("Départ");
            w.addCheckpoint(500, 500);
            w.setArea("Hub");
            w.addCheckpoint(900, 500);
        });
        assertEquals("Départ", world.checkpoints().get(0).area());
        assertEquals("Hub", world.checkpoints().get(1).area());
    }

    @Test
    void takingALightIsReportedOnceAndSendsBackToTheHub() {
        Progress progress = new Progress();
        World world = world(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            w.setHub(w.addCheckpoint(5000, 5000));
            w.addLight(new Light("Split", 1000, 1000, false));
        });
        run(world, 0.1);
        assertEquals("Split", world.takeNewLight());
        assertNull(world.takeNewLight());
        assertTrue(progress.hasLight("Split"));
        assertEquals(0, progress.checkpoint(), "the hub becomes the restart point");
        assertEquals(List.of("Split"), world.obtainedLights());
        assertFalse(world.isComplete());
    }

    @Test
    void theLastLightWinsTheLevel() {
        World world = world(new Progress(), w -> {
            w.addDroplet(1000, 1000, 1);
            w.addLight(new Light("Fin", 1000, 1000, true));
        });
        run(world, 0.1);
        assertTrue(world.isComplete());
        assertNull(world.takeNewLight(), "the last light doesn't send back to the hub");
        assertEquals(0, world.lightCount());
    }

    @Test
    void theGateOpensWhenEveryLightIsBack() {
        Consumer<World> layout = w -> {
            w.addDroplet(1000, 1000, 1);
            w.addLight(new Light("A", 8000, 8000, false));
            w.addLight(new Light("B", 9000, 8000, false));
            w.addDoor(new Door(3000, 0, 3000, 400, true));
        };
        assertTrue(hasClosedDoor(world(new Progress(List.of(), List.of("A"), List.of(), 0), layout)));
        assertFalse(hasClosedDoor(world(new Progress(List.of(), List.of("A", "B"), List.of(), 0), layout)));
    }

    @Test
    void aHoldButtonOpensItsDoorOnlyWhilePressed() {
        Consumer<World> pressed = w -> {
            w.addDroplet(1000, 1000, 1);
            int door = w.addDoor(new Door(3000, 0, 3000, 400, false));
            w.addButton(new DoorButton(door, 1000, 1000, false));
        };
        Consumer<World> released = w -> {
            w.addDroplet(1000, 1000, 1);
            int door = w.addDoor(new Door(3000, 0, 3000, 400, false));
            w.addButton(new DoorButton(door, 2000, 2000, false));
        };
        World pressedWorld = world(new Progress(), pressed);
        World releasedWorld = world(new Progress(), released);
        run(pressedWorld, 0.1);
        run(releasedWorld, 0.1);
        assertFalse(hasClosedDoor(pressedWorld));
        assertTrue(hasClosedDoor(releasedWorld));
        assertFalse(pressedWorld.progress().isLocked(0), "a hold button doesn't lock the door");
    }

    @Test
    void aLockButtonKeepsItsDoorOpenForTheRestOfTheGame() {
        Progress progress = new Progress();
        World pressed = world(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            int door = w.addDoor(new Door(3000, 0, 3000, 400, false));
            w.addButton(new DoorButton(door, 1000, 1000, true));
        });
        run(pressed, 0.1);
        assertTrue(progress.isLocked(0));
        World later = world(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            int door = w.addDoor(new Door(3000, 0, 3000, 400, false));
            w.addButton(new DoorButton(door, 2000, 2000, true));
        });
        assertFalse(hasClosedDoor(later));
    }

    @Test
    void touchingAWallCountsOneHit() {
        Progress progress = new Progress();
        World world = world(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            w.addWall(900, 1000, 1100, 1000);
        });
        run(world, 1);
        assertEquals(1, progress.hits(), "one hit per contact, not one per frame");
    }

    @Test
    void touchingAnEnemyCountsAHit() {
        Progress progress = new Progress();
        World world = world(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            w.addEnemy(new Bouncer(1010, 1000, 0, 1));
        });
        run(world, 0.05);
        assertEquals(1, progress.hits());
    }

    @Test
    void onlyTheEnemiesNearThePlayerMove() {
        Bouncer near = new Bouncer(2000, 1000, 1, 0);
        Bouncer far = new Bouncer(15_000, 1000, 1, 0);
        World world = world(new Progress(), w -> {
            w.addDroplet(1000, 1000, 1);
            w.addEnemy(near);
            w.addEnemy(far);
        });
        run(world, 0.5);
        assertTrue(near.x() > 2000);
        assertEquals(15_000, far.x());
    }

    @Test
    void theBarriersOnlyStopTheEnemies() {
        World world = world(new Progress(), w -> w.addBarrier(0, 0, 100, 0, 100, 100));
        assertEquals(2, world.barriersNear(50, 0, 10).size());
        assertTrue(world.obstaclesNear(50, 0, 10).isEmpty(), "barriers are not deadly walls");
    }

    @Test
    void theTestRoomPutsTheDropInTheMiddle() {
        World world = new World(Levels.get(Levels.TEST));
        assertEquals(1500, world.player().x());
        assertEquals(1500, world.player().y());
        assertEquals(1, world.playerWaterShare(), 1e-9);
    }

    @Test
    void basinsFullLongEnoughWinTheLevel() {
        World world = world(new Progress(), w -> {
            w.addDroplet(1000, 1000, 1);
            w.addGoal(900, 900, 200, 200, 0.9);
        });
        run(world, 0.5);
        assertFalse(world.isComplete(), "the basin must stay full for a second");
        run(world, 0.6);
        assertTrue(world.isComplete());
    }

    @Test
    void anOpenRoomDrawsEverythingInIt() {
        Progress progress = new Progress(List.of(), List.of("A"), List.of(), 0);
        World world = world(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            w.addCheckpoint(1000, 1000);
            w.addLight(new Light("A", 1200, 1000, false));
            w.addLight(new Light("Fin", 800, 1000, true));
            int door = w.addDoor(new Door(1300, 900, 1300, 1100, false));
            w.addDoor(new Door(700, 900, 700, 1100, true));
            w.addButton(new DoorButton(door, 1000, 1000, false));
            w.addButton(new DoorButton(door, 1000, 1200, true));
            w.addGoal(850, 850, 300, 300, 0.5);
            w.addWall(900, 1250, 1100, 1250);
            w.addEnemy(new Bouncer(1100, 800, 1, 0));
        });
        run(world, 0.1);
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        Rectangle view = new Rectangle(600, 700, 800, 600);
        g.translate(-view.x, -view.y);
        world.draw(g, view);
        g.dispose();
        long lit = Arrays.stream(image.getRGB(0, 0, 800, 600, null, 0, 800)).filter(rgb -> (rgb & 0xFFFFFF) != 0).count();
        assertTrue(lit > 800 * 600 / 2);
        assertEquals(1, world.droplets().size());
        assertEquals(0, world.timeSinceDeath());
        assertFalse(world.isPlayerDead(), "nothing in the room touches the player's drop");
    }

    @Test
    void aLevelWithoutPlanIsReadInWorldPx() {
        World world = new World(Levels.get(Levels.TEST));
        Plan plan = world.planAt(1234, 567);
        assertEquals("Test", plan.name());
        assertEquals(1234, plan.planX(1234, 567), 1e-9);
        assertEquals(567, plan.planY(1234, 567), 1e-9);
    }

    @Test
    void eachPointIsReadInThePlanOfItsZone() {
        World world = new World(Levels.get(Levels.WORLD));
        Plan split = SplitZone.PLAN;
        assertSame(split, world.planAt(split.x(309, 300), split.y(309, 300)));
        assertSame(LightZone.PLAN, world.planAt(LightZone.PLAN.anchorX(), LightZone.PLAN.anchorY()));
        double spokeX = (HubZone.PLAN.x(447, 414) + split.anchorX()) / 2;
        double spokeY = (HubZone.PLAN.y(447, 414) + split.anchorY()) / 2;
        assertSame(HubZone.PLAN, world.planAt(spokeX, spokeY), "the spokes belong to the overview");
    }
}
