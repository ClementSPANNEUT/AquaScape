package input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GamepadTest {
    private static List<String> record(Gamepad gamepad) {
        List<String> events = new ArrayList<>();
        gamepad.addListener(new GamepadListener() {
            @Override
            public void pressed(GamepadButton button) {
                events.add("+" + button);
            }

            @Override
            public void released(GamepadButton button) {
                events.add("-" + button);
            }
        });
        return events;
    }

    @Test
    void aPressAndAReleaseAreToldOnce() {
        FakeGamepad gamepad = new FakeGamepad();
        List<String> events = record(gamepad);
        gamepad.hold(GamepadButton.A);
        gamepad.poll();
        gamepad.poll();
        assertEquals(List.of("+A"), events, "held for two polls, pressed once");
        assertTrue(gamepad.isHeld(GamepadButton.A));
        gamepad.letGo(GamepadButton.A);
        gamepad.poll();
        assertEquals(List.of("+A", "-A"), events);
        assertFalse(gamepad.isHeld(GamepadButton.A));
    }

    @Test
    void severalButtonsCanChangeAtOnce() {
        FakeGamepad gamepad = new FakeGamepad();
        List<String> events = record(gamepad);
        gamepad.hold(GamepadButton.UP, GamepadButton.Y);
        gamepad.poll();
        gamepad.letGo(GamepadButton.UP);
        gamepad.hold(GamepadButton.START);
        gamepad.poll();
        assertEquals(List.of("+UP", "+Y", "-UP", "+START"), events);
    }

    @Test
    void theDirectionComesFromTheHeldDirections() {
        FakeGamepad gamepad = new FakeGamepad();
        gamepad.hold(GamepadButton.RIGHT, GamepadButton.UP);
        gamepad.poll();
        assertEquals(1, gamepad.directionX());
        assertEquals(-1, gamepad.directionY());
        gamepad.hold(GamepadButton.LEFT, GamepadButton.DOWN);
        gamepad.poll();
        assertEquals(0, gamepad.directionX(), "left and right cancel out");
        assertEquals(0, gamepad.directionY());
    }

    @Test
    void listenersAreToldInTheOrderTheyWereAdded() {
        FakeGamepad gamepad = new FakeGamepad();
        List<String> order = new ArrayList<>();
        gamepad.addListener(button -> order.add("first"));
        gamepad.addListener(button -> order.add("second"));
        gamepad.tap(GamepadButton.B);
        assertEquals(List.of("first", "second"), order);
    }

    @Test
    void withoutAControllerNothingHappens() {
        Gamepad none = Gamepad.none();
        List<String> events = record(none);
        none.poll();
        assertFalse(none.isConnected());
        assertEquals("aucune", none.name());
        assertEquals(0, none.directionX());
        assertEquals(0, none.directionY());
        assertEquals(0, none.aimX());
        assertEquals(0, none.aimY());
        assertTrue(events.isEmpty());
        none.close();
    }

    @Test
    void aPlayStationControllerNamesItsButtonsItsOwnWay() {
        Gamepad ps5 = new FakeGamepad("PS5 Controller");
        assertTrue(ps5.isPlayStation());
        assertEquals("Croix", ps5.buttonName(GamepadButton.A));
        assertEquals("Rond", ps5.buttonName(GamepadButton.B));
        assertEquals("Options", ps5.buttonName(GamepadButton.START));
        assertEquals("R2", ps5.buttonName(GamepadButton.RT));
        assertTrue(new FakeGamepad("DualSense Wireless Controller").isPlayStation());

        Gamepad xbox = new FakeGamepad("Xbox Series Controller");
        assertFalse(xbox.isPlayStation());
        assertEquals("A", xbox.buttonName(GamepadButton.A));
        assertEquals("RT", xbox.buttonName(GamepadButton.RT));
        assertEquals("A", Gamepad.none().buttonName(GamepadButton.A));
    }

    @Test
    void onlyTheFourDirectionsAreDirections() {
        for (GamepadButton button : GamepadButton.values()) {
            boolean direction = button == GamepadButton.UP || button == GamepadButton.DOWN
                    || button == GamepadButton.LEFT || button == GamepadButton.RIGHT;
            assertEquals(direction, button.isDirection(), button.name());
        }
    }

    @Test
    void openingTheRealControllersNeverFails() {
        Gamepad gamepad = Gamepad.open();
        assertNotNull(gamepad);
        gamepad.poll();
        assertNotNull(gamepad.name());
        gamepad.close();
    }
}
