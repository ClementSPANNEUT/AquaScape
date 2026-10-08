package screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static screen.Swing.binding;
import static screen.Swing.click;
import static screen.Swing.move;
import static screen.Swing.onEdt;
import static screen.Swing.paint;

import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import save.SaveData;
import world.Checkpoint;
import world.Progress;
import world.World;
import zone.Levels;

class MapPanelTest {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final List<Checkpoint> CHECKPOINTS = new World(Levels.get(Levels.WORLD)).checkpoints();

    @TempDir
    Path folder;
    private SaveData save;
    private RecordingGame game;
    private MapPanel map;

    @BeforeEach
    void freshSave() {
        save = SaveData.load(folder.resolve("save.properties"));
    }

    private void open() throws Exception {
        onEdt(() -> {
            game = new RecordingGame(save);
            map = new MapPanel(game);
            map.setSize(WIDTH, HEIGHT);
            map.open();
            game.calls().clear();
        });
    }

    private void validateEverything(int last) {
        List<Integer> all = IntStream.range(0, CHECKPOINTS.size()).boxed().toList();
        save.remember(new Progress(all, List.of("Split", "Fin"), List.of(), last));
        save.complete(Levels.WORLD);
    }

    private int played() {
        String call = game.calls().get(game.calls().size() - 1);
        assertTrue(call.startsWith("play "), call);
        return Integer.parseInt(call.substring("play ".length()));
    }

    @Test
    void aFreshSaveOnlyOffersTheStart() throws Exception {
        open();
        onEdt(() -> {
            binding(map, KeyEvent.VK_RIGHT);
            binding(map, KeyEvent.VK_DOWN);
            binding(map, KeyEvent.VK_ENTER);
            assertEquals(List.of("play 0"), game.calls());
        });
    }

    @Test
    void theArrowsGoToTheNextValidatedCheckpointInThatDirection() throws Exception {
        validateEverything(0);
        open();
        onEdt(() -> {
            binding(map, KeyEvent.VK_RIGHT);
            binding(map, KeyEvent.VK_ENTER);
            int right = played();
            assertTrue(CHECKPOINTS.get(right).x() > CHECKPOINTS.get(0).x());

            binding(map, KeyEvent.VK_LEFT);
            binding(map, KeyEvent.VK_ENTER);
            assertTrue(CHECKPOINTS.get(played()).x() < CHECKPOINTS.get(right).x());

            binding(map, KeyEvent.VK_UP);
            binding(map, KeyEvent.VK_DOWN);
            binding(map, KeyEvent.VK_SPACE);
            assertTrue(game.isValidated(played()));
        });
    }

    @Test
    void theMapOpensOnTheLastCheckpointTouched() throws Exception {
        validateEverything(3);
        open();
        onEdt(() -> {
            binding(map, KeyEvent.VK_ENTER);
            assertEquals(3, played());
        });
    }

    @Test
    void aCheckpointCanBePickedWithTheMouse() throws Exception {
        validateEverything(0);
        open();
        onEdt(() -> {
            paint(map, WIDTH, HEIGHT);
            for (int y = 100; y < HEIGHT && game.calls().isEmpty(); y += 3) {
                for (int x = 0; x < WIDTH && game.calls().isEmpty(); x += 3) {
                    move(map, x, y);
                    click(map, x, y);
                }
            }
            assertTrue(game.isValidated(played()));
        });
    }

    @Test
    void theBackButtonAndEscapeGoBackToTheMenu() throws Exception {
        open();
        onEdt(() -> {
            move(map, 50, 30);
            paint(map, WIDTH, HEIGHT);
            click(map, 50, 30);
            binding(map, KeyEvent.VK_ESCAPE);
            assertEquals(List.of("menu", "menu"), game.calls());
        });
    }

    @Test
    void itDrawsTheWorldItsCheckpointsAndItsLights() throws Exception {
        open();
        BufferedImage[] fresh = new BufferedImage[1];
        onEdt(() -> fresh[0] = paint(map, WIDTH, HEIGHT));
        validateEverything(0);
        open();
        onEdt(() -> {
            BufferedImage done = paint(map, WIDTH, HEIGHT);
            assertTrue(Swing.colours(done) > 20);
            assertFalse(Swing.same(fresh[0], done), "validated checkpoints and lights look different");
        });
    }
}
