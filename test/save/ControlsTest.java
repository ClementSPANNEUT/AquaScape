package save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import input.GamepadButton;
import java.awt.event.KeyEvent;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;
import save.Controls.Action;

class ControlsTest {
    @Test
    void defaultsAreTheArrowsAndZqsd() {
        Controls controls = new Controls();
        assertEquals(KeyEvent.VK_UP, controls.key(Action.UP, 0));
        assertEquals(KeyEvent.VK_Z, controls.key(Action.UP, 1));
        assertEquals("↑ ← ↓ → / Z Q S D", controls.describeMovement());
        assertEquals("R", controls.describe(Action.RESTART));
    }

    @Test
    void thereIsNoBoostAction() {
        for (Action action : Action.values()) {
            assertFalse(action.name().equals("BOOST"), "the boost was removed from the game");
        }
        assertNull(new Controls().set(Action.UP, 1, KeyEvent.VK_SHIFT), "Maj is free for any action");
        Properties oldSave = new Properties();
        oldSave.setProperty("controls.BOOST", KeyEvent.VK_SHIFT + ",0");
        Controls controls = new Controls();
        controls.load(oldSave);
        assertEquals(KeyEvent.VK_UP, controls.key(Action.UP, 0), "an old save with a boost key still loads");
    }

    @Test
    void aKeyLeavesTheActionThatHadIt() {
        Controls controls = new Controls();
        Action previous = controls.set(Action.DOWN, 0, KeyEvent.VK_Z);
        assertEquals(Action.UP, previous);
        assertTrue(controls.matches(Action.DOWN, KeyEvent.VK_Z));
        assertFalse(controls.matches(Action.UP, KeyEvent.VK_Z));
        assertEquals(Controls.NONE, controls.key(Action.UP, 1));
    }

    @Test
    void rebindingTheSameKeyChangesNothingElse() {
        Controls controls = new Controls();
        assertNull(controls.set(Action.UP, 0, KeyEvent.VK_UP));
        assertNull(controls.set(Action.UP, 1, KeyEvent.VK_I));
        assertEquals("↑ / I", controls.describe(Action.UP));
    }

    @Test
    void escapeAndEmptyKeysCantBeBound() {
        assertFalse(Controls.canBind(KeyEvent.VK_ESCAPE));
        assertFalse(Controls.canBind(Controls.NONE));
        assertTrue(Controls.canBind(KeyEvent.VK_A));
    }

    @Test
    void anActionIsPressedWhenOneOfItsKeysIsHeld() {
        Controls controls = new Controls();
        assertTrue(controls.isPressed(Action.UP, Set.of(KeyEvent.VK_Z)));
        assertTrue(controls.isPressed(Action.UP, Set.of(KeyEvent.VK_UP)));
        assertFalse(controls.isPressed(Action.DOWN, Set.of(KeyEvent.VK_Z)));
    }

    @Test
    void clearingAllTheKeysShowsADash() {
        Controls controls = new Controls();
        controls.clear(Action.RESTART, 0);
        assertEquals("—", controls.describe(Action.RESTART));
        assertFalse(controls.matches(Action.RESTART, Controls.NONE));
    }

    @Test
    void resetPutsBackTheDefaults() {
        Controls controls = new Controls();
        controls.set(Action.LEFT, 0, KeyEvent.VK_J);
        controls.reset();
        assertEquals(KeyEvent.VK_LEFT, controls.key(Action.LEFT, 0));
    }

    @Test
    void keysSurviveSavingAndLoading() {
        Controls controls = new Controls();
        controls.set(Action.UP, 0, KeyEvent.VK_I);
        controls.clear(Action.RESTART, 0);
        Properties properties = new Properties();
        controls.store(properties);

        Controls loaded = new Controls();
        loaded.load(properties);
        for (Action action : Action.values()) {
            for (int slot = 0; slot < Controls.SLOTS; slot++) {
                assertEquals(controls.key(action, slot), loaded.key(action, slot), action + " slot " + slot);
            }
        }
    }

    @Test
    void actionsMissingFromTheSaveKeepTheirKeys() {
        Controls controls = new Controls();
        controls.load(new Properties());
        assertEquals(KeyEvent.VK_G, controls.key(Action.GRID, 0));
    }

    @Test
    void theSlideIsOnFAndTheRightTriggerByDefault() {
        Controls controls = new Controls();
        assertEquals(KeyEvent.VK_F, controls.key(Action.SLIDE, 0));
        assertEquals(Controls.NONE, controls.key(Action.SLIDE, 1));
        assertEquals(GamepadButton.RT, controls.button(Action.SLIDE));
        assertEquals("Slide", Action.SLIDE.label());
        assertFalse(Action.SLIDE.usesStick());
    }

