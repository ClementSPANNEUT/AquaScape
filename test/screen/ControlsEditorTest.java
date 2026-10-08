package screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import input.FakeGamepad;
import input.GamepadButton;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import save.Controls;
import save.Controls.Action;
import save.WindowSize;

class ControlsEditorTest {
    private static final int FIRST_CELL_X = 150 + 14 + 10;
    private static final int FIRST_CELL_Y = 34 + 2 + 10;
    private static final int CELL_STEP = 150 + 14;
    private static final int ROW_STEP = 28;
    private static final int DASH_ROW = Action.DASH.ordinal();

    private Controls controls;
    private ControlsEditor editor;
    private int changes;
    private int closes;

    @BeforeEach
    void open() {
        controls = new Controls();
        editor = new ControlsEditor(controls, new FakeGamepad("PS5 Controller"), () -> changes++, () -> closes++);
        editor.open();
    }

    private void press(GamepadButton button, int times) {
        for (int i = 0; i < times; i++) {
            editor.gamepadPressed(button);
        }
    }

    private void selectTheControllerButtonOfTheDash() {
        press(GamepadButton.DOWN, DASH_ROW);
        press(GamepadButton.RIGHT, 2);
    }

    @Test
    void entersANewKeyForTheSelectedAction() {
        editor.keyPressed(KeyEvent.VK_ENTER);
        editor.keyPressed(KeyEvent.VK_I);
        assertEquals(KeyEvent.VK_I, controls.key(Action.UP, 0));
        assertEquals(1, changes);
    }

    @Test
    void movesBetweenActionsAndSlots() {
        editor.keyPressed(KeyEvent.VK_DOWN);
        editor.keyPressed(KeyEvent.VK_RIGHT);
        editor.keyPressed(KeyEvent.VK_ENTER);
        editor.keyPressed(KeyEvent.VK_K);
        assertEquals(KeyEvent.VK_K, controls.key(Action.DOWN, 1));
    }

    @Test
    void escapeCancelsTheKeyBeingEntered() {
        editor.keyPressed(KeyEvent.VK_ENTER);
        editor.keyPressed(KeyEvent.VK_ESCAPE);
        assertEquals(KeyEvent.VK_UP, controls.key(Action.UP, 0));
        assertEquals(0, changes);
        assertEquals(0, closes, "the first Échap only cancels");
        editor.keyPressed(KeyEvent.VK_ESCAPE);
        assertEquals(1, closes);
    }

    @Test
    void deleteEmptiesTheSelectedSlot() {
        editor.keyPressed(KeyEvent.VK_DELETE);
        assertEquals(Controls.NONE, controls.key(Action.UP, 0));
        assertEquals(1, changes);
    }

    @Test
    void takingAKeyFromAnotherActionSaysSo() {
        editor.keyPressed(KeyEvent.VK_ENTER);
        editor.keyPressed(KeyEvent.VK_DOWN);
        assertEquals(KeyEvent.VK_DOWN, controls.key(Action.UP, 0));
        assertEquals(Controls.NONE, controls.key(Action.DOWN, 0), "the key moved from Bas to Haut");
        BufferedImage withMessage = paint();
        editor.open();
        assertFalse(Swing.same(withMessage, paint()), "the message is gone once the table reopens");
    }

    @Test
    void theMouseSelectsAndActivatesACell() {
        paint();
        editor.mouseMoved(FIRST_CELL_X + CELL_STEP, FIRST_CELL_Y);
        editor.mousePressed(FIRST_CELL_X + CELL_STEP, FIRST_CELL_Y + ROW_STEP);
        BufferedImage capturing = paint();
        editor.mouseMoved(FIRST_CELL_X, FIRST_CELL_Y);
        editor.keyPressed(KeyEvent.VK_L);
        assertEquals(KeyEvent.VK_L, controls.key(Action.DOWN, 1), "moving the mouse while a key is awaited changes nothing");
        assertFalse(Swing.same(capturing, paint()));

        editor.mousePressed(FIRST_CELL_X, FIRST_CELL_Y);
        editor.mousePressed(1, 1);
        editor.keyPressed(KeyEvent.VK_M);
        assertEquals(KeyEvent.VK_UP, controls.key(Action.UP, 0), "a second click cancels the key being entered");
        editor.mousePressed(1, 1);
        assertEquals(0, closes);
    }

