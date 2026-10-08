package screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static screen.Swing.click;
import static screen.Swing.drag;
import static screen.Swing.leave;
import static screen.Swing.move;
import static screen.Swing.onEdt;
import static screen.Swing.paint;
import static screen.Swing.press;
import static screen.Swing.release;

import input.GamepadButton;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import save.Controls.Action;
import save.SaveData;
import save.WindowSize;
import world.Progress;
import zone.Levels;

class GamePanelTest {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FRAME_MS = 20;

    @TempDir
    Path folder;
    private SaveData save;
    private RecordingGame game;
    private GamePanel panel;

    @BeforeEach
    void start() throws Exception {
        onEdt(() -> {
            save = SaveData.load(folder.resolve("save.properties"));
            game = new RecordingGame(save);
            panel = new GamePanel(game);
            panel.setSize(WIDTH, HEIGHT);
            panel.start(Levels.WORLD, new Progress());
            game.calls().clear();
        });
    }

    @AfterEach
    void stop() throws Exception {
        onEdt(panel::stop);
    }

    private void frames(int count) throws InterruptedException {
        for (int i = 0; i < count; i++) {
            Thread.sleep(FRAME_MS);
            panel.update();
        }
    }

    private int key(Action action) {
        return save.controls().key(action, 0);
    }

    private void clickFirstButtonFromTheBottom() {
        for (int y = HEIGHT - 1; y >= 0 && game.calls().isEmpty(); y -= 4) {
            move(panel, WIDTH / 2, y);
            click(panel, WIDTH / 2, y);
        }
    }

    @Test
    void theArrowsMoveTheBubble() throws Exception {
        onEdt(() -> {
            double startX = panel.world().player().x();
            press(panel, key(Action.RIGHT));
            frames(10);
            release(panel, key(Action.RIGHT));
            assertTrue(panel.world().player().x() > startX);
        });
    }

    @Test
    void thePauseStopsTheGame() throws Exception {
        onEdt(() -> {
            press(panel, key(Action.RIGHT));
            frames(5);
            press(panel, KeyEvent.VK_ESCAPE);
            double x = panel.world().player().x();
            frames(5);
            assertEquals(x, panel.world().player().x(), "the moving bubble stops while paused");
            paint(panel, WIDTH, HEIGHT);
            press(panel, KeyEvent.VK_ENTER);
            press(panel, key(Action.UP));
            press(panel, key(Action.DOWN));
            frames(3);
        });
    }

    @Test
    void thePauseMenuLeadsToTheMapAndTheMenu() throws Exception {
        onEdt(() -> {
            press(panel, KeyEvent.VK_ESCAPE);
            press(panel, KeyEvent.VK_UP);
            press(panel, KeyEvent.VK_UP);
            press(panel, KeyEvent.VK_ENTER);
            assertEquals("map", game.calls().get(0), "Carte is the fourth choice");

            press(panel, KeyEvent.VK_DOWN);
            press(panel, KeyEvent.VK_RIGHT);
            assertEquals("menu", game.calls().get(1), "Retour au menu is the last choice");
        });
    }

    @Test
    void restartingStartsAgainFromTheSameCheckpoint() throws Exception {
        onEdt(() -> {
            press(panel, key(Action.RESTART));
            press(panel, KeyEvent.VK_ESCAPE);
            press(panel, KeyEvent.VK_DOWN);
            press(panel, KeyEvent.VK_ENTER);
            assertEquals(2, game.calls().size());
            assertEquals("restart " + Levels.WORLD + " 0", game.calls().get(0));
            assertEquals(game.calls().get(0), game.calls().get(1), "Recommencer does the same");
        });
    }

    private void openTheSettingsOfThePause() {
        press(panel, KeyEvent.VK_ESCAPE);
        press(panel, KeyEvent.VK_DOWN);
        press(panel, KeyEvent.VK_DOWN);
        press(panel, KeyEvent.VK_ENTER);
    }

    private void presses(int key, int times) {
        for (int i = 0; i < times; i++) {
            press(panel, key);
        }
    }

    private boolean isPlaying() throws InterruptedException {
        save.controls().set(Action.RIGHT, 0, KeyEvent.VK_L);
        double x = panel.world().player().x();
        press(panel, KeyEvent.VK_L);
        frames(5);
        release(panel, KeyEvent.VK_L);
        return panel.world().player().x() > x;
    }

