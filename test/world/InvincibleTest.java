package world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import enemy.Spinner;
import enemy.ability.Shoot;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class InvincibleTest {
    private static final double FRAME = 1 / 60.0;

    private static World invincible(Progress progress, Consumer<World> layout) {
        World world = new World(new Level("invincible", "", 8000, 8000, layout), progress);
        world.setInvincible(true);
        return world;
    }

    private static Progress withAbilities() {
        return new Progress(List.of(), List.of("Split", "Shoot"), List.of(), 0);
    }

    private static void run(World world, double seconds, int dx) {
        for (int i = 0; i < Math.round(seconds / FRAME); i++) {
            world.update(FRAME, dx, 0);
        }
    }

    @Test
    void theModeIsOffUnlessAsked() {
        Progress progress = new Progress();
        World world = new World(new Level("mortal", "", 8000, 8000, w -> {
            w.addDroplet(1000, 1000, 1);
            w.addWall(1020, 800, 1020, 1200);
        }), progress);
        assertFalse(world.isInvincible());
        run(world, FRAME, 0);
        assertTrue(world.isPlayerDead());
    }

    @Test
    void anInvincibleDropSurvivesAWall() {
        Progress progress = new Progress();
        World world = invincible(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            w.addWall(1020, 800, 1020, 1200);
        });
        assertTrue(world.isInvincible());
        run(world, 1, 0);
        assertFalse(world.isPlayerDead());
        assertEquals(1, world.droplets().size());
        assertEquals(1, progress.hits(), "a long contact counts as one hit");
        assertFalse(world.isComplete());
    }

    @Test
    void eachNewContactCountsAsAHit() {
        Progress progress = new Progress();
        World world = invincible(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            w.addWall(1020, 800, 1020, 1200);
        });
        run(world, 1.5, -1);
        assertTrue(world.player().x() < 900, "the drop moved away from the wall");
        assertEquals(1, progress.hits());
        run(world, 3, 1);
        assertTrue(world.player().x() > 1100, "it went back through the wall");
        assertEquals(2, progress.hits());
        assertFalse(world.isPlayerDead());
    }

    @Test
    void enemiesOnlyHitAnInvincibleDrop() {
        Progress progress = new Progress();
        World world = invincible(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            w.addEnemy(new Spinner(1010, 1000, 80, 90, 0));
        });
        run(world, 0.5, 0);
        assertFalse(world.isPlayerDead());
        assertEquals(1, progress.hits());
    }

    @Test
    void theOtherDropsStillDie() {
        Progress progress = new Progress();
        World world = invincible(progress, w -> {
            w.addDroplet(1000, 1000, 0.5);
            w.addDroplet(2000, 1000, 0.5);
            w.addWall(2010, 800, 2010, 1200);
        });
        run(world, FRAME, 0);
        assertEquals(1, world.droplets().size(), "only the player's drop is protected");
        assertFalse(world.isPlayerDead());
        assertEquals(0, progress.hits());
    }

    @Test
    void aShotChargedTooLongDoesNotBlowAnInvincibleDropUp() {
        Progress progress = withAbilities();
        World world = invincible(progress, w -> w.addDroplet(1000, 1000, 1));
        for (int i = 0; i < Math.round((Shoot.CHARGE_TIME + 0.2) / FRAME); i++) {
            world.holdShot(true, 1, 0);
            world.update(FRAME, 0, 0);
        }
        assertFalse(world.isPlayerDead());
        assertFalse(world.hasExploded());
        assertEquals(1, progress.hits(), "it still counts as a hit");
        assertFalse(world.isChargingShot(), "and the charge is lost");
        assertTrue(world.shots().isEmpty());
        world.holdShot(false, 1, 0);
        world.holdShot(true, 1, 0);
        assertTrue(world.isChargingShot(), "the key let go, the next shot charges");
    }

    @Test
    void aThrownInvincibleDropGoesThroughAWall() {
        Progress progress = withAbilities();
        World world = invincible(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            w.addWall(1300, 800, 1300, 1200);
        });
        for (int i = 0; i < 60; i++) {
            world.holdDash(true);
            world.update(FRAME, 1, 0);
        }
        world.holdDash(false);
        for (int i = 0; i < 30; i++) {
            world.update(0.05, 0, 0);
        }
        assertTrue(world.player().x() > 1300, "the wall didn't hold it back");
        assertEquals(1, progress.hits());
        assertTrue(world.canDash(), "it is still alive");
    }
}
