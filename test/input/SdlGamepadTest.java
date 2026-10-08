package input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SdlGamepadTest {
    @Test
    void aStickPushedDownALittleSidewaysOnlyGoesDown() {
        assertEquals(GamepadButton.DOWN, SdlGamepad.dominant(null, 0.6f, 0.8f));
        assertEquals(GamepadButton.DOWN, SdlGamepad.dominant(null, -0.6f, 0.8f));
        assertEquals(GamepadButton.UP, SdlGamepad.dominant(null, 0.55f, -0.7f));
    }

    @Test
    void aStickPushedMostlySidewaysGoesSideways() {
        assertEquals(GamepadButton.RIGHT, SdlGamepad.dominant(null, 0.8f, 0.6f));
        assertEquals(GamepadButton.LEFT, SdlGamepad.dominant(null, -0.9f, -0.3f));
    }

    @Test
    void anExactDiagonalCountsAsUpOrDown() {
        assertEquals(GamepadButton.DOWN, SdlGamepad.dominant(null, 1, 1));
        assertEquals(GamepadButton.UP, SdlGamepad.dominant(null, -1, -1));
    }

    @Test
    void theDirectionHeldStaysWhileItIsPushed() {
        assertEquals(GamepadButton.DOWN, SdlGamepad.dominant(GamepadButton.DOWN, 0.8f, 0.6f),
                "rolling the stick toward the right doesn't add a push to the right");
        assertEquals(GamepadButton.RIGHT, SdlGamepad.dominant(GamepadButton.DOWN, 0.8f, 0.4f));
    }

    @Test
    void nothingIsPushedInsideTheDeadZone() {
        assertNull(SdlGamepad.dominant(null, 0.3f, -0.4f));
        assertNull(SdlGamepad.dominant(GamepadButton.UP, 0, -0.2f));
    }

    @Test
    void theArrowsWinOverTheStick() {
        assertEquals(1, SdlGamepad.axis(false, true, -0.9f));
        assertEquals(-1, SdlGamepad.axis(true, false, 0.9f));
        assertEquals(0.4f, SdlGamepad.axis(false, false, 0.4f));
        assertEquals(0.4f, SdlGamepad.axis(true, true, 0.4f), "both arrows cancel out");
    }

    @Test
    void theRightStickAimsOnlyOnceItIsPushed() {
        assertEquals(0, SdlGamepad.aim(0.2f, 0.1f), "a stick at rest drifts a little");
        assertEquals(0.2f, SdlGamepad.aim(0.2f, 0.9f), 1e-6, "pushed down, its small sideways part counts");
        assertEquals(-0.8f, SdlGamepad.aim(-0.8f, 0), 1e-6);
    }

    @Test
    void movingKeepsTheDiagonals() {
        assertEquals(1, SdlGamepad.step(0.7f));
        assertEquals(-1, SdlGamepad.step(-0.7f));
        assertEquals(0, SdlGamepad.step(0.5f));
    }
}
