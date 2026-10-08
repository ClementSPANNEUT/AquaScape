package world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProgressTest {
    @Test
    void aNewGameStartsAtTheFirstCheckpoint() {
        Progress progress = new Progress();
        assertEquals(0, progress.checkpoint());
        assertEquals(0, progress.hits());
        assertFalse(progress.takeChanged());
    }

    @Test
    void touchingACheckpointMakesItTheRestartPoint() {
        Progress progress = new Progress();
        progress.touchCheckpoint(4);
        assertEquals(4, progress.checkpoint());
        assertTrue(progress.hasReached(4));
        assertTrue(progress.takeChanged());
        assertFalse(progress.takeChanged(), "the change is reported once");
    }

    @Test
    void touchingTheSameCheckpointAgainIsNoChange() {
        Progress progress = new Progress();
        progress.touchCheckpoint(2);
        progress.takeChanged();
        progress.touchCheckpoint(2);
        assertFalse(progress.takeChanged());
    }

    @Test
    void theStartOfASavedGameCountsAsReached() {
        Progress progress = new Progress(List.of(1), List.of(), List.of(), 6);
        assertEquals(6, progress.checkpoint());
        assertTrue(progress.hasReached(1));
        assertTrue(progress.hasReached(6));
    }

    @Test
    void lightsKeepTheOrderTheyWereObtainedIn() {
        Progress progress = new Progress();
        progress.obtainLight("Split");
        progress.obtainLight("Dash");
        progress.obtainLight("Split");
        assertEquals(List.of("Split", "Dash"), List.copyOf(progress.lights()));
        assertTrue(progress.hasLight("Dash"));
    }

    @Test
    void lockedDoorsStayOpen() {
        Progress progress = new Progress();
        progress.lock(3);
        assertTrue(progress.isLocked(3));
        assertFalse(progress.isLocked(2));
        assertTrue(progress.takeChanged());
    }

    @Test
    void hitsAreCounted() {
        Progress progress = new Progress();
        progress.hit();
        progress.hit();
        assertEquals(2, progress.hits());
    }
}
