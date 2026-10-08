package enemy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GaterTest {
    private static final double PERIOD = 3.3;
    private static final double STEP = 1 / 120.0;

    @Test
    void closesAndOpensTheCorridorInRhythm() {
        TestArena arena = new TestArena(1000, 1000);
        Gater gater = new Gater(500, 400, 500, 600);
        arena.enemies.add(gater);
        boolean seenOpen = false;
        boolean seenClosed = false;
        for (double t = 0; t < PERIOD; t += STEP) {
            arena.run(STEP);
            if (gater.touches(500, 500, 1)) {
                seenClosed = true;
            } else {
                seenOpen = true;
            }
        }
        assertTrue(seenOpen, "the middle of the corridor is free part of the time");
        assertTrue(seenClosed, "and blocked the rest of the time");
    }

    @Test
    void itsJawsAlwaysCoverTheWalls() {
        TestArena arena = new TestArena(1000, 1000);
        Gater gater = new Gater(500, 400, 500, 600);
        arena.enemies.add(gater);
        for (double t = 0; t < PERIOD; t += STEP) {
            arena.run(STEP);
            assertTrue(gater.touches(500, 405, 1));
            assertTrue(gater.touches(500, 595, 1));
        }
    }

    @Test
    void neverTouchesBesideTheCorridor() {
        TestArena arena = new TestArena(1000, 1000);
        Gater gater = new Gater(500, 400, 500, 600);
        arena.enemies.add(gater);
        for (double t = 0; t < PERIOD; t += STEP) {
            arena.run(STEP);
            assertFalse(gater.touches(600, 500, 5));
        }
    }
}
