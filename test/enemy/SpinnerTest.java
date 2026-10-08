package enemy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpinnerTest {
    @Test
    void killsAlongItsBarOnly() {
        Spinner spinner = new Spinner(500, 500, 100, 0, 90);
        assertTrue(spinner.touches(580, 500, 5));
        assertTrue(spinner.touches(420, 500, 5), "the bar has two arms");
        assertFalse(spinner.touches(500, 580, 5));
        assertFalse(spinner.touches(620, 500, 5), "nothing past the end of an arm");
    }

    @Test
    void turnsAtItsSpeed() {
        TestArena arena = new TestArena(1000, 1000);
        Spinner spinner = new Spinner(500, 500, 100, 0, 90);
        arena.enemies.add(spinner);
        arena.run(1);
        assertTrue(spinner.touches(500, 580, 5), "a quarter turn after 1 s at 90°/s");
        assertFalse(spinner.touches(580, 500, 5));
    }

    @Test
    void negativeSpeedTurnsTheOtherWay() {
        TestArena arena = new TestArena(1000, 1000);
        Spinner spinner = new Spinner(500, 500, 100, 45, -45);
        arena.enemies.add(spinner);
        arena.run(1);
        assertTrue(spinner.touches(580, 500, 5), "back to 0° after 1 s at -45°/s");
    }
}
