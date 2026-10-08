package screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static screen.Swing.click;
import static screen.Swing.move;
import static screen.Swing.onEdt;
import static screen.Swing.paint;
import static screen.Swing.press;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import save.Controls.Action;
import save.SaveData;

class ControlsPanelTest {
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 768;

    @TempDir
    Path folder;
    private SaveData save;
    private RecordingGame game;
    private ControlsPanel panel;

    @BeforeEach
    void open() throws Exception {
        save = SaveData.load(folder.resolve("save.properties"));
        onEdt(() -> {
            game = new RecordingGame(save);
            panel = new ControlsPanel(game);
            panel.open();
            game.calls().clear();
        });
    }

    @Test
    void aKeyCanBeChangedAndIsSaved() throws Exception {
        onEdt(() -> {
            press(panel, KeyEvent.VK_ENTER);
            press(panel, KeyEvent.VK_K);
            assertEquals(KeyEvent.VK_K, save.controls().key(Action.UP, 0));
            assertEquals(List.of("save controls"), game.calls());
        });
    }

    @Test
    void escapeGoesBackToTheSettings() throws Exception {
        onEdt(() -> {
            press(panel, KeyEvent.VK_ESCAPE);
            assertEquals(List.of("settings"), game.calls());
        });
    }

    @Test
    void theLowestRightCellIsRetour() throws Exception {
        onEdt(() -> {
            assertTrue(Swing.colours(paint(panel, WIDTH, HEIGHT)) > 10);
            for (int y = HEIGHT - 1; y >= 0 && game.calls().isEmpty(); y -= 4) {
                for (int x = WIDTH - 1; x >= 0 && game.calls().isEmpty(); x -= 4) {
                    move(panel, x, y);
                    click(panel, x, y);
                }
            }
            assertEquals(List.of("settings"), game.calls());
        });
    }
}
