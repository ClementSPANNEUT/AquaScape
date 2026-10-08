package screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static screen.Swing.onEdt;
import static screen.Swing.paint;

import input.FakeGamepad;
import input.GamepadButton;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import save.Controls;
import save.SaveData;
import world.Progress;
import zone.Levels;

class GamepadControlsTest {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FRAME_MS = 20;

    @TempDir
    Path folder;
    private FakeGamepad pad;
    private SaveData save;
    private RecordingGame game;
    private GamePanel panel;

    @BeforeEach
    void start() throws Exception {
        onEdt(() -> {
            pad = new FakeGamepad();
            save = SaveData.load(folder.resolve("save.properties"));
            game = new RecordingGame(save, pad);
            panel = game.showTheGameScreen();
            panel.setSize(WIDTH, HEIGHT);
            panel.start(Levels.WORLD, new Progress(List.of(), List.of("Split"), List.of(), 0));
            game.calls().clear();
        });
    }

    @AfterEach
    void stop() throws Exception {
        onEdt(panel::stop);
    }

    private void frames(GamePanel target, int count) throws InterruptedException {
        for (int i = 0; i < count; i++) {
            Thread.sleep(FRAME_MS);
            target.update();
        }
    }

    private void taps(GamepadButton button, int times) {
        for (int i = 0; i < times; i++) {
            pad.tap(button);
        }
    }

    private boolean movesRight() throws InterruptedException {
        double x = panel.world().player().x();
        pad.hold(GamepadButton.RIGHT);
        pad.poll();
        frames(panel, 10);
        pad.letGo(GamepadButton.RIGHT);
        pad.poll();
        return panel.world().player().x() > x;
    }

    @Test
    void theStickMovesTheBubble() throws Exception {
        onEdt(() -> assertTrue(movesRight()));
    }

    private void framesUntilTension(double tension) throws InterruptedException {
        for (int i = 0; i < 150 && panel.world().dashTension() < tension; i++) {
            frames(panel, 1);
        }
    }

    @Test
    void holdingAStretchesTheBubbleAndLettingGoThrowsIt() throws Exception {
        onEdt(() -> {
            pad.hold(GamepadButton.RIGHT, GamepadButton.A);
            pad.poll();
            framesUntilTension(0.5);
            assertTrue(panel.world().player().isAnchored(), "the back is stuck while A is held");
            assertTrue(panel.world().dashTension() >= 0.5);
            pad.letGo(GamepadButton.A);
            pad.poll();
            frames(panel, 1);
            assertFalse(panel.world().player().isAnchored());
            assertTrue(panel.world().player().vx() > 350, "letting go throws the bubble");
        });
    }

    @Test
    void theDashFollowsTheButtonChosenInTheControls() throws Exception {
        onEdt(() -> {
            save.controls().setButton(Controls.Action.DASH, GamepadButton.RT);
            pad.hold(GamepadButton.RIGHT, GamepadButton.A);
            pad.poll();
            frames(panel, 5);
            assertFalse(panel.world().player().isAnchored(), "A no longer dashes");
            pad.letGo(GamepadButton.A);
            pad.hold(GamepadButton.RT);
            pad.poll();
            frames(panel, 2);
            assertTrue(panel.world().player().isAnchored(), "R2 dashes instead");
        });
    }

    @Test
    void validatingAPopupWithADoesNotStickTheBubble() throws Exception {
        onEdt(() -> {
            pad.tap(GamepadButton.START);
            pad.hold(GamepadButton.A);
            pad.poll();
            frames(panel, 3);
            assertFalse(panel.world().player().isAnchored(), "A closed the pause, and must be let go before a dash");
            pad.letGo(GamepadButton.A);
            pad.poll();
            frames(panel, 1);
            pad.hold(GamepadButton.A);
            pad.poll();
            frames(panel, 1);
            assertTrue(panel.world().player().isAnchored());
        });
    }

