package screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static screen.Swing.onEdt;
import static screen.Swing.press;

import audio.FakeMusic;
import audio.Track;
import input.Gamepad;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import save.SaveData;
import world.Checkpoint;
import world.Progress;
import world.World;
import zone.Levels;

class GameMusicTest {
    @TempDir
    Path folder;
    private FakeMusic music;
    private Game game;
    private GamePanel panel;

    @BeforeEach
    void open() throws Exception {
        onEdt(() -> {
            music = new FakeMusic();
            game = new Game(SaveData.load(folder.resolve("save.properties")), Gamepad.none(), music);
            panel = game.gamePanel();
            panel.setSize(1280, 720);
        });
    }

    @AfterEach
    void close() throws Exception {
        onEdt(game::showMenu);
    }

    private int checkpointIn(String zone) {
        game.play(0);
        World world = panel.world();
        List<Checkpoint> checkpoints = world.checkpoints();
        for (int i = 0; i < checkpoints.size(); i++) {
            Checkpoint checkpoint = checkpoints.get(i);
            if (world.planAt(checkpoint.x(), checkpoint.y()).name().equals(zone)) {
                return i;
            }
        }
        throw new AssertionError("no checkpoint in " + zone);
    }

    private void playFrom(int checkpoint) {
        panel.start(Levels.WORLD, new Progress(List.of(), List.of(), List.of(), checkpoint));
        panel.update();
    }

    @Test
    void theMenusShareOneTrack() throws Exception {
        onEdt(() -> {
            assertEquals(Track.MENU, music.playing());
            game.showMap();
            game.showSettings();
            game.showControls();
            game.showMenu();
            assertEquals(List.of(Track.MENU), music.started(), "it goes on from one menu to the other");
        });
    }

    @Test
    void theGamePlaysTheTrackOfTheZoneThePlayerIsIn() throws Exception {
        onEdt(() -> {
            playFrom(0);
            assertEquals(Track.HUB, music.playing());
            playFrom(checkpointIn("Split"));
            assertEquals(Track.SPLIT, music.playing());
            playFrom(checkpointIn("Dash"));
            assertEquals(Track.DASH, music.playing());
            playFrom(checkpointIn("Shoot"));
            assertEquals(Track.SHOOT, music.playing());
            playFrom(checkpointIn("Pary"));
            assertEquals(Track.PARY, music.playing());
            playFrom(checkpointIn("Lumière"));
            assertEquals(Track.LIGHT, music.playing());
            playFrom(checkpointIn("Fin"));
            assertEquals(Track.END, music.playing());
        });
    }

    @Test
    void aZoneWithoutItsFileKeepsTheMusicOfTheHub() throws Exception {
        onEdt(() -> {
            music.lose(Track.SHOOT);
            playFrom(0);
            playFrom(checkpointIn("Shoot"));
            assertEquals(Track.HUB, music.playing());
            assertEquals(List.of(Track.MENU, Track.HUB), music.started());
        });
    }

    @Test
    void comingBackToLifeDoesNotStartTheTrackAgain() throws Exception {
        onEdt(() -> {
            int checkpoint = checkpointIn("Split");
            playFrom(checkpoint);
            int started = music.started().size();
            playFrom(checkpoint);
            for (int frame = 0; frame < 5; frame++) {
                panel.update();
            }
            assertEquals(started, music.started().size());
        });
    }

    @Test
    void thePauseKeepsTheTrackOfTheZone() throws Exception {
        onEdt(() -> {
            playFrom(checkpointIn("Split"));
            int started = music.started().size();
            press(panel, KeyEvent.VK_ESCAPE);
            panel.update();
            assertEquals(Track.SPLIT, music.playing());
            assertEquals(started, music.started().size());
        });
    }

    @Test
    void leavingTheGameBringsBackTheTrackOfTheMenus() throws Exception {
        onEdt(() -> {
            playFrom(0);
            game.showMenu();
            assertEquals(List.of(Track.MENU, Track.HUB, Track.MENU), music.started());
        });
    }

    @Test
    void theRoomsHaveTheirTrack() throws Exception {
        onEdt(() -> {
            game.playTraining();
            panel.update();
            assertEquals(Track.TRAINING, music.playing());
            game.playTest();
            panel.update();
            assertEquals(Track.HUB, music.playing(), "the test room has no track of its own");
        });
    }
}
