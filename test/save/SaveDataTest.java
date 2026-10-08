package save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import world.Progress;

class SaveDataTest {
    @TempDir
    Path folder;

    @Test
    void aMissingFileGivesAFreshSave() {
        SaveData save = SaveData.load(folder.resolve("none.properties"));
        assertEquals(0, save.completedLevels());
        assertTrue(save.checkpoints().isEmpty());
        assertTrue(save.lights().isEmpty());
        assertEquals(WindowSize.DEFAULT, save.windowSize());
        assertFalse(save.isInvincible(), "the drop can die unless the player asks otherwise");
    }

    @Test
    void everythingSurvivesSavingAndLoading() {
        Path file = folder.resolve("sub").resolve("save.properties");
        SaveData save = SaveData.load(file);
        save.remember(new Progress(List.of(0, 3), List.of("Split", "Lumière"), List.of(2), 5));
        save.complete(0);
        save.setWindowSize(WindowSize.HD);
        save.setInvincible(true);
        save.setVolume(40);
        save.controls().set(Controls.Action.UP, 0, KeyEvent.VK_I);
        save.save();

        SaveData loaded = SaveData.load(file);
        assertEquals(Set.of(0, 3, 5), loaded.checkpoints());
        assertEquals(List.of("Split", "Lumière"), List.copyOf(loaded.lights()));
        assertEquals(Set.of(2), loaded.doors());
        assertEquals(5, loaded.lastCheckpoint());
        assertEquals(1, loaded.completedLevels());
        assertEquals(WindowSize.HD, loaded.windowSize());
        assertTrue(loaded.isInvincible());
        assertEquals(40, loaded.volume());
        assertEquals(KeyEvent.VK_I, loaded.controls().key(Controls.Action.UP, 0));
    }

    @Test
    void theVolumeIsAtItsLoudestUntilThePlayerChangesIt() throws IOException {
        Path file = folder.resolve("save.properties");
        assertEquals(SaveData.MAX_VOLUME, SaveData.load(file).volume());
        Files.writeString(file, "lights=Split\n");
        SaveData old = SaveData.load(file);
        assertEquals(SaveData.MAX_VOLUME, old.volume(), "a save written before the volume existed");
        assertEquals(List.of("Split"), List.copyOf(old.lights()));
    }

    @Test
    void theVolumeStaysBetweenSilenceAndItsLoudest() throws IOException {
        Path file = folder.resolve("save.properties");
        SaveData save = SaveData.load(file);
        save.setVolume(250);
        assertEquals(100, save.volume());
        save.setVolume(-20);
        assertEquals(0, save.volume());
        Files.writeString(file, "volume=900\nlights=Split\n");
        SaveData loaded = SaveData.load(file);
        assertEquals(100, loaded.volume(), "a value written by hand is brought back in");
        assertEquals(List.of("Split"), List.copyOf(loaded.lights()));
    }

    @Test
    void whatWasReachedStaysReached() {
        SaveData save = SaveData.load(folder.resolve("save.properties"));
        save.remember(new Progress(List.of(3), List.of("Dash"), List.of(), 3));
        save.remember(new Progress(List.of(), List.of(), List.of(), 1));
        assertEquals(Set.of(1, 3), save.checkpoints());
        assertEquals(Set.of("Dash"), save.lights());
        assertEquals(1, save.lastCheckpoint());
    }

    @Test
    void replayingAnOlderLevelChangesNothing() {
        SaveData save = SaveData.load(folder.resolve("save.properties"));
        save.complete(2);
        save.complete(0);
        assertEquals(3, save.completedLevels());
    }

    @Test
    void anUnreadableFileIsIgnored() throws IOException {
        Path file = folder.resolve("broken.properties");
        Files.writeString(file, "completedLevels=abc\n");
        SaveData save = SaveData.load(file);
        assertEquals(0, save.completedLevels());
    }
}
