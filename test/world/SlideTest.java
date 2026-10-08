package world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import enemy.ability.Ability;
import fluid.Droplet;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class SlideTest {
    private static final double FRAME = 1 / 60.0;

    private static World world(Progress progress, Consumer<World> layout) {
        return new World(new Level("slide", "", 20_000, 8000, layout), progress);
    }

    private static World world() {
        return world(withDash(), w -> w.addDroplet(1000, 1000, 1));
    }

    private static Progress withDash() {
        return new Progress(List.of(), List.of("Split", "Dash"), List.of(), 0);
    }

    private static void run(World world, int frames, int dx, int dy) {
        for (int i = 0; i < frames; i++) {
            world.holdSlide(false);
            world.update(FRAME, dx, dy);
        }
    }

    private static void tap(World world, int dx) {
        world.holdSlide(true);
        world.update(FRAME, dx, 0);
        world.holdSlide(false);
        world.update(FRAME, dx, 0);
    }

    private static World slidingRight() {
        World world = world();
        run(world, 40, 1, 0);
        tap(world, 1);
        assertTrue(world.isSliding());
        return world;
    }

    @Test
    void slidingNeedsTheLightOfDash() {
        World without = world(new Progress(List.of(), List.of("Split"), List.of(), 0), w -> w.addDroplet(1000, 1000, 1));
        assertFalse(without.canSlide());
        run(without, 40, 1, 0);
        tap(without, 1);
        assertFalse(without.isSliding());

        World with = world();
        assertTrue(with.canSlide());
        run(with, 40, 1, 0);
        tap(with, 1);
        assertTrue(with.isSliding());
    }

    @Test
    void takingTheLightOfDashUnlocksTheSlideAtOnce() {
        Progress progress = new Progress();
        World world = world(progress, w -> {
            w.addDroplet(1000, 1000, 1);
            w.addLight(new Light("Dash", 1000, 1000, false));
        });
        assertFalse(world.canSlide());
        run(world, 1, 0, 0);
        assertEquals("Dash", world.takeNewLight());
        assertTrue(progress.hasAbility(Ability.SLIDE));
        assertTrue(world.canSlide());
    }

    @Test
    void theDropKeepsItsSpeedAndItsDirectionWithNoKeyHeld() {
        World world = world();
        run(world, 40, 1, 0);
        double speed = world.player().speed();
        assertTrue(speed > 200);
        world.holdSlide(true);
        double x = world.player().x();
        for (int i = 0; i < 180; i++) {
            world.update(FRAME, 0, 0);
            world.holdSlide(false);
        }
        assertEquals(speed, world.player().speed(), 1e-9, "three seconds later, with no key held");
        assertEquals(0, world.player().vy(), 1e-9);
        assertEquals(1000, world.player().y(), 1e-6);
        assertEquals(x + speed * 3, world.player().x(), 1e-6);

        World coasting = world();
        run(coasting, 40, 1, 0);
        run(coasting, 180, 0, 0);
        assertTrue(coasting.player().speed() < 10, "without the slide, the drop has nearly stopped");
    }

    @Test
    void steeringDoesNotTurnASlidingDrop() {
        World world = slidingRight();
        double speed = world.player().vx();
        run(world, 60, -1, 1);
        assertEquals(speed, world.player().vx(), 1e-9);
        assertEquals(0, world.player().vy(), 1e-9);
    }

    @Test
    void aSecondPressGivesTheControlBack() {
        World world = slidingRight();
        double speed = world.player().speed();
        tap(world, 0);
        assertFalse(world.isSliding());
        run(world, 60, 0, 1);
        assertTrue(world.player().vx() < speed / 2, "it slows down again");
        assertTrue(world.player().vy() > 100, "and it can be steered");
    }

    @Test
    void holdingTheKeyCountsAsOnePress() {
        World world = world();
        run(world, 40, 1, 0);
        for (int i = 0; i < 90; i++) {
            world.holdSlide(true);
            world.update(FRAME, 0, 0);
        }
        assertTrue(world.isSliding());
    }

    @Test
    void aDropAtRestDoesNotStartSliding() {
        World world = world();
        tap(world, 0);
        assertFalse(world.isSliding());
        run(world, 30, 1, 0);
        assertTrue(world.player().x() > 1000, "the drop can still be steered");
    }

    @Test
    void stickingTheBackForADashEndsTheSlide() {
        World world = slidingRight();
        world.holdDash(true);
        world.update(FRAME, 1, 0);
        assertTrue(world.player().isAnchored());
        assertFalse(world.isSliding());
    }

    @Test
    void theSlideKeepsTheSpeedOfADash() {
        World world = world();
        for (int i = 0; i < 300 && world.dashTension() < 0.7; i++) {
            world.holdDash(true);
            world.update(FRAME, 1, 0);
        }
        world.holdDash(false);
        world.holdSlide(true);
        double speed = world.player().speed();
        assertTrue(speed > 500, "thrown at " + speed + " px/s");
        assertTrue(world.isSliding());
        for (int i = 0; i < 240; i++) {
            world.holdDash(false);
            world.holdSlide(false);
            world.update(FRAME, 0, 0);
        }
        assertEquals(speed, world.player().speed(), 1e-9, "four seconds later");
        assertEquals(1, world.droplets().size(), "the bubble holds together at that speed");
    }

    @Test
    void takingAnotherDropEndsTheSlide() {
        World world = world();
        for (int i = 0; i < 300 && world.droplets().size() == 1; i++) {
            world.holdDash(true);
            world.update(FRAME, 1, 0);
        }
        assertEquals(2, world.droplets().size(), "pulled long enough, the drop tears");
        world.holdDash(false);
        Droplet head = world.player();
        assertTrue(head.speed() >= enemy.ability.Slide.MIN_SPEED, "the head keeps its speed: " + head.speed());
        world.holdSlide(true);
        assertTrue(head.isSliding());

        assertTrue(world.switchDrop());
        assertFalse(head.isSliding(), "the drop left behind slows down");
        assertFalse(world.isSliding());
    }

    @Test
    void aSlidingDropStillDiesOnAWall() {
        World world = world(withDash(), w -> {
            w.addDroplet(1000, 1000, 1);
            w.addWall(1600, 800, 1600, 1200);
        });
        run(world, 40, 1, 0);
        tap(world, 1);
        for (int i = 0; i < 300 && !world.isPlayerDead(); i++) {
            world.holdSlide(false);
            world.update(FRAME, 0, 0);
        }
        assertTrue(world.isPlayerDead());
        assertFalse(world.isSliding());
        assertFalse(world.canSlide());
    }

    @Test
    void theKeyHeldWhenTheGameResumesDoesNotCount() {
        World world = world();
        run(world, 40, 1, 0);
        world.waitForSlideKey();
        world.holdSlide(true);
        world.update(FRAME, 1, 0);
        assertFalse(world.isSliding(), "the button that closed the pop-up is still down");
        world.holdSlide(false);
        tap(world, 1);
        assertTrue(world.isSliding(), "let go then pressed again");
    }
}
