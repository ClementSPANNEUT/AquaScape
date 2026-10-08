package enemy.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fluid.Droplet;
import org.junit.jupiter.api.Test;

class SlideTest {
    private static final double FRAME = 1 / 60.0;
    private static final int BIG = 100_000;

    private final Slide slide = new Slide();

    private static Droplet movingDrop() {
        Droplet drop = new Droplet(5000, 5000, 30);
        for (int i = 0; i < 40; i++) {
            drop.update(FRAME, 820, 0, BIG, BIG);
        }
        return drop;
    }

    private void tap(Droplet drop, boolean allowed) {
        slide.hold(drop, true, allowed);
        slide.hold(drop, false, allowed);
    }

    @Test
    void aPressMakesTheDropSlideAndAnotherOneStopsIt() {
        Droplet drop = movingDrop();
        tap(drop, true);
        assertTrue(drop.isSliding());
        tap(drop, true);
        assertFalse(drop.isSliding());
        tap(drop, true);
        assertTrue(drop.isSliding());
    }

    @Test
    void holdingTheKeyCountsAsOnePress() {
        Droplet drop = movingDrop();
        for (int frame = 0; frame < 120; frame++) {
            slide.hold(drop, true, true);
            drop.update(FRAME, 0, 0, BIG, BIG);
        }
        assertTrue(drop.isSliding(), "it didn't turn off again while the key stayed down");
    }

    @Test
    void theSlideKeepsTheSpeedOfThePress() {
        Droplet drop = movingDrop();
        double speed = drop.speed();
        tap(drop, true);
        for (int frame = 0; frame < 300; frame++) {
            slide.hold(drop, false, true);
            drop.update(FRAME, 0, 0, BIG, BIG);
        }
        assertEquals(speed, drop.speed(), 1e-9);
    }

    @Test
    void aPlayerWhoHasNotLearntItCannotSlide() {
        Droplet drop = movingDrop();
        tap(drop, false);
        assertFalse(drop.isSliding());
    }

    @Test
    void aDropThatSlidesCanAlwaysStop() {
        Droplet drop = movingDrop();
        tap(drop, true);
        tap(drop, false);
        assertFalse(drop.isSliding(), "even if the ability is no longer allowed");
    }

    @Test
    void aDropAtRestHasNoDirectionToKeep() {
        Droplet still = new Droplet(5000, 5000, 30);
        tap(still, true);
        assertFalse(still.isSliding());

        Droplet slow = new Droplet(5000, 5000, 30);
        while (slow.speed() < Slide.MIN_SPEED / 2) {
            slow.update(FRAME, 820, 0, BIG, BIG);
        }
        assertTrue(slow.speed() < Slide.MIN_SPEED);
        tap(slow, true);
        assertFalse(slow.isSliding(), "too slow at " + slow.speed() + " px/s");
    }

    @Test
    void aKeyHeldWhenTheGameResumesDoesNotCount() {
        Droplet drop = movingDrop();
        slide.waitForRelease();
        slide.hold(drop, true, true);
        slide.hold(drop, true, true);
        assertFalse(drop.isSliding(), "the button that closed the pop-up is still down");
        slide.hold(drop, false, true);
        slide.hold(drop, true, true);
        assertTrue(drop.isSliding(), "let go then pressed again");
    }

    @Test
    void theSlideGoesOnThroughAPause() {
        Droplet drop = movingDrop();
        tap(drop, true);
        slide.waitForRelease();
        slide.hold(drop, false, true);
        assertTrue(drop.isSliding());
    }
}