    @Test
    void aSaveWrittenBeforeAnActionExistedGivesItItsDefaults() {
        Properties oldSave = new Properties();
        oldSave.setProperty("controls.RESTART", KeyEvent.VK_R + ",0");
        oldSave.setProperty("gamepad.RESTART", "RB");
        oldSave.setProperty("gamepad.GRID", "LB");
        Controls controls = new Controls();
        controls.load(oldSave);
        assertEquals(KeyEvent.VK_F, controls.key(Action.SLIDE, 0));
        assertEquals(GamepadButton.RT, controls.button(Action.SLIDE));
        assertEquals(GamepadButton.RB, controls.button(Action.RESTART));
    }

    @Test
    void anActionNewToASaveGivesUpTheKeyAndTheButtonTheSaveGaveToAnother() {
        Properties oldSave = new Properties();
        oldSave.setProperty("controls.SHOOT", KeyEvent.VK_F + ",0");
        oldSave.setProperty("gamepad.RESTART", "RT");
        Controls controls = new Controls();
        controls.load(oldSave);
        assertEquals(KeyEvent.VK_F, controls.key(Action.SHOOT, 0), "the player's choice stays");
        assertEquals(Controls.NONE, controls.key(Action.SLIDE, 0), "so the slide has no key until one is chosen");
        assertEquals(GamepadButton.RT, controls.button(Action.RESTART));
        assertNull(controls.button(Action.SLIDE));
        assertEquals(KeyEvent.VK_G, controls.key(Action.GRID, 0), "the other defaults are untouched");
        assertEquals(KeyEvent.VK_LEFT, controls.key(Action.AIM_LEFT, 0), "a key shared by moving and aiming stays");
        assertEquals(KeyEvent.VK_LEFT, controls.key(Action.LEFT, 0));
    }

    @Test
    void theSlideBindingsAreSavedLikeTheOthers() {
        Controls controls = new Controls();
        controls.set(Action.SLIDE, 0, KeyEvent.VK_C);
        controls.setButton(Action.SLIDE, GamepadButton.B);
        Properties properties = new Properties();
        controls.store(properties);
        Controls loaded = new Controls();
        loaded.load(properties);
        assertEquals(KeyEvent.VK_C, loaded.key(Action.SLIDE, 0));
        assertEquals(GamepadButton.B, loaded.button(Action.SLIDE));
    }

    @Test
    void theDashIsOnSpaceByDefault() {
        Controls controls = new Controls();
        assertEquals(KeyEvent.VK_SPACE, controls.key(Action.DASH, 0));
        assertEquals("Espace", controls.describe(Action.DASH));
    }

    @Test
    void theControllerHasAButtonForEachActionByDefault() {
        Controls controls = new Controls();
        assertEquals(GamepadButton.A, controls.button(Action.DASH));
        assertEquals(GamepadButton.X, controls.button(Action.SWAP));
        assertEquals(GamepadButton.Y, controls.button(Action.RESTART));
        assertEquals(GamepadButton.BACK, controls.button(Action.GRID));
        assertEquals(KeyEvent.VK_E, controls.key(Action.SWAP, 0));
        assertEquals("Changer de goutte", Action.SWAP.label());
        assertEquals(GamepadButton.LT, controls.button(Action.SHOOT));
        assertEquals(KeyEvent.VK_CONTROL, controls.key(Action.SHOOT, 0));
        assertEquals("Ctrl", controls.describe(Action.SHOOT));
        assertTrue(controls.matches(Action.DASH, GamepadButton.A));
        assertFalse(controls.matches(Action.DASH, GamepadButton.B));
        assertFalse(controls.matches(Action.DASH, (GamepadButton) null));
    }

    @Test
    void theMovementsKeepTheStick() {
        Controls controls = new Controls();
        for (Action action : new Action[] {Action.UP, Action.DOWN, Action.LEFT, Action.RIGHT}) {
            assertTrue(action.usesStick(), action.name());
            assertTrue(action.isMovement());
            assertTrue(action.stickLabel().startsWith("Stick "));
            assertNull(controls.button(action), "a stick, not a button");
            assertThrows(IllegalArgumentException.class, () -> controls.setButton(action, GamepadButton.X));
            controls.clearButton(action);
            assertTrue(action.usesStick(), "the stick can't be taken away");
        }
        assertFalse(Action.DASH.usesStick());
        assertNull(Action.DASH.stickLabel());
        assertFalse(Action.DASH.isMovement());
    }

    @Test
    void theAimTurnsWithTwoKeysOrTheRightStick() {
        Controls controls = new Controls();
        assertEquals(KeyEvent.VK_LEFT, controls.key(Action.AIM_LEFT, 0));
        assertEquals(KeyEvent.VK_RIGHT, controls.key(Action.AIM_RIGHT, 0));
        assertEquals("Viser à gauche", Action.AIM_LEFT.label());
        for (Action action : new Action[] {Action.AIM_LEFT, Action.AIM_RIGHT}) {
            assertTrue(action.isAim());
            assertFalse(action.isMovement());
            assertEquals("Stick droit", action.stickLabel());
            assertNull(controls.button(action));
            assertThrows(IllegalArgumentException.class, () -> controls.setButton(action, GamepadButton.X));
        }
        assertFalse(Action.SHOOT.isAim());
    }