    @Test
    void theMouseReachesTheControllerColumn() {
        paint();
        editor.mousePressed(FIRST_CELL_X + 2 * CELL_STEP, FIRST_CELL_Y + DASH_ROW * ROW_STEP);
        editor.gamepadPressed(GamepadButton.RB);
        assertEquals(GamepadButton.RB, controls.button(Action.DASH));
    }

    @Test
    void theButtonRowCannotBeEmptied() {
        editor.keyPressed(KeyEvent.VK_UP);
        editor.keyPressed(KeyEvent.VK_BACK_SPACE);
        editor.keyPressed(KeyEvent.VK_F1);
        assertEquals(0, changes);
        assertEquals(KeyEvent.VK_UP, controls.key(Action.UP, 0));
    }

    @Test
    void theTableFitsTheSmallestWindow() {
        int padding = 28;
        assertEquals(12, Action.values().length, "a new action adds a row: check the table still fits");
        assertTrue(ControlsEditor.HEIGHT + Popup.HEADER_HEIGHT + padding <= WindowSize.SMALL.height(),
                "the pop-up would be " + (ControlsEditor.HEIGHT + Popup.HEADER_HEIGHT + padding) + " px high");
        assertTrue(ControlsEditor.WIDTH + 2 * padding <= WindowSize.SMALL.width());
    }