    @Test
    void theSettingsOfThePauseChangeTheVolume() throws Exception {
        onEdt(() -> {
            openTheSettingsOfThePause();
            presses(KeyEvent.VK_DOWN, 2);
            press(panel, KeyEvent.VK_LEFT);
            assertEquals(90, game.volume());
            assertEquals(0.9, game.music().volume(), 1e-9, "the music is turned down at once");
            assertEquals(90, SaveData.load(folder.resolve("save.properties")).volume(), "it is saved at once");
            press(panel, KeyEvent.VK_Q);
            press(panel, KeyEvent.VK_A);
            press(panel, KeyEvent.VK_D);
            press(panel, KeyEvent.VK_ENTER);
            assertEquals(80, game.volume(), "Q, A and D do the same as the arrows, and Entrée does nothing");
            presses(KeyEvent.VK_RIGHT, 5);
            assertEquals(100, game.volume());
            presses(KeyEvent.VK_LEFT, 14);
            assertEquals(0, game.volume());
            assertFalse(isPlaying(), "← never left the settings");
            assertTrue(Swing.colours(paint(panel, WIDTH, HEIGHT)) > 10);
        });
    }

    @Test
    void leavingTheSettingsOfThePauseComesBackOnTheirEntry() throws Exception {
        onEdt(() -> {
            openTheSettingsOfThePause();
            press(panel, KeyEvent.VK_ESCAPE);
            assertFalse(isPlaying(), "Échap went back to the pause, not to the game");
            press(panel, KeyEvent.VK_ENTER);
            presses(KeyEvent.VK_DOWN, 2);
            press(panel, KeyEvent.VK_LEFT);
            assertEquals(90, game.volume(), "Réglages was still selected, so Entrée opened the settings again");

            presses(KeyEvent.VK_DOWN, 2);
            press(panel, KeyEvent.VK_ENTER);
            press(panel, KeyEvent.VK_ESCAPE);
            assertTrue(isPlaying(), "Retour then Échap closed the settings, then the pause");
            assertTrue(game.calls().isEmpty());
        });
    }

    @Test
    void theInvincibleModeOfThePauseAppliesAtOnce() throws Exception {
        onEdt(() -> {
            openTheSettingsOfThePause();
            presses(KeyEvent.VK_DOWN, 3);
            BufferedImage off = paint(panel, WIDTH, HEIGHT);
            press(panel, KeyEvent.VK_ENTER);
            assertTrue(game.isInvincible());
            assertTrue(panel.world().isInvincible(), "the world being played is told at once");
            assertTrue(SaveData.load(folder.resolve("save.properties")).isInvincible(), "it is saved at once");
            assertFalse(Swing.same(off, paint(panel, WIDTH, HEIGHT)), "the button turns green");

            press(panel, KeyEvent.VK_RIGHT);
            assertFalse(game.isInvincible(), "→ validates too");
            assertFalse(panel.world().isInvincible());
        });
    }