    @Test
    void xChoosesTheOtherHalfOfATornBubble() throws Exception {
        onEdt(() -> {
            pad.hold(GamepadButton.RIGHT, GamepadButton.A);
            pad.poll();
            for (int i = 0; i < 200 && panel.world().droplets().size() == 1; i++) {
                frames(panel, 1);
            }
            pad.letGo(GamepadButton.RIGHT, GamepadButton.A);
            pad.poll();
            assertEquals(2, panel.world().droplets().size(), "held too long, the bubble tears in two");
            fluid.Droplet head = panel.world().player();
            pad.tap(GamepadButton.X);
            assertTrue(panel.world().player() != head);
            assertTrue(panel.world().player().x() < head.x(), "the other half is the back one");
            pad.tap(GamepadButton.X);
            assertTrue(panel.world().player() == head);
        });
    }

    @Test
    void yRestartsAndBackShowsTheGrid() throws Exception {
        onEdt(() -> {
            BufferedImage withGrid = paint(panel, WIDTH, HEIGHT);
            pad.tap(GamepadButton.BACK);
            assertFalse(Swing.same(withGrid, paint(panel, WIDTH, HEIGHT)), "Select hides the grid");
            pad.tap(GamepadButton.Y);
            assertTrue(game.calls().contains("restart " + Levels.WORLD + " 0"));
        });
    }

    @Test
    void theHudShowsTheButtonsOfTheController() throws Exception {
        onEdt(() -> {
            BufferedImage plugged = paint(panel, WIDTH, HEIGHT);
            save.controls().setButton(Controls.Action.DASH, GamepadButton.LB);
            BufferedImage rebound = paint(panel, WIDTH, HEIGHT);
            assertFalse(Swing.same(plugged, rebound), "the hint names the button chosen for the dash");
            pad.unplug();
            pad.poll();
            assertFalse(Swing.same(rebound, paint(panel, WIDTH, HEIGHT)), "the hint goes back to the keyboard");
        });
    }

    @Test
    void theGameButtonsWaitBehindAPopup() throws Exception {
        onEdt(() -> {
            panel.lightTaken("Split");
            pad.tap(GamepadButton.Y);
            assertFalse(game.calls().stream().anyMatch(call -> call.startsWith("restart")));
        });
    }

    private void startWithTheSlide() throws InterruptedException {
        panel.start(Levels.WORLD, new Progress(List.of(), List.of("Split", "Dash"), List.of(), 0));
        pad.hold(GamepadButton.RIGHT);
        pad.poll();
        frames(panel, 12);
    }

    @Test
    void theRightTriggerMakesTheBubbleSlide() throws Exception {
        onEdt(() -> {
            startWithTheSlide();
            pad.hold(GamepadButton.RT);
            pad.poll();
            frames(panel, 5);
            assertTrue(panel.world().isSliding(), "one press, however long the trigger stays down");
            pad.letGo(GamepadButton.RT, GamepadButton.RIGHT);
            pad.poll();
            double speed = panel.world().player().speed();
            frames(panel, 10);
            assertEquals(speed, panel.world().player().speed(), 1e-9, "the stick is let go and the speed is kept");

            pad.hold(GamepadButton.RT);
            pad.poll();
            frames(panel, 1);
            assertFalse(panel.world().isSliding(), "a second press gives the control back");
            assertTrue(Swing.colours(Swing.paint(panel, WIDTH, HEIGHT)) > 10);
        });
    }

    @Test
    void theButtonThatClosesThePauseDoesNotStartTheSlide() throws Exception {
        onEdt(() -> {
            save.controls().setButton(Controls.Action.SLIDE, GamepadButton.B);
            startWithTheSlide();
            pad.tap(GamepadButton.START);
            pad.hold(GamepadButton.B);
            pad.poll();
            frames(panel, 3);
            assertFalse(panel.world().isSliding(), "B closed the pause and is still held");
            pad.letGo(GamepadButton.B);
            pad.poll();
            frames(panel, 1);
            pad.hold(GamepadButton.B);
            pad.poll();
            frames(panel, 1);
            assertTrue(panel.world().isSliding(), "let go then pressed again, B makes the bubble slide");
        });
    }

    @Test
    void theControllerDrivesTheMenus() throws Exception {
        onEdt(() -> {
            game.showMenu();
            game.calls().clear();
            pad.tap(GamepadButton.DOWN);
            pad.tap(GamepadButton.A);
            assertEquals(List.of("map"), game.calls(), "the second button of the menu is the map");
        });
    }

