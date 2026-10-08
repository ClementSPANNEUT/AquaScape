package zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import enemy.Enemy;
import enemy.Gater;
import enemy.Spinner;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import world.Checkpoint;
import world.Level;
import world.Light;
import world.Progress;
import world.Wall;
import world.World;

class LevelsTest {
    private static World world;

    @BeforeAll
    static void buildTheWorld() {
        world = new World(Levels.get(Levels.WORLD));
    }

    @Test
    void thereAreTheWorldTheTestRoomAndTheTraining() {
        assertEquals(3, Levels.count());
        assertEquals("Aquascape", Levels.get(Levels.WORLD).name());
        assertEquals("Test", Levels.get(Levels.TEST).name());
        assertEquals("Entraînement : Dash", Levels.get(Levels.TRAINING).name());
    }

    @Test
    void theWorldHasItsCheckpointsAndLights() {
        assertEquals(25, world.checkpoints().size());
        List<String> names = world.lights().stream().map(Light::name).collect(Collectors.toList());
        assertEquals(List.of("Lumière", "Split", "Dash", "Shoot", "Pary", "Fin"), names);
        assertEquals(1, world.lights().stream().filter(Light::isLast).count());
        assertEquals(5, world.lightCount());
        assertEquals("Départ", world.checkpoints().get(0).area());
        assertEquals("Hub", world.checkpoints().get(1).area());
    }

    @Test
    void checkpointsAndLightsAreInsideTheTunnel() {
        for (Checkpoint checkpoint : world.checkpoints()) {
            assertTrue(world.tunnel().contains(checkpoint.x(), checkpoint.y()), checkpoint.area());
        }
        for (Light light : world.lights()) {
            assertTrue(world.tunnel().contains(light.x(), light.y()), light.name());
        }
    }

    @Test
    void noEnemyStartsInTheRockOrOnAWall() {
        assertEnemiesClearOfTheWalls(world);
    }

    @Test
    void theTrainingRoomHasAStartSpinnersAndAnExit() {
        World training = new World(Levels.get(Levels.TRAINING), new Progress(List.of(), List.of("Split"), List.of(), 0));
        assertEquals(1, training.checkpoints().size());
        assertEquals(1, training.lights().size());
        Light exit = training.lights().get(0);
        assertEquals(DashTraining.EXIT, exit.name());
        assertTrue(training.tunnel().contains(exit.x(), exit.y()));
        assertTrue(training.tunnel().contains(training.player().x(), training.player().y()));
        assertEquals(5, training.enemies().size());
        assertTrue(training.enemies().stream().allMatch(enemy -> enemy instanceof Spinner));
        assertEnemiesClearOfTheWalls(training);
        assertTrue(training.canDash(), "the lights brought back make the dash work in the training");
        assertSame(DashTraining.PLAN, training.planAt(training.player().x(), training.player().y()));
        assertEquals(2, training.obstaclesNear(1800, 800, 1).size(), "two closed doors bar the hall");
    }

    private static World trainingFrom(double x, Progress progress) {
        return new World(new Level("Entraînement", "", DashTraining.WIDTH, DashTraining.HEIGHT, w -> {
            w.addDroplet(x, 800, 1);
            Levels.get(Levels.TRAINING).layout().accept(w);
        }), progress);
    }

    @Test
    void theFirstTrainingDoorOpensByStretchingBetweenItsTwoButtons() {
        Progress progress = new Progress(List.of(), List.of("Split"), List.of(), 0);
        World training = trainingFrom(1510, progress);
        for (int frame = 0; frame < 54; frame++) {
            training.holdDash(true);
            training.update(1 / 60.0, 1, 0);
        }
        assertEquals(0, progress.hits(), "the head went through the door held open by the back");
        assertTrue(progress.isLocked(0), "and touched the button that locks it open");
        assertEquals(1, training.obstaclesNear(1800, 800, 1).size(), "only the second door is still closed");
    }

    @Test
    void theSecondTrainingDoorNeedsAHalfLeftOnItsButton() {
        Progress progress = new Progress(List.of(), List.of("Split"), List.of(0), 0);
        World training = trainingFrom(2330, progress);
        for (int frame = 0; frame < 300 && training.droplets().size() == 1; frame++) {
            training.holdDash(true);
            training.update(1 / 60.0, 1, 0);
        }
        assertEquals(2, training.droplets().size(), "the thread is too short to reach the door");
        training.holdDash(false);
        for (int frame = 0; frame < 150 && training.takeNewLight() == null; frame++) {
            training.update(1 / 60.0, 1, 0);
        }
        assertEquals(0, progress.hits(), "the half left on the button kept the door open");
        assertTrue(training.player().x() > 2800, "the other half reached the exit");
    }

    @Test
    void theTrainingRoomIsDugOnlyOnce() {
        World first = new World(Levels.get(Levels.TRAINING));
        World second = new World(Levels.get(Levels.TRAINING));
        assertSame(first.tunnel(), second.tunnel());
    }

    private static void assertEnemiesClearOfTheWalls(World world) {
        for (Enemy enemy : world.enemies()) {
            String name = enemy.getClass().getSimpleName() + " at " + Math.round(enemy.x()) + ", " + Math.round(enemy.y());
            assertTrue(world.tunnel().contains(enemy.x(), enemy.y()), name + " is in the rock");
            if (enemy instanceof Gater) {
                continue;
            }
            double reach = enemy instanceof Spinner ? enemy.radius() + 6 : enemy.radius();
            for (Wall wall : world.tunnel().edges()) {
                assertTrue(!wall.touches(enemy.x(), enemy.y(), reach), name + " touches a wall");
            }
        }
    }

    @Test
    void aDropRestingOnAnyCheckpointIsSafe() {
        for (int checkpoint = 0; checkpoint < world.checkpoints().size(); checkpoint++) {
            Progress progress = new Progress(List.of(), List.of(), List.of(), checkpoint);
            World fresh = new World(Levels.get(Levels.WORLD), progress);
            for (int frame = 0; frame < 180; frame++) {
                fresh.update(1 / 60.0, 0, 0);
            }
            assertEquals(0, progress.hits(), "hit while resting on checkpoint " + checkpoint);
        }
    }
}