    private BufferedImage paint() {
        BufferedImage image = new BufferedImage(ControlsEditor.WIDTH, ControlsEditor.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        editor.paint(g, new Rectangle(0, 0, ControlsEditor.WIDTH, ControlsEditor.HEIGHT));
        g.dispose();
        return image;
    }

    @Test
    void theLastRowResetsOrCloses() {
        controls.set(Action.LEFT, 0, KeyEvent.VK_J);
        controls.setButton(Action.DASH, GamepadButton.LB);
        editor.keyPressed(KeyEvent.VK_UP);
        editor.keyPressed(KeyEvent.VK_ENTER);
        assertEquals(KeyEvent.VK_LEFT, controls.key(Action.LEFT, 0), "Par défaut");
        assertEquals(GamepadButton.A, controls.button(Action.DASH), "Par défaut also resets the controller");
        editor.keyPressed(KeyEvent.VK_RIGHT);
        editor.keyPressed(KeyEvent.VK_ENTER);
        assertEquals(1, closes, "Retour");
    }

    @Test
    void theControllerBindsTheButtonPressedNext() {
        selectTheControllerButtonOfTheDash();
        editor.gamepadPressed(GamepadButton.A);
        editor.gamepadPressed(GamepadButton.X);
        assertEquals(GamepadButton.X, controls.button(Action.DASH));
        assertEquals(KeyEvent.VK_SPACE, controls.key(Action.DASH, 0), "the keys are left alone");
        assertEquals(1, changes);
        assertEquals(0, closes);
    }

    @Test
    void theButtonThatValidatesCanBeBoundToo() {
        selectTheControllerButtonOfTheDash();
        controls.setButton(Action.DASH, GamepadButton.RT);
        editor.gamepadPressed(GamepadButton.A);
        editor.gamepadPressed(GamepadButton.B);
        assertEquals(GamepadButton.B, controls.button(Action.DASH), "B is bound instead of leaving the table");
        assertEquals(0, closes);
    }

    @Test
    void takingAButtonFromAnotherActionSaysSo() {
        selectTheControllerButtonOfTheDash();
        BufferedImage before = paint();
        editor.gamepadPressed(GamepadButton.A);
        editor.gamepadPressed(GamepadButton.Y);
        assertEquals(GamepadButton.Y, controls.button(Action.DASH));
        assertNull(controls.button(Action.RESTART), "the button moved from Recommencer to Dash");
        assertFalse(Swing.same(before, paint()));
    }

    @Test
    void theStickDoesNothingWhileAButtonIsAwaited() {
        selectTheControllerButtonOfTheDash();
        editor.gamepadPressed(GamepadButton.A);
        editor.gamepadPressed(GamepadButton.DOWN);
        editor.gamepadPressed(GamepadButton.LEFT);
        editor.gamepadPressed(GamepadButton.RB);
        assertEquals(GamepadButton.RB, controls.button(Action.DASH), "the table still waited for a button");
    }

    @Test
    void startCancelsTheButtonBeingEntered() {
        selectTheControllerButtonOfTheDash();
        editor.gamepadPressed(GamepadButton.A);
        editor.gamepadPressed(GamepadButton.START);
        assertEquals(GamepadButton.A, controls.button(Action.DASH));
        assertEquals(0, changes);
        assertEquals(0, closes, "the first Start only cancels");
        editor.gamepadPressed(GamepadButton.START);
        assertEquals(1, closes);
    }

    @Test
    void aKeyCancelsTheButtonBeingEntered() {
        selectTheControllerButtonOfTheDash();
        editor.gamepadPressed(GamepadButton.A);
        editor.keyPressed(KeyEvent.VK_K);
        editor.gamepadPressed(GamepadButton.RB);
        assertEquals(GamepadButton.A, controls.button(Action.DASH));
        assertEquals(KeyEvent.VK_SPACE, controls.key(Action.DASH, 0));
        assertEquals(0, changes);
    }

    @Test
    void aControllerButtonIsNeverBoundAsAKey() {
        editor.gamepadPressed(GamepadButton.A);
        editor.gamepadPressed(GamepadButton.A);
        assertEquals(KeyEvent.VK_UP, controls.key(Action.UP, 0), "pressing A twice used to bind Entrée");
        assertEquals(0, changes);
        editor.keyPressed(KeyEvent.VK_I);
        assertEquals(KeyEvent.VK_UP, controls.key(Action.UP, 0), "the second A cancelled the key being entered");
    }

    @Test
    void theControllerCanStartEnteringAKey() {
        editor.gamepadPressed(GamepadButton.A);
        editor.keyPressed(KeyEvent.VK_I);
        assertEquals(KeyEvent.VK_I, controls.key(Action.UP, 0));
    }

    @Test
    void theStickOfAMovementCantBeChanged() {
        press(GamepadButton.RIGHT, 2);
        editor.gamepadPressed(GamepadButton.A);
        editor.gamepadPressed(GamepadButton.X);
        assertNull(controls.button(Action.UP));
        assertEquals(0, changes);
        editor.gamepadPressed(GamepadButton.B);
        assertEquals(1, closes, "nothing was awaited, so B leaves");
    }

    @Test
    void theRightStickOfTheAimCantBeChangedEither() {
        press(GamepadButton.DOWN, Action.AIM_LEFT.ordinal());
        press(GamepadButton.RIGHT, 2);
        BufferedImage before = paint();
        editor.gamepadPressed(GamepadButton.A);
        editor.gamepadPressed(GamepadButton.X);
        assertNull(controls.button(Action.AIM_LEFT));
        assertEquals(0, changes);
        assertFalse(Swing.same(before, paint()), "a message says the right stick always aims");
    }

    @Test
    void anAimKeyCanBeTheSameAsAMovementKey() {
        press(GamepadButton.DOWN, Action.AIM_LEFT.ordinal());
        editor.keyPressed(KeyEvent.VK_ENTER);
        editor.keyPressed(KeyEvent.VK_Q);
        assertEquals(KeyEvent.VK_Q, controls.key(Action.AIM_LEFT, 0));
        assertEquals(KeyEvent.VK_Q, controls.key(Action.LEFT, 1), "Q still moves the drop left");
    }

    @Test
    void xEmptiesTheSelectedCell() {
        selectTheControllerButtonOfTheDash();
        editor.gamepadPressed(GamepadButton.X);
        assertNull(controls.button(Action.DASH));
        editor.gamepadPressed(GamepadButton.LEFT);
        editor.gamepadPressed(GamepadButton.LEFT);
        editor.gamepadPressed(GamepadButton.X);
        assertEquals(Controls.NONE, controls.key(Action.DASH, 0));
        assertEquals(2, changes);
    }

    @Test
    void theControllerReachesTheLastRow() {
        press(GamepadButton.RIGHT, 2);
        editor.gamepadPressed(GamepadButton.UP);
        editor.gamepadPressed(GamepadButton.A);
        assertEquals(1, closes, "from the last column, up goes to Retour");
    }

    @Test
    void theColumnsWrapAround() {
        editor.gamepadPressed(GamepadButton.LEFT);
        press(GamepadButton.DOWN, DASH_ROW);
        editor.gamepadPressed(GamepadButton.A);
        editor.gamepadPressed(GamepadButton.L3);
        assertEquals(GamepadButton.L3, controls.button(Action.DASH), "left of the first column is the controller");
    }
}
