package screen;

import audio.Music;
import audio.Track;
import input.Gamepad;
import input.GamepadButton;
import input.GamepadListener;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import save.Controls;
import save.SaveData;
import world.Progress;
import save.WindowSize;
import zone.Levels;

/**
 * Owns the window, the save, the controller, the music and the screens (menu, checkpoint map, controls or game), and
 * switches between them. The controller is read about 60 times a second; each button pressed goes to the screen
 * being shown, which handles it as a {@link GamepadListener}. The menus have their own music; in game, the game
 * screen asks for the track of the zone the player is in.
 */
public class Game {
    private static final String MENU = "menu";
    private static final String MAP = "map";
    private static final String CONTROLS = "controls";
    private static final String PLAY = "play";
    private static final int GAMEPAD_DELAY_MS = 16;

    private final JFrame frame;
    private final CardLayout cards = new CardLayout();
    private final JPanel screens = new JPanel(cards);
    private final SaveData save;
    private final MenuPanel menu;
    private final MapPanel map;
    private final ControlsPanel controlsPanel;
    private final GamePanel gamePanel;
    private final Gamepad gamepad;
    private final Music music;
    private JComponent current;

    /**
     * Opens the window on the start menu, at the saved window size, starts reading the controller and plays the
     * music of the menus.
     */
    public Game() {
        this(SaveData.load(), new JFrame("Aquascape"), Gamepad.open(), Music.open());
    }

    /**
     * Builds the game on a save without opening a window, reading a controller or playing music: the screens work
     * but nothing is shown, which lets the tests drive them.
     * @param save the save to read and write
     */
    Game(SaveData save) {
        this(save, Gamepad.none());
    }

    /**
     * Builds the game on a save and a controller without opening a window or playing music; the tests poll the
     * controller themselves.
     * @param save the save to read and write
     * @param gamepad the controller
     */
    Game(SaveData save, Gamepad gamepad) {
        this(save, gamepad, Music.none());
    }

    /**
     * Builds the game on a save, a controller and a music without opening a window; the tests look at the tracks
     * the music is asked for.
     * @param save the save to read and write
     * @param gamepad the controller
     * @param music the music
     */
    Game(SaveData save, Gamepad gamepad, Music music) {
        this(save, null, gamepad, music);
    }

    private Game(SaveData save, JFrame frame, Gamepad gamepad, Music music) {
        this.save = save;
        this.frame = frame;
        this.gamepad = gamepad;
        this.music = music;
        menu = new MenuPanel(this);
        map = new MapPanel(this);
        controlsPanel = new ControlsPanel(this);
        gamePanel = new GamePanel(this);
        screens.add(menu, MENU);
        screens.add(map, MAP);
        screens.add(controlsPanel, CONTROLS);
        screens.add(gamePanel, PLAY);
        gamepad.addListener(this::toScreen);
        music.setVolume(save.volume() / (double) SaveData.MAX_VOLUME);
        if (frame != null) {
            new Timer(GAMEPAD_DELAY_MS, e -> gamepad.poll()).start();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(screens);
            WindowSize size = save.windowSize();
            applyWindowSize(WindowSize.available().contains(size) ? size : WindowSize.DEFAULT);
        }
        showMenu();
    }

    /** Shows the start menu. */
    public void showMenu() {
        gamePanel.stop();
        menu.showMain();
        show(MENU, menu);
    }

    /** Shows the settings, a page of the start menu. */
    public void showSettings() {
        gamePanel.stop();
        menu.showSettings();
        show(MENU, menu);
    }

    /** Shows the checkpoint map. */
    public void showMap() {
        gamePanel.stop();
        map.open();
        show(MAP, map);
    }

    /** Shows the controls screen. */
    public void showControls() {
        gamePanel.stop();
        controlsPanel.open();
        show(CONTROLS, controlsPanel);
    }

    private void show(String card, JComponent screen) {
        current = screen;
        cards.show(screens, card);
        if (screen != gamePanel) {
            music.play(Track.MENU);
        }
    }

    /** {@return the controller, which is {@link Gamepad#none()} when none can be used} */
    public Gamepad gamepad() {
        return gamepad;
    }

    /** {@return the music, which is {@link Music#none()} when no sound is wanted} */
    public Music music() {
        return music;
    }

    /** {@return the game screen, for the tests} */
    GamePanel gamePanel() {
        return gamePanel;
    }

    /** {@return the player's key bindings} */
    public Controls controls() {
        return save.controls();
    }

    /** Saves the key bindings. */
    public void saveControls() {
        save.save();
    }

    /**
     * Starts the world from a checkpoint, with the saved progress.
     * @param checkpoint index of a validated checkpoint
     */
    public void play(int checkpoint) {
        show(PLAY, gamePanel);
        gamePanel.start(Levels.WORLD, savedProgress(checkpoint));
    }

