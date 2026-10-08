package enemy.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fluid.Droplet;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashTest {
    private static final double FRAME = 1 / 60.0;
    private static final int BIG = 100_000;

    private static void stretch(Dash dash, Droplet drop, int frames) {
        for (int i = 0; i < frames; i++) {
            dash.hold(drop, true, true);
            drop.update(FRAME, 820, 0, BIG, BIG);
        }
    }

    @Test
    void holdingTheKeySticksTheDropAndLettingGoThrowsIt() {
        Dash dash = new Dash();
        Droplet drop = new Droplet(1000, 1000, 30);
        stretch(dash, drop, 50);
        assertTrue(drop.isAnchored());
        dash.hold(drop, false, true);
        assertFalse(drop.isAnchored());
        assertTrue(drop.vx() > 400, "thrown toward its head");
    }

    @Test
    void aPlayerWhoHasNotLearntItCannotDash() {
        Dash dash = new Dash();
        Droplet drop = new Droplet(1000, 1000, 30);
        dash.hold(drop, true, false);
        assertFalse(drop.isAnchored());
    }

    @Test
    void aCancelledDashIsNotThrownAndNeedsTheKeyLetGo() {
        Dash dash = new Dash();
        Droplet drop = new Droplet(1000, 1000, 30);
        stretch(dash, drop, 50);
        double speed = drop.speed();
        dash.cancel(drop);
        assertFalse(drop.isAnchored());
        assertEquals(speed, drop.speed(), 1e-9);
        dash.hold(drop, true, true);
        assertFalse(drop.isAnchored(), "the key is still held");
        dash.hold(drop, false, true);
        dash.hold(drop, true, true);
        assertTrue(drop.isAnchored());
    }

    @Test
    void tearingGivesTheTwoHalvesAndNeedsTheKeyLetGo() {
        Dash dash = new Dash();
        Droplet drop = new Droplet(1000, 1000, 30);
        for (int i = 0; i < 300 && !drop.isTearing(); i++) {
            stretch(dash, drop, 1);
        }
        List<Droplet> halves = dash.tear(drop);
        assertEquals(2, halves.size());
        assertTrue(halves.get(0).x() > halves.get(1).x(), "the head comes first");
        dash.hold(halves.get(0), true, true);
        assertFalse(halves.get(0).isAnchored(), "the key is still held");
    }
}