    @Test
    void theSettingsOfThePauseChangeTheWindowSize() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "the sizes depend on the screen");
        onEdt(() -> {
            List<WindowSize> sizes = WindowSize.available();
            openTheSettingsOfThePause();
            press(panel, KeyEvent.VK_DOWN);
            press(panel, KeyEvent.VK_ENTER);
            assertTrue(Swing.colours(paint(panel, WIDTH, HEIGHT)) > 10);
            press(panel, KeyEvent.VK_DOWN);
            press(panel, KeyEvent.VK_ENTER);
            assertEquals(sizes.get(1), game.windowSize(), "the list opens on the current size, the first one");
            assertEquals(sizes.get(1), SaveData.load(folder.resolve("save.properties")).windowSize());

            press(panel, KeyEvent.VK_ESCAPE);
            press(panel, KeyEvent.VK_ENTER);
            press(panel, KeyEvent.VK_UP);
            press(panel, KeyEvent.VK_ENTER);
            assertEquals(sizes.get(0), game.windowSize(),
                    "Échap went back on the size, and its list opened on the new current size");

            presses(KeyEvent.VK_UP, 1);
            press(panel, KeyEvent.VK_ENTER);
            press(panel, KeyEvent.VK_ESCAPE);
            press(panel, KeyEvent.VK_ESCAPE);
            assertTrue(isPlaying(), "Retour, the last choice, went back to the settings");
        });
    }

    @Test
    void theMouseSetsTheVolumeOfThePause() throws Exception {
        onEdt(() -> {
            openTheSettingsOfThePause();
            paint(panel, WIDTH, HEIGHT);
            int row = -1;
            for (int y = 0; y < HEIGHT && row < 0; y += 4) {
                drag(panel, WIDTH / 2 - 40, y);
                row = game.volume() == 100 ? -1 : y;
            }
            int dragged = game.volume();
            assertTrue(dragged > 0 && dragged < 100, "dragging over the bar set it to " + dragged);
            assertEquals(dragged, SaveData.load(folder.resolve("save.properties")).volume());

            click(panel, WIDTH / 2 + 20, row);
            assertTrue(game.volume() > dragged, "a click further right turned it up, to " + game.volume());
            int clicked = game.volume();
            click(panel, WIDTH / 2 - 190, row);
            assertEquals(clicked, game.volume(), "a click on the name of the slider changes nothing");
            press(panel, KeyEvent.VK_LEFT);
            assertEquals(clicked, game.volume(), "and the mouse does not move the selection of the keyboard");
        });
    }

    @Test
    void theControllerChangesTheVolumeOfThePause() throws Exception {
        onEdt(() -> {
            press(panel, KeyEvent.VK_ESCAPE);
            panel.pressed(GamepadButton.DOWN);
            panel.pressed(GamepadButton.DOWN);
            panel.pressed(GamepadButton.A);
            panel.pressed(GamepadButton.LEFT);
            panel.pressed(GamepadButton.RIGHT);
            assertEquals(100, game.volume(), "sideways pushes do nothing on a button");
            assertTrue(game.calls().isEmpty());

            panel.pressed(GamepadButton.DOWN);
            panel.pressed(GamepadButton.DOWN);
            panel.pressed(GamepadButton.LEFT);
            panel.pressed(GamepadButton.LEFT);
            panel.pressed(GamepadButton.RIGHT);
            panel.pressed(GamepadButton.A);
            assertEquals(90, game.volume());
            panel.pressed(GamepadButton.B);
            assertFalse(isPlaying(), "B went back to the pause");
            panel.pressed(GamepadButton.B);
            assertTrue(isPlaying(), "and B again resumed the game");
        });
    }

    @Test
    void theControlsCanBeChangedDuringTheGame() throws Exception {
        onEdt(() -> {
            openTheSettingsOfThePause();
            press(panel, KeyEvent.VK_ENTER);
            paint(panel, WIDTH, HEIGHT);
            move(panel, 10, 10);
            click(panel, 10, 10);
            press(panel, KeyEvent.VK_ENTER);
            press(panel, KeyEvent.VK_I);
            assertEquals(KeyEvent.VK_I, save.controls().key(Action.UP, 0));
            assertTrue(game.calls().contains("save controls"));

            press(panel, KeyEvent.VK_ESCAPE);
            press(panel, KeyEvent.VK_ESCAPE);
            assertFalse(isPlaying(), "the controls and the settings are closed, the pause is left");
            press(panel, KeyEvent.VK_ESCAPE);
            assertTrue(isPlaying(), "Échap closed the controls, the settings, then the pause");
        });
    }

    @Test
    void theGridShowsOverTheWorldAndCanBeHidden() throws Exception {
        onEdt(() -> {
            move(panel, WIDTH / 2, HEIGHT / 2);
            BufferedImage withGrid = paint(panel, WIDTH, HEIGHT);
            press(panel, key(Action.GRID));
            BufferedImage without = paint(panel, WIDTH, HEIGHT);
            leave(panel);
            assertTrue(Swing.colours(withGrid) > 20);
            assertFalse(Swing.same(withGrid, without));
        });
    }

    @Test
    void theGearOpensThePauseAndItsButtonsCanBeClicked() throws Exception {
        onEdt(() -> {
            move(panel, WIDTH - 28, 26);
            paint(panel, WIDTH, HEIGHT);
            click(panel, WIDTH - 28, 26);
            assertTrue(game.calls().isEmpty());
            clickFirstButtonFromTheBottom();
            assertEquals("menu", game.calls().get(0), "the lowest button is Retour au menu");
        });
    }

    private void framesUntilTension(double tension) throws InterruptedException {
        for (int i = 0; i < 150 && panel.world().dashTension() < tension; i++) {
            frames(1);
        }
    }

    @Test
    void holdingTheDashKeyStretchesTheBubbleOnceSplitIsBack() throws Exception {
        onEdt(() -> {
            press(panel, key(Action.RIGHT));
            press(panel, key(Action.DASH));
            frames(12);
            assertFalse(panel.world().player().isAnchored(), "no dash before Split");
            release(panel, key(Action.DASH));
            release(panel, key(Action.RIGHT));

            panel.start(Levels.WORLD, new Progress(List.of(), List.of("Split"), List.of(), 0));
            BufferedImage resting = paint(panel, WIDTH, HEIGHT);
            press(panel, key(Action.RIGHT));
            press(panel, key(Action.DASH));
            framesUntilTension(0.5);
            assertTrue(panel.world().player().isAnchored(), "the back is stuck while the key is held");
            assertTrue(panel.world().dashTension() >= 0.5);
            assertFalse(Swing.same(resting, paint(panel, WIDTH, HEIGHT)));

            release(panel, key(Action.DASH));
            frames(1);
            assertFalse(panel.world().player().isAnchored());
            assertTrue(panel.world().player().vx() > 350, "letting go throws the bubble");
        });
    }

    @Test
    void openingThePauseLetsGoOfTheGroundWithoutThrowingTheBubble() throws Exception {
        onEdt(() -> {
            panel.start(Levels.WORLD, new Progress(List.of(), List.of("Split"), List.of(), 0));
            press(panel, key(Action.RIGHT));
            press(panel, key(Action.DASH));
            framesUntilTension(0.5);
            press(panel, KeyEvent.VK_ESCAPE);
            assertFalse(panel.world().player().isAnchored());
            press(panel, KeyEvent.VK_ENTER);
            frames(1);
            assertTrue(panel.world().player().speed() < 300, "the bubble wasn't thrown when the game resumed");
        });
    }

    @Test
    void theSwapKeyChoosesTheOtherHalfOfATornBubble() throws Exception {
        onEdt(() -> {
            panel.start(Levels.WORLD, new Progress(List.of(), List.of("Split"), List.of(), 0));
            fluid.Droplet whole = panel.world().player();
            press(panel, key(Action.SWAP));
            assertEquals(whole, panel.world().player(), "nothing to choose with one drop");

            press(panel, key(Action.RIGHT));
            press(panel, key(Action.DASH));
            for (int i = 0; i < 200 && panel.world().droplets().size() == 1; i++) {
                frames(1);
            }
            release(panel, key(Action.DASH));
            release(panel, key(Action.RIGHT));
            assertEquals(2, panel.world().droplets().size(), "held too long, the bubble tears in two");
            fluid.Droplet head = panel.world().player();
            BufferedImage steeringTheHead = paint(panel, WIDTH, HEIGHT);
            press(panel, key(Action.SWAP));
            assertTrue(panel.world().player() != head);
            assertTrue(panel.world().player().x() < head.x(), "the other half is the back one");
            assertFalse(Swing.same(steeringTheHead, paint(panel, WIDTH, HEIGHT)));
        });
    }

    @Test
    void theLightOfSplitOpensAPopupThatLeadsToTheTraining() throws Exception {
        onEdt(() -> {
            panel.lightTaken("Split");
            double x = panel.world().player().x();
            press(panel, key(Action.RIGHT));
            frames(5);
            assertEquals(x, panel.world().player().x(), "the game waits behind the pop-up");
            assertTrue(Swing.colours(paint(panel, WIDTH, HEIGHT)) > 10);
            press(panel, KeyEvent.VK_ENTER);
            assertTrue(game.calls().isEmpty(), "a key pressed right away is ignored");
            Thread.sleep(600);
            press(panel, KeyEvent.VK_ENTER);
            assertEquals(List.of("training"), game.calls(), "Salle d'entraînement is the first choice");
        });
    }

    @Test
    void thePopupCanSendBackToTheHub() throws Exception {
        onEdt(() -> {
            panel.lightTaken("Split");
            Thread.sleep(600);
            press(panel, KeyEvent.VK_ESCAPE);
            assertTrue(game.calls().isEmpty());
            double x = panel.world().player().x();
            press(panel, key(Action.RIGHT));
            frames(5);
            assertTrue(panel.world().player().x() > x, "the game goes on");
            paint(panel, WIDTH, HEIGHT);
        });
    }

    @Test
    void thePopupButtonsCanBeClicked() throws Exception {
        onEdt(() -> {
            panel.lightTaken("Split");
            paint(panel, WIDTH, HEIGHT);
            clickFirstButtonFromTheBottom();
            assertTrue(game.calls().isEmpty(), "the lowest button is Retourner au hub");
            panel.lightTaken("Split");
            for (int y = 0; y < HEIGHT && game.calls().isEmpty(); y += 4) {
                move(panel, WIDTH / 2, y);
                click(panel, WIDTH / 2, y);
            }
            assertEquals(List.of("training"), game.calls(), "the highest button is Salle d'entraînement");
        });
    }

    @Test
    void theLightOfShootTeachesTheShootWithoutATrainingRoom() throws Exception {
        onEdt(() -> {
            panel.lightTaken("Shoot");
            assertTrue(Swing.colours(paint(panel, WIDTH, HEIGHT)) > 10);
            Thread.sleep(600);
            press(panel, KeyEvent.VK_DOWN);
            press(panel, KeyEvent.VK_ENTER);
            assertTrue(game.calls().isEmpty(), "the only choice is Retourner au hub");
            double x = panel.world().player().x();
            press(panel, key(Action.RIGHT));
            frames(5);
            assertTrue(panel.world().player().x() > x, "the game goes on");
        });
    }

    private double shotAngle() {
        enemy.ability.Shot shot = panel.world().shots().get(0);
        return Math.toDegrees(Math.atan2(shot.vy(), shot.vx()));
    }

    private void tap(int key) throws InterruptedException {
        press(panel, key);
        frames(1);
        release(panel, key);
        frames(1);
    }

    @Test
    void ctrlChargesAShotAndTheAimKeysTurnItsArrowStepByStep() throws Exception {
        onEdt(() -> {
            assertEquals(KeyEvent.VK_CONTROL, key(Action.SHOOT));
            assertEquals(KeyEvent.VK_RIGHT, key(Action.AIM_RIGHT));
            panel.start(Levels.WORLD, new Progress(List.of(), List.of("Split", "Shoot"), List.of(), 0));
            frames(1);
            BufferedImage idle = paint(panel, WIDTH, HEIGHT);
            press(panel, KeyEvent.VK_CONTROL);
            frames(2);
            assertTrue(panel.world().isChargingShot());
            BufferedImage aimingRight = paint(panel, WIDTH, HEIGHT);
            assertFalse(Swing.same(idle, aimingRight), "the power and the arrow show while charging");
            tap(key(Action.AIM_RIGHT));
            tap(key(Action.AIM_RIGHT));
            tap(key(Action.AIM_RIGHT));
            assertFalse(Swing.same(aimingRight, paint(panel, WIDTH, HEIGHT)), "the arrow turned");
            release(panel, KeyEvent.VK_CONTROL);
            frames(1);
            assertEquals(1, panel.world().shots().size(), "letting go of Ctrl fires");
            assertEquals(3 * enemy.ability.Shoot.AIM_STEP, shotAngle(), 1e-6, "one step per press");
        });
    }

    @Test
    void anAimKeyHeldSpinsTheArrow() throws Exception {
        onEdt(() -> {
            panel.start(Levels.WORLD, new Progress(List.of(), List.of("Split", "Shoot"), List.of(), 0));
            frames(1);
            press(panel, KeyEvent.VK_CONTROL);
            press(panel, key(Action.AIM_LEFT));
            frames(40);
            release(panel, key(Action.AIM_LEFT));
            release(panel, KeyEvent.VK_CONTROL);
            frames(1);
            assertTrue(shotAngle() < -15, "held for most of a second, it turned much more than a step: " + shotAngle());
        });
    }

    @Test
    void theAimKeysDoNotSteerTheBubbleWhileAShotCharges() throws Exception {
        onEdt(() -> {
            panel.start(Levels.WORLD, new Progress(List.of(), List.of("Split", "Shoot"), List.of(), 0));
            frames(1);
            assertEquals(key(Action.RIGHT), key(Action.AIM_RIGHT), "by default the arrows both move and aim");
            double x = panel.world().player().x();
            press(panel, KeyEvent.VK_CONTROL);
            press(panel, key(Action.AIM_RIGHT));
            frames(8);
            assertEquals(x, panel.world().player().x(), 1e-6, "the arrow only turns the aim");
            release(panel, key(Action.AIM_RIGHT));

            press(panel, KeyEvent.VK_D);
            frames(8);
            release(panel, KeyEvent.VK_D);
            assertTrue(panel.world().player().x() > x, "the other movement key still steers");
            panel.world().cancelShot();
            release(panel, KeyEvent.VK_CONTROL);
        });
    }

    @Test
    void theArrowsMoveTheBubbleAgainOnceTheShotIsFired() throws Exception {
        onEdt(() -> {
            panel.start(Levels.WORLD, new Progress(List.of(), List.of("Split", "Shoot"), List.of(), 0));
            frames(1);
            press(panel, KeyEvent.VK_CONTROL);
            frames(2);
            release(panel, KeyEvent.VK_CONTROL);
            frames(1);
            double x = panel.world().player().x();
            press(panel, key(Action.RIGHT));
            frames(8);
            release(panel, key(Action.RIGHT));
            assertTrue(panel.world().player().x() > x);
        });
    }

    @Test
    void theShootKeyCanBeChangedInTheControls() throws Exception {
        onEdt(() -> {
            save.controls().set(Action.SHOOT, 0, KeyEvent.VK_F);
            panel.start(Levels.WORLD, new Progress(List.of(), List.of("Split", "Shoot"), List.of(), 0));
            frames(1);
            press(panel, KeyEvent.VK_CONTROL);
            frames(3);
            assertFalse(panel.world().isChargingShot(), "Ctrl no longer shoots");
            press(panel, KeyEvent.VK_F);
            frames(3);
            assertTrue(panel.world().isChargingShot());
        });
    }

    private void tapTheSlideKey() throws InterruptedException {
        press(panel, key(Action.SLIDE));
        frames(1);
        release(panel, key(Action.SLIDE));
        frames(1);
    }

    @Test
    void theSlideKeyKeepsTheBubbleMovingOnceDashIsBack() throws Exception {
        onEdt(() -> {
            assertEquals(KeyEvent.VK_F, key(Action.SLIDE));
            press(panel, key(Action.RIGHT));
            frames(12);
            tapTheSlideKey();
            assertFalse(panel.world().isSliding(), "no slide before the light of Dash");
            release(panel, key(Action.RIGHT));

            panel.start(Levels.WORLD, new Progress(List.of(), List.of("Split", "Dash"), List.of(), 0));
            BufferedImage resting = paint(panel, WIDTH, HEIGHT);
            press(panel, key(Action.RIGHT));
            frames(12);
            tapTheSlideKey();
            release(panel, key(Action.RIGHT));
            assertTrue(panel.world().isSliding());
            double speed = panel.world().player().speed();
            double x = panel.world().player().x();
            assertTrue(speed > 60);
            press(panel, key(Action.UP));
            frames(10);
            release(panel, key(Action.UP));
            assertEquals(speed, panel.world().player().speed(), 1e-9, "no key held, and Haut did not turn it");
            assertTrue(panel.world().player().x() > x);
            assertFalse(Swing.same(resting, paint(panel, WIDTH, HEIGHT)), "the HUD says the speed is kept");

            tapTheSlideKey();
            assertFalse(panel.world().isSliding(), "a second press gives the control back");
            frames(10);
            assertTrue(panel.world().player().speed() < speed, "the bubble slows down again");
        });
    }

    @Test
    void holdingTheSlideKeyDoesNotTurnItOffAgain() throws Exception {
        onEdt(() -> {
            panel.start(Levels.WORLD, new Progress(List.of(), List.of("Split", "Dash"), List.of(), 0));
            press(panel, key(Action.RIGHT));
            frames(12);
            press(panel, key(Action.SLIDE));
            frames(8);
            press(panel, key(Action.SLIDE));
            frames(2);
            assertTrue(panel.world().isSliding(), "the key repeats while it is held, and still counts once");
        });
    }

    @Test
    void theSlideGoesOnThroughAPause() throws Exception {
        onEdt(() -> {
            panel.start(Levels.WORLD, new Progress(List.of(), List.of("Split", "Dash"), List.of(), 0));
            press(panel, key(Action.RIGHT));
            frames(12);
            tapTheSlideKey();
            release(panel, key(Action.RIGHT));
            double speed = panel.world().player().speed();
            press(panel, KeyEvent.VK_ESCAPE);
            assertTrue(Swing.colours(paint(panel, WIDTH, HEIGHT)) > 10);
            press(panel, KeyEvent.VK_ESCAPE);
            frames(3);
            assertTrue(panel.world().isSliding());
            assertEquals(speed, panel.world().player().speed(), 1e-9);
        });
    }

    @Test
    void theLightOfDashTeachesTheSlideWithoutATrainingRoom() throws Exception {
        onEdt(() -> {
            panel.lightTaken("Dash");
            assertTrue(Swing.colours(paint(panel, WIDTH, HEIGHT)) > 10);
            Thread.sleep(600);
            press(panel, KeyEvent.VK_DOWN);
            press(panel, KeyEvent.VK_ENTER);
            assertTrue(game.calls().isEmpty(), "the only choice is Retourner au hub");
            double x = panel.world().player().x();
            press(panel, key(Action.RIGHT));
            frames(5);
            assertTrue(panel.world().player().x() > x, "the game goes on");
        });
    }

    @Test
    void aLightThatTeachesNothingSendsBackToTheHubRightAway() throws Exception {
        onEdt(() -> {
            panel.lightTaken("Lumière");
            double x = panel.world().player().x();
            press(panel, key(Action.RIGHT));
            frames(5);
            assertTrue(panel.world().player().x() > x, "no pop-up");
            assertTrue(game.calls().isEmpty());
        });
    }

    @Test
    void theTrainingRoomLeadsBackToTheHub() throws Exception {
        onEdt(() -> {
            save.remember(new Progress(List.of(), List.of("Split"), List.of(), 0));
            panel.start(Levels.TRAINING, game.trainingProgress());
            assertTrue(panel.world().canDash(), "the training gets the saved lights");
            assertTrue(Swing.colours(paint(panel, WIDTH, HEIGHT)) > 10);
            panel.lightTaken(zone.DashTraining.EXIT);
            assertEquals(List.of("continue"), game.calls(), "the exit goes back to the world");

            press(panel, KeyEvent.VK_ESCAPE);
            paint(panel, WIDTH, HEIGHT);
            press(panel, KeyEvent.VK_UP);
            press(panel, KeyEvent.VK_UP);
            press(panel, KeyEvent.VK_ENTER);
            assertEquals(List.of("continue", "continue"), game.calls(), "so does Retourner au hub in the pause");

            press(panel, KeyEvent.VK_ESCAPE);
            press(panel, key(Action.RESTART));
            assertEquals("restart " + Levels.TRAINING + " 0", game.calls().get(2));
            assertTrue(panel.world().canDash(), "restarting the training keeps the dash");
        });
    }

    @Test
    void theInvincibleModeOfTheSettingsAppliesToTheGame() throws Exception {
        onEdt(() -> {
            assertFalse(panel.world().isInvincible());
            BufferedImage mortal = paint(panel, WIDTH, HEIGHT);
            game.setInvincible(true);
            panel.start(Levels.WORLD, new Progress());
            assertTrue(panel.world().isInvincible());
            assertFalse(Swing.same(mortal, paint(panel, WIDTH, HEIGHT)), "the HUD says the mode is on");
            press(panel, key(Action.RESTART));
            assertTrue(panel.world().isInvincible(), "it stays on after a restart");
        });
    }

    @Test
    void theTestRoomCanBePlayed() throws Exception {
        onEdt(() -> {
            panel.start(Levels.TEST, new Progress());
            press(panel, key(Action.DOWN));
            frames(5);
            assertEquals(1, panel.world().droplets().size());
            assertTrue(Swing.colours(paint(panel, WIDTH, HEIGHT)) > 10);
        });
    }
}
