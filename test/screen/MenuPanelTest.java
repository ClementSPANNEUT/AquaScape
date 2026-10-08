package screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static screen.Swing.binding;
import static screen.Swing.click;
import static screen.Swing.move;
import static screen.Swing.onEdt;
import static screen.Swing.paint;

import input.GamepadButton;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import save.SaveData;
import save.WindowSize;
import world.Progress;

class MenuPanelTest {
    private static final int VOLUME_ROW = 2;

    @TempDir
    Path folder;
    private Path file;
    private SaveData save;
    private RecordingGame game;
    private MenuPanel menu;

    @BeforeEach
    void open() throws Exception {
        file = folder.resolve("save.properties");
        save = SaveData.load(file);
        onEdt(() -> {
            game = new RecordingGame(save);
            menu = new MenuPanel(game);
            menu.showMain();
            game.calls().clear();
        });
    }

    private static List<JButton> buttons(Container container) {
        List<JButton> buttons = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (child instanceof JButton button) {
                buttons.add(button);
            } else if (child instanceof Container inner) {
                buttons.addAll(buttons(inner));
            }
        }
        return buttons;
    }

    private List<String> texts() {
        return buttons(menu).stream().map(JButton::getText).toList();
    }

    private JButton button(String text) {
        return buttons(menu).stream().filter(b -> b.getText().equals(text)).findFirst().orElseThrow();
    }

    private void openSettings() {
        button("Réglages").doClick(0);
    }

    private void keys(int key, int times) {
        for (int i = 0; i < times; i++) {
            binding(menu, key);
        }
    }

    private void assertEveryButtonShows(int width, int height) {
        paint(menu, width, height);
        for (JButton button : buttons(menu)) {
            Rectangle box = SwingUtilities.convertRectangle(button, new Rectangle(button.getSize()), menu);
            assertTrue(new Rectangle(width, height).contains(box), button.getText() + " is at " + box);
        }
    }

    @Test
    void theMainMenuHasOneEntryForTheSettings() throws Exception {
        onEdt(() -> assertEquals(List.of("Jouer", "Carte des checkpoints", "Salle de test", "Réglages", "Quitter"),
                texts()));
    }

    @Test
    void theKeysReachEveryChoiceOfTheMainMenu() throws Exception {
        onEdt(() -> {
            binding(menu, KeyEvent.VK_ENTER);
            binding(menu, KeyEvent.VK_DOWN);
            binding(menu, KeyEvent.VK_SPACE);
            binding(menu, KeyEvent.VK_S);
            binding(menu, KeyEvent.VK_D);
            binding(menu, KeyEvent.VK_DOWN);
            binding(menu, KeyEvent.VK_DOWN);
            binding(menu, KeyEvent.VK_RIGHT);
            binding(menu, KeyEvent.VK_DOWN);
            binding(menu, KeyEvent.VK_ENTER);
            binding(menu, KeyEvent.VK_Z);
            binding(menu, KeyEvent.VK_ENTER);
            binding(menu, KeyEvent.VK_ESCAPE);
            binding(menu, KeyEvent.VK_LEFT);
            assertEquals(List.of("continue", "map", "test", "quit", "continue", "quit"), game.calls());
        });
    }

    @Test
    void thePlayButtonSaysContinueOnceTheGameHasStarted() throws Exception {
        onEdt(() -> {
            assertEquals("Jouer", buttons(menu).get(0).getText());
            save.remember(new Progress(List.of(2), List.of(), List.of(), 2));
            menu.showMain();
            assertEquals("Continuer", buttons(menu).get(0).getText());
        });
    }

    @Test
    void theMouseSelectsTheButtonUnderIt() throws Exception {
        onEdt(() -> {
            move(button("Salle de test"), 5, 5);
            binding(menu, KeyEvent.VK_ENTER);
            assertEquals(List.of("test"), game.calls());
        });
    }

    @Test
    void theSettingsGatherTheControlsTheWindowSizeTheVolumeAndTheInvincibleMode() throws Exception {
        onEdt(() -> {
            openSettings();
            assertEquals(List.of("Contrôles", "Taille de l'écran : " + WindowSize.DEFAULT.label(), "Volume : 100 %",
                    "Mode invincible : désactivé", "Retour"), texts());
        });
    }

    @Test
    void theSettingsOpenTheControls() throws Exception {
        onEdt(() -> {
            openSettings();
            binding(menu, KeyEvent.VK_ENTER);
            binding(menu, KeyEvent.VK_RIGHT);
            assertEquals(List.of("controls", "controls"), game.calls(), "Entrée and → both validate");
        });
    }

    @Test
    void leavingTheSettingsComesBackOnTheirEntry() throws Exception {
        onEdt(() -> {
            openSettings();
            binding(menu, KeyEvent.VK_LEFT);
            assertEquals("Jouer", texts().get(0), "← went back to the main menu");
            binding(menu, KeyEvent.VK_ENTER);
            assertEquals("Contrôles", texts().get(0), "Réglages was still selected");
            button("Retour").doClick(0);
            binding(menu, KeyEvent.VK_DOWN);
            binding(menu, KeyEvent.VK_ENTER);
            assertEquals(List.of("quit"), game.calls(), "Quitter is the button below Réglages");
        });
    }

    @Test
    void theSettingsChangeTheWindowSize() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "the sizes depend on the screen");
        onEdt(() -> {
            openSettings();
            binding(menu, KeyEvent.VK_DOWN);
            binding(menu, KeyEvent.VK_ENTER);
            List<WindowSize> sizes = WindowSize.available();
            assertEquals(sizes.size() + 1, buttons(menu).size(), "one button per size, and Retour");
            binding(menu, KeyEvent.VK_DOWN);
            binding(menu, KeyEvent.VK_ENTER);
            assertEquals(sizes.get(1), save.windowSize());
            assertEquals(sizes.get(1), game.windowSize());
            assertEquals(sizes.get(1), SaveData.load(file).windowSize(), "it is saved at once");

            binding(menu, KeyEvent.VK_ESCAPE);
            assertEquals("Taille de l'écran : " + sizes.get(1).label(), texts().get(1), "back in the settings");
            binding(menu, KeyEvent.VK_ENTER);
            assertEquals(sizes.size() + 1, buttons(menu).size(), "the size was still selected, so Entrée reopens it");
            button("Retour").doClick(0);
            assertEquals("Contrôles", texts().get(0));
        });
    }

    @Test
    void theSettingsTurnTheInvincibleModeOnAndOff() throws Exception {
        onEdt(() -> {
            openSettings();
            assertFalse(game.isInvincible());
            button("Mode invincible : désactivé").doClick(0);
            assertTrue(game.isInvincible());
            assertTrue(SaveData.load(file).isInvincible(), "it is saved at once");

            binding(menu, KeyEvent.VK_ENTER);
            assertFalse(game.isInvincible(), "the button stays selected, so Entrée turns it off again");
            assertEquals("Mode invincible : désactivé", button("Mode invincible : désactivé").getText());
        });
    }

    @Test
    void theArrowsChangeTheVolumeWhichIsSavedAtOnce() throws Exception {
        onEdt(() -> {
            openSettings();
            keys(KeyEvent.VK_DOWN, VOLUME_ROW);
            binding(menu, KeyEvent.VK_LEFT);
            assertEquals(90, game.volume());
            assertEquals(0.9, game.music().volume(), 1e-9, "the music is turned down at once");
            assertEquals(90, SaveData.load(file).volume(), "it is saved at once");
            assertEquals("Volume : 90 %", texts().get(VOLUME_ROW));

            binding(menu, KeyEvent.VK_Q);
            binding(menu, KeyEvent.VK_A);
            binding(menu, KeyEvent.VK_D);
            assertEquals(80, game.volume(), "Q, A and D do the same as the arrows");
            assertEquals(List.of(), game.calls());
            assertEquals(5, buttons(menu).size(), "← did not leave the settings");
        });
    }

    @Test
    void theVolumeStopsAtSilenceAndAtItsLoudest() throws Exception {
        onEdt(() -> {
            openSettings();
            keys(KeyEvent.VK_DOWN, VOLUME_ROW);
            keys(KeyEvent.VK_RIGHT, 3);
            assertEquals(100, game.volume());
            keys(KeyEvent.VK_LEFT, 14);
            assertEquals(0, game.volume());
            assertEquals(0, game.music().volume(), 1e-9);
            assertEquals("Volume : 0 %", texts().get(VOLUME_ROW));
            binding(menu, KeyEvent.VK_ENTER);
            assertEquals(0, game.volume(), "Entrée does nothing on the volume");
        });
    }

    @Test
    void theSettingsShowTheSavedVolume() throws Exception {
        onEdt(() -> {
            game.setVolume(40);
            menu.showSettings();
            assertEquals("Volume : 40 %", texts().get(VOLUME_ROW));
        });
    }

    @Test
    void aClickOnTheBarSetsTheVolume() throws Exception {
        onEdt(() -> {
            openSettings();
            paint(menu, 800, 600);
            JButton volume = buttons(menu).get(VOLUME_ROW);
            click(volume, volume.getWidth() / 2, volume.getHeight() / 2);
            assertTrue(game.volume() > 0 && game.volume() < 100, "set to " + game.volume());
            assertEquals(game.volume(), SaveData.load(file).volume());
            binding(menu, KeyEvent.VK_ENTER);
            assertEquals(List.of("controls"), game.calls(), "a click does not move the selection of the keyboard");
        });
    }

    @Test
    void theControllerChangesTheVolumeWithSidewaysPushes() throws Exception {
        onEdt(() -> {
            openSettings();
            menu.pressed(GamepadButton.RIGHT);
            menu.pressed(GamepadButton.LEFT);
            assertEquals(List.of(), game.calls());
            assertEquals("Contrôles", texts().get(0), "sideways pushes do nothing on a button");

            menu.pressed(GamepadButton.DOWN);
            menu.pressed(GamepadButton.DOWN);
            menu.pressed(GamepadButton.LEFT);
            menu.pressed(GamepadButton.LEFT);
            menu.pressed(GamepadButton.RIGHT);
            assertEquals(90, game.volume());
            menu.pressed(GamepadButton.A);
            assertEquals(90, game.volume());
            menu.pressed(GamepadButton.B);
            assertEquals("Jouer", texts().get(0), "B leaves the settings");
        });
    }

    @Test
    void everyPageFitsTheSmallestWindow() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "the sizes depend on the screen");
        onEdt(() -> {
            assertEveryButtonShows(WindowSize.SMALL.width(), WindowSize.SMALL.height());
            openSettings();
            assertEveryButtonShows(WindowSize.SMALL.width(), WindowSize.SMALL.height());
            button("Taille de l'écran : " + WindowSize.DEFAULT.label()).doClick(0);
            assertEveryButtonShows(WindowSize.SMALL.width(), WindowSize.SMALL.height());
        });
    }

    @Test
    void itPaintsTheBackgroundAndTheButtons() throws Exception {
        onEdt(() -> {
            assertTrue(Swing.colours(paint(menu, 800, 600)) > 20);
            openSettings();
            assertTrue(Swing.colours(paint(menu, 800, 600)) > 20);
        });
    }
}