    /** Starts the world from the last checkpoint touched, or from the start. */
    public void continueGame() {
        play(isValidated(save.lastCheckpoint()) ? save.lastCheckpoint() : 0);
    }

    /** Starts the test room. */
    public void playTest() {
        show(PLAY, gamePanel);
        gamePanel.start(Levels.TEST, new Progress());
    }

    /** Starts the training room of the dash, with the lights already brought back so the dash works there. */
    public void playTraining() {
        show(PLAY, gamePanel);
        gamePanel.start(Levels.TRAINING, trainingProgress());
    }

    /** {@return a progress for the training room: the saved lights, but no checkpoint or door of the world} */
    public Progress trainingProgress() {
        return new Progress(List.of(), save.lights(), List.of(), 0);
    }

    /**
     * Builds a progress from the save, starting at a checkpoint.
     * @param checkpoint index of the checkpoint to start from
     * @return the progress
     */
    public Progress savedProgress(int checkpoint) {
        return new Progress(save.checkpoints(), save.lights(), save.doors(), checkpoint);
    }

    /**
     * Builds the progress to restart a level with.
     * @param level index of the level
     * @param checkpoint checkpoint the game started from
     * @return the saved progress for the world, the training progress for the training room, an empty one for
     *     the test room
     */
    public Progress restartProgress(int level, int checkpoint) {
        if (level == Levels.WORLD) {
            return savedProgress(checkpoint);
        }
        return level == Levels.TRAINING ? trainingProgress() : new Progress();
    }

    /**
     * Saves the progress of a game; only the world is saved, not the test or training rooms.
     * @param level index of the level
     * @param progress the progress to save
     */
    public void saveProgress(int level, Progress progress) {
        if (level == Levels.WORLD) {
            save.remember(progress);
            save.save();
        }
    }

    /**
     * Tells whether a checkpoint can be chosen on the map: the start always, the others once reached in a game.
     * @param checkpoint index of the checkpoint
     * @return {@code true} if it can be chosen
     */
    public boolean isValidated(int checkpoint) {
        return checkpoint == 0 || save.checkpoints().contains(checkpoint);
    }

    /** {@return whether the player has reached a checkpoint beyond the start} */
    public boolean hasStarted() {
        return save.checkpoints().stream().anyMatch(checkpoint -> checkpoint != 0);
    }

    /** {@return the last checkpoint touched, as saved} */
    public int lastCheckpoint() {
        return save.lastCheckpoint();
    }

    /**
     * Records a finished level.
     * @param level index of the level
     */
    public void completeLevel(int level) {
        save.complete(level);
        save.save();
    }

    /**
     * Tells whether a level was finished.
     * @param level index of the level
     * @return {@code true} if it was finished
     */
    public boolean isCompleted(int level) {
        return level < save.completedLevels();
    }

    /** {@return whether the invincible mode is on: the player's drop can't die} */
    public boolean isInvincible() {
        return save.isInvincible();
    }

    /**
     * Turns the invincible mode on or off and saves it; it applies from the next time a level starts, and the
     * game screen gives it at once to the world being played when it is changed from the pause.
     * @param invincible whether the player's drop can't die
     */
    public void setInvincible(boolean invincible) {
        save.setInvincible(invincible);
        save.save();
    }

    /** {@return the volume of the music, in percent} */
    public int volume() {
        return save.volume();
    }

    /**
     * Changes the volume of the music at once and saves it.
     * @param percent the volume, from 0 (silent) to 100
     */
    public void setVolume(int percent) {
        save.setVolume(percent);
        save.save();
        music.setVolume(save.volume() / (double) SaveData.MAX_VOLUME);
    }

    /** {@return the window size} */
    public WindowSize windowSize() {
        return save.windowSize();
    }

    /**
     * Changes and saves the window size.
     * @param size the new size
     */
    public void setWindowSize(WindowSize size) {
        save.setWindowSize(size);
        save.save();
        applyWindowSize(size);
    }

    private void applyWindowSize(WindowSize size) {
        if (frame == null) {
            return;
        }
        boolean fullScreen = size == WindowSize.FULL_SCREEN;
        if (frame.isUndecorated() != fullScreen) {
            frame.dispose(); // the title bar can only be added or removed while the window is hidden
            frame.setUndecorated(fullScreen);
        }
        if (fullScreen) {
            frame.setBounds(frame.getGraphicsConfiguration().getBounds());
        } else {
            screens.setPreferredSize(new Dimension(size.width(), size.height()));
            frame.pack();
            frame.setLocationRelativeTo(null);
        }
        frame.setVisible(true);
    }

    /** Closes the window, stops the music, lets go of the controller and ends the game. */
    public void quit() {
        music.stop();
        gamepad.close();
        if (frame != null) {
            frame.dispose();
        }
        System.exit(0);
    }

    private void toScreen(GamepadButton button) {
        if (current instanceof GamepadListener screen) {
            screen.pressed(button);
        }
    }
}