    @Test
    void aKeyCanBothMoveTheDropAndTurnTheAim() {
        Controls controls = new Controls();
        assertEquals(KeyEvent.VK_LEFT, controls.key(Action.LEFT, 0), "by default the arrows do both");
        assertTrue(Action.LEFT.sharesKeysWith(Action.AIM_RIGHT));
        assertTrue(Action.AIM_LEFT.sharesKeysWith(Action.UP));
        assertFalse(Action.LEFT.sharesKeysWith(Action.RIGHT));
        assertFalse(Action.AIM_LEFT.sharesKeysWith(Action.AIM_RIGHT));
        assertFalse(Action.AIM_LEFT.sharesKeysWith(Action.SHOOT));

        assertNull(controls.set(Action.AIM_LEFT, 1, KeyEvent.VK_Q), "Q keeps moving the drop left");
        assertTrue(controls.matches(Action.LEFT, KeyEvent.VK_Q));
        assertTrue(controls.matches(Action.AIM_LEFT, KeyEvent.VK_Q));

        assertEquals(Action.AIM_LEFT, controls.set(Action.AIM_RIGHT, 1, KeyEvent.VK_Q), "but only one way of the aim");
        assertFalse(controls.matches(Action.AIM_LEFT, KeyEvent.VK_Q));
        assertTrue(controls.matches(Action.LEFT, KeyEvent.VK_Q));

        controls.set(Action.DASH, 1, KeyEvent.VK_LEFT);
        assertFalse(controls.matches(Action.LEFT, KeyEvent.VK_LEFT), "another action takes the key from both");
        assertFalse(controls.matches(Action.AIM_LEFT, KeyEvent.VK_LEFT));
    }

    @Test
    void aButtonLeavesTheActionThatHadIt() {
        Controls controls = new Controls();
        assertNull(controls.setButton(Action.DASH, GamepadButton.RB), "R1 was free");
        assertEquals(Action.RESTART, controls.setButton(Action.DASH, GamepadButton.Y));
        assertEquals(GamepadButton.Y, controls.button(Action.DASH));
        assertNull(controls.button(Action.RESTART));
        assertNull(controls.setButton(Action.DASH, GamepadButton.Y), "binding the same button changes nothing");
    }

    @Test
    void startAndTheDirectionsCantBeBound() {
        assertFalse(Controls.canBind(GamepadButton.START));
        assertFalse(Controls.canBind(GamepadButton.UP));
        assertFalse(Controls.canBind((GamepadButton) null));
        assertTrue(Controls.canBind(GamepadButton.B));
        assertTrue(Controls.canBind(GamepadButton.L3));
        Controls controls = new Controls();
        assertThrows(IllegalArgumentException.class, () -> controls.setButton(Action.DASH, GamepadButton.START));
    }

    @Test
    void buttonsSurviveSavingAndLoading() {
        Controls controls = new Controls();
        controls.setButton(Action.DASH, GamepadButton.RB);
        controls.clearButton(Action.GRID);
        Properties properties = new Properties();
        controls.store(properties);
        assertNull(properties.getProperty("gamepad.UP"), "the stick isn't saved");

        Controls loaded = new Controls();
        loaded.load(properties);
        assertEquals(GamepadButton.RB, loaded.button(Action.DASH));
        assertEquals(GamepadButton.Y, loaded.button(Action.RESTART));
        assertNull(loaded.button(Action.GRID));
    }

    @Test
    void aSaveWithAButtonThatCantBeBoundKeepsTheDefault() {
        Properties properties = new Properties();
        properties.setProperty("gamepad.DASH", "START");
        properties.setProperty("gamepad.RESTART", "TURBO");
        properties.setProperty("gamepad.UP", "X");
        Controls controls = new Controls();
        controls.load(properties);
        assertEquals(GamepadButton.A, controls.button(Action.DASH));
        assertEquals(GamepadButton.Y, controls.button(Action.RESTART));
        assertNull(controls.button(Action.UP), "the movements never get a button");
        assertTrue(Action.UP.usesStick());
    }

    @Test
    void resetPutsBackTheDefaultButtons() {
        Controls controls = new Controls();
        controls.setButton(Action.RESTART, GamepadButton.A);
        controls.reset();
        assertEquals(GamepadButton.A, controls.button(Action.DASH));
        assertEquals(GamepadButton.Y, controls.button(Action.RESTART));
    }

    @Test
    void keysHaveFrenchNames() {
        assertEquals("Espace", Controls.keyName(KeyEvent.VK_SPACE));
        assertEquals("→", Controls.keyName(KeyEvent.VK_RIGHT));
        assertEquals("—", Controls.keyName(Controls.NONE));
    }
}