    @Test
    void aSidewaysPushNeitherValidatesNorGoesBackInTheMenu() throws Exception {
        onEdt(() -> {
            game.showMenu();
            game.calls().clear();
            pad.tap(GamepadButton.RIGHT);
            pad.tap(GamepadButton.LEFT);
            assertEquals(List.of(), game.calls());
        });
    }

    @Test
    void thePauseMenuOpensTheChoiceUnderTheSelection() throws Exception {
        onEdt(() -> {
            pad.tap(GamepadButton.START);
            taps(GamepadButton.DOWN, 3);
            pad.tap(GamepadButton.A);
            assertEquals(List.of("map"), game.calls(), "the fourth choice of the pause is the map");
        });
    }

    @Test
    void aSidewaysPushDoesNotResumeThePausedGame() throws Exception {
        onEdt(() -> {
            pad.tap(GamepadButton.START);
            pad.hold(GamepadButton.DOWN, GamepadButton.LEFT);
            pad.poll();
            pad.letGo(GamepadButton.DOWN, GamepadButton.LEFT);
            pad.poll();
            pad.tap(GamepadButton.RIGHT);
            assertEquals(List.of(), game.calls(), "right doesn't validate « Recommencer »");
            assertFalse(movesRight(), "the game is still paused");
            pad.tap(GamepadButton.A);
            assertEquals(List.of("restart " + Levels.WORLD + " 0"), game.calls(),
                    "down moved to « Recommencer » although the stick also went left");
        });
    }

    @Test
    void startPausesAndBResumes() throws Exception {
        onEdt(() -> {
            pad.tap(GamepadButton.START);
            assertFalse(movesRight(), "nothing moves during the pause");
            pad.tap(GamepadButton.B);
            assertTrue(movesRight(), "B closed the pause");
            pad.tap(GamepadButton.B);
            assertTrue(movesRight(), "in game, B is free: only Start pauses");
        });
    }

    @Test
    void theControlsOfThePauseBindAControllerButton() throws Exception {
        onEdt(() -> {
            pad.tap(GamepadButton.START);
            taps(GamepadButton.DOWN, 2);
            pad.tap(GamepadButton.A);
            pad.tap(GamepadButton.A);
            taps(GamepadButton.DOWN, Controls.Action.DASH.ordinal());
            taps(GamepadButton.RIGHT, 2);
            pad.tap(GamepadButton.A);
            pad.tap(GamepadButton.X);
            assertEquals(GamepadButton.X, save.controls().button(Controls.Action.DASH));
            assertEquals(List.of("save controls"), game.calls());
            assertEquals(GamepadButton.X, SaveData.load(folder.resolve("save.properties")).controls()
                    .button(Controls.Action.DASH), "the button is written in the save");

            pad.tap(GamepadButton.B);
            pad.tap(GamepadButton.A);
            taps(GamepadButton.DOWN, Controls.Action.RESTART.ordinal());
            taps(GamepadButton.RIGHT, 2);
            pad.tap(GamepadButton.X);
            assertNull(save.controls().button(Controls.Action.RESTART),
                    "B went back to the settings on « Contrôles », and X emptied the cell");
        });
    }

    private void startWithTheShoot() throws InterruptedException {
        panel.start(Levels.WORLD, new Progress(List.of(), List.of("Split", "Shoot"), List.of(), 0));
        frames(panel, 1);
    }

    @Test
    void holdingL2ChargesAShotAimedWithTheRightStick() throws Exception {
        onEdt(() -> {
            startWithTheShoot();
            BufferedImage idle = paint(panel, WIDTH, HEIGHT);
            pad.aim(0, -1);
            pad.hold(GamepadButton.LT);
            pad.poll();
            frames(panel, 8);
            assertTrue(panel.world().isChargingShot());
            assertTrue(panel.world().shotPower() > 0);
            assertFalse(Swing.same(idle, paint(panel, WIDTH, HEIGHT)), "the power shows while charging");
            pad.letGo(GamepadButton.LT);
            pad.poll();
            frames(panel, 1);
            assertFalse(panel.world().isChargingShot());
            assertEquals(1, panel.world().shots().size(), "letting go fires a ball");
            assertTrue(panel.world().shots().get(0).vy() < 0, "upward, where the right stick points");
            assertEquals(0, panel.world().shots().get(0).vx(), 1e-6);
        });
    }

