package enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpeederTest {
    private static final double FRAME = 1 / 60.0;

    @Test
    void dashesToTheOtherStopThenWaits() {
        TestArena arena = new TestArena(2000, 2000);
        Speeder speeder = new Speeder(100, 500, 600, 500);
        arena.enemies.add(speeder);
        double time = 0;
        while (speeder.x() < 600 - 1e-6 && time < 3) {
            arena.run(FRAME);
            time += FRAME;
        }
        assertEquals(600, speeder.x(), 1e-6, "reaches the second stop within its wait and its dash");
        assertEquals(500, speeder.y(), 1e-6);
        for (int i = 0; i < 110; i++) {
            arena.run(FRAME);
            assertEquals(600, speeder.x(), 1e-6, "waits about 2 s on the stop");
        }
        arena.run(1.5);
        assertEquals(100, speeder.x(), 1e-6, "then dashes back to the first stop");
    }

    @Test
    void killsOnItsTriangleOnly() {
        Speeder speeder = new Speeder(100, 500, 600, 500);
        double r = speeder.radius();
        assertTrue(speeder.touches(100 + 0.9 * r, 500, 1), "the tip points at the next stop");
        assertTrue(speeder.touches(100, 500, 1), "the centre is inside");
        assertFalse(speeder.touches(100, 500 + 0.7 * r, 1), "inside its radius but outside the triangle");
        assertFalse(speeder.touches(100 - 0.9 * r, 500, 1), "behind the triangle");
    }
}
