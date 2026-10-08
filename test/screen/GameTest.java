package screen;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static screen.Swing.onEdt;

import audio.FakeMusic;
import input.Gamepad;
import java.awt.event.KeyEvent;
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

class GameTest {
    @TempDir
    Path folder;
    private Path file;
    private SaveData save;
    private Game game;

    @BeforeEach
    void open() throws Exception {
        file = folder.resolve("save.properties");
        save = SaveData.load(file);
        onEdt(() -> game = new Game(save));
    }

    @AfterEach
    void close() throws Exception {
        onEdt(game::showMenu);
    }

    @Test
    void onlyTheStartIsValidatedAtFirst() {
        assertTrue(game.isValidated(0));
        assertFalse(game.isValidated(3));
        assertFalse(game.hasStarted());
        assertEquals(0, game.lastCheckpoint());
    }

    @Test
    void onlyTheWorldIsSaved() {
        game.saveProgress(Levels.TEST, new Progress(List.of(4), List.of(), List.of(), 4));
        assertFalse(game.isValidated(4));

        game.saveProgress(Levels.WORLD, new Progress(List.of(4), List.of("Dash"), List.of(), 4));
        assertTrue(game.isValidated(4));
        assertTrue(game.hasStarted());
        assertEquals(4, game.lastCheckpoint());
        assertTrue(SaveData.load(file).checkpoints().contains(4), "written to the file");
    }

    @Test
    void restartingTheTestRoomForgetsTheProgress() {
        game.saveProgress(Levels.WORLD, new Progress(List.of(2), List.of(), List.of(), 2));
        assertEquals(3, game.restartProgress(Levels.WORLD, 3).checkpoint());
        assertTrue(game.restartProgress(Levels.WORLD, 3).hasReached(2));
        assertFalse(game.restartProgress(Levels.TEST, 3).hasReached(2));
        assertEquals(2, game.savedProgress(2).checkpoint());
    }

    @Test
    void finishingTheWorldIsRemembered() {
        assertFalse(game.isCompleted(Levels.WORLD));
        game.completeLevel(Levels.WORLD);
        assertTrue(game.isCompleted(Levels.WORLD));
        assertEquals(1, SaveData.load(file).completedLevels());
    }

    @Test
    void theWindowSizeAndTheControlsAreSaved() {
        game.setWindowSize(WindowSize.HD);
        assertEquals(WindowSize.HD, game.windowSize());
        assertSame(save.controls(), game.controls());
        game.controls().set(Action.UP, 0, KeyEvent.VK_I);
        game.saveControls();

        SaveData loaded = SaveData.load(file);
        assertEquals(WindowSize.HD, loaded.windowSize());
        assertEquals(KeyEvent.VK_I, loaded.controls().key(Action.UP, 0));
    }

    @Test
    void theVolumeIsSavedAndGivenToTheMusic() throws Exception {
        onEdt(() -> {
            FakeMusic music = new FakeMusic();
            Game loud = new Game(save, Gamepad.none(), music);
            assertEquals(100, loud.volume());
            assertEquals(List.of(1.0), music.volumes(), "the saved volume is set when the game opens");

            loud.setVolume(40);
            assertEquals(40, loud.volume());
            assertEquals(0.4, music.volume(), 1e-9);
            assertEquals(40, SaveData.load(file).volume(), "written to the file");

            loud.setVolume(250);
            assertEquals(100, loud.volume());
            assertEquals(1, music.volume(), 1e-9);
        });
    }

    @Test
    void theVolumeOfTheLastGameComesBack() throws Exception {
        onEdt(() -> {
            game.setVolume(30);
            FakeMusic music = new FakeMusic();
            Game next = new Game(SaveData.load(file), Gamepad.none(), music);
            assertEquals(30, next.volume());
            assertEquals(0.3, music.volume(), 1e-9);
        });
    }

    @Test
    void everyScreenCanBeShown() throws Exception {
        onEdt(() -> assertDoesNotThrow(() -> {
            game.showMap();
            game.showSettings();
            game.showControls();
            game.play(0);
            game.playTest();
            game.continueGame();
            game.showMenu();
        }));
    }
}