    @Test
    void theLeftStickMovesTheBubbleWithoutChangingTheAim() throws Exception {
        onEdt(() -> {
            startWithTheShoot();
            double x = panel.world().player().x();
            pad.aim(0, 1);
            pad.hold(GamepadButton.LEFT, GamepadButton.LT);
            pad.poll();
            frames(panel, 5);
            pad.aim(0, 0);
            frames(panel, 5);
            pad.letGo(GamepadButton.LT);
            pad.poll();
            frames(panel, 1);
            assertTrue(panel.world().player().x() < x, "the bubble went left while charging");
            assertTrue(panel.world().shots().get(0).vy() > 0, "the ball goes down, where the right stick last pointed");
            assertEquals(0, panel.world().shots().get(0).vx(), 1e-6);
        });
    }

    @Test
    void theShootNeedsTheLightOfShoot() throws Exception {
        onEdt(() -> {
            frames(panel, 1);
            pad.hold(GamepadButton.LT);
            pad.poll();
            frames(panel, 5);
            assertFalse(panel.world().isChargingShot(), "this game only has the light of Split");
        });
    }

    @Test
    void holdingL2TooLongBlowsTheBubbleUp() throws Exception {
        onEdt(() -> {
            startWithTheShoot();
            pad.hold(GamepadButton.LT);
            pad.poll();
            for (int i = 0; i < 500 && !panel.world().isPlayerDead(); i++) {
                frames(panel, 1);
            }
            assertTrue(panel.world().isPlayerDead());
            assertTrue(panel.world().hasExploded());
            assertTrue(panel.world().shots().isEmpty());
            assertTrue(Swing.colours(paint(panel, WIDTH, HEIGHT)) > 10);
        });
    }

    @Test
    void aShotChargedWhenThePauseOpensIsNotFired() throws Exception {
        onEdt(() -> {
            startWithTheShoot();
            pad.hold(GamepadButton.LT);
            pad.poll();
            frames(panel, 5);
            pad.tap(GamepadButton.START);
            pad.letGo(GamepadButton.LT);
            pad.poll();
            pad.tap(GamepadButton.B);
            frames(panel, 2);
            assertTrue(panel.world().shots().isEmpty());
            assertFalse(panel.world().isChargingShot());
        });
    }

    @Test
    void theMapAndTheControlsScreenTakeTheController() throws Exception {
        onEdt(() -> {
            MapPanel map = new MapPanel(game);
            map.setSize(WIDTH, HEIGHT);
            map.open();
            map.pressed(GamepadButton.RIGHT);
            map.pressed(GamepadButton.A);
            map.pressed(GamepadButton.B);
            assertEquals(List.of("play 0", "menu"), game.calls(), "only the start is validated, so right stays on it");

            game.calls().clear();
            ControlsPanel controls = new ControlsPanel(game);
            controls.open();
            controls.pressed(GamepadButton.DOWN);
            controls.pressed(GamepadButton.X);
            assertEquals(Controls.NONE, save.controls().key(Controls.Action.DOWN, 0));
            controls.pressed(GamepadButton.START);
            assertEquals(List.of("save controls", "settings"), game.calls());
        });
    }

    @Test
    void startPausesTheRealGameAndBResumesIt() throws Exception {
        onEdt(() -> {
            Game real = new Game(save, pad);
            real.playTest();
            GamePanel screen = real.gamePanel();
            screen.setSize(WIDTH, HEIGHT);
            pad.tap(GamepadButton.START);
            double y = screen.world().player().y();
            pad.hold(GamepadButton.DOWN);
            pad.poll();
            frames(screen, 5);
            assertEquals(y, screen.world().player().y(), "nothing moves during the pause");
            pad.letGo(GamepadButton.DOWN);
            pad.poll();
            pad.tap(GamepadButton.B);
            pad.hold(GamepadButton.DOWN);
            pad.poll();
            frames(screen, 10);
            assertTrue(screen.world().player().y() > y, "B closed the pause");
            screen.stop();
        });
    }
}
