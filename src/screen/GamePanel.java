package screen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import audio.Track;
import enemy.ability.Ability;
import enemy.ability.Slide;
import enemy.ability.Shoot;
import fluid.Droplet;
import fluid.FluidRenderer;
import input.Gamepad;
import input.GamepadButton;
import input.GamepadListener;
import save.Controls;
import save.WindowSize;
import world.Plan;
import world.Progress;
import world.World;
import zone.Levels;

/**
 * The game screen: runs the game loop (about 60 frames a second), reads the keys, moves the camera, draws the
 * world and the HUD, and shows the pop-ups for the pause, its settings, the controls, a newly learnt ability and
 * the victory. The settings of the pause are those of the start menu: the controls, the window size, the volume
 * and the invincible mode, which is applied at once to the world being played.
 * <p>The dash key is read every frame: held, it sticks the back of the drop and lets its head stretch; let go, it
 * throws the drop. So is the shoot key: held, it charges a shot aimed with the right stick of the controller, or
 * turned step by step with the two aim keys, which stop steering the drop meanwhile; let go, it fires. And so is
 * the slide key: a press makes the drop keep its speed and its direction, another one gives the control back. With
 * a controller, the stick or the arrows move the drop, Start pauses, and the buttons chosen
 * in the controls (A, X, Y and Select by default) dash, change drop, restart and show the grid. In the pop-ups, up
 * and down choose, A validates and B or Start go back; left and right only change the volume.
 *
 * @serial exclude
 */
public class GamePanel extends JPanel implements GamepadListener {
    private static final int FRAME_DELAY_MS = 16; // ~60 FPS
    private static final double MAX_FRAME_TIME = 0.05; 
    private static final double RESPAWN_DELAY = 0.8;
    private static final double MESSAGE_TIME = 3;
    private static final Color LIGHT_TEXT = new Color(120, 240, 255);
    private static final Color GAUGE_OK = new Color(80, 170, 255);
    private static final Color GAUGE_DANGER = new Color(255, 90, 80);
    private static final Color BUTTON_COLOR = new Color(40, 90, 160);
    private static final Color GEAR_COLOR = new Color(170, 180, 200);
    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    private static final Font DEATH_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 40);
    private static final Font POPUP_TEXT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
    private static final Font MESSAGE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    private static final int POPUP_PADDING = 24;
    private static final int POPUP_SIDE = 56;
    private static final int TEXT_LINE = 24;
    private static final long ABILITY_INPUT_DELAY = 500_000_000L;
    private static final Color DASH_WAITING = new Color(140, 145, 165);
    private static final double TEAR_WARNING = 0.85;
    private static final int POPUP_HINT_SPACE = 34;
    private static final int CONTROLS_PADDING = 28;
    private static final Font BUTTON_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 17);
    private static final Font HINT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final int BUTTON_WIDTH = 300;
    private static final int SETTINGS_BUTTON_WIDTH = 440;
    private static final Color CURRENT_COLOR = new Color(35, 140, 100);
    private static final String SETTINGS_LABEL = "Réglages";
    private static final String CONTROLS_LABEL = "Contrôles";
    private static final String WINDOW_SIZE_LABEL = "Taille de l'écran";
    private static final String VOLUME_LABEL = "Volume";
    private static final String BACK_LABEL = "Retour";
    private static final int WINDOW_SIZE_ROW = 1;
    private static final int VOLUME_ROW = 2;
    private static final int INVINCIBLE_ROW = 3;
    private static final int BUTTON_HEIGHT = 46;
    private static final int BUTTON_GAP = 12;
    private static final Font POINTER_FONT = new Font(Font.MONOSPACED, Font.BOLD, 13);
    private static final Color POINTER_BACK = new Color(5, 8, 18, 200);
    private static final Color POINTER_TEXT = new Color(255, 215, 100);
    private static final int COMPASS_INSET = 70;

    private enum Page { NONE, PAUSE, SETTINGS, WINDOW_SIZES, CONTROLS, ABILITY, WIN }

    private final Game game;
    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Camera camera = new Camera();
    private final Timer timer = new Timer(FRAME_DELAY_MS, e -> {
        update();
        repaint();
    });
    private World world;
    private int levelIndex;
    private boolean won;
    private long lastFrameTime;
    private Progress progress = new Progress();
    private int startCheckpoint;
    private String message;
    private double messageTime;
    private Page page = Page.NONE;
    private long pageOpenedAt;
    private Ability learnt;
    private int selected;
    private boolean gearHovered;
    private Shape gear;
    private Rectangle gearBox;
    private boolean showGrid = true;
    private int mouseX = -1;
    private int mouseY = -1;
    private final ControlsEditor controlsEditor;
    private List<WindowSize> sizes = List.of();

    /**
     * Creates the game screen.
     * @param game the game, to switch screens and reach the save
     */
    public GamePanel(Game game) {
        this.game = game;
        controlsEditor = new ControlsEditor(game.controls(), game.gamepad(), game::saveControls,
                () -> openPage(Page.SETTINGS));
        setBackground(Ui.BACKGROUND);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (page == Page.CONTROLS) {
                    e.consume();
                    controlsEditor.keyPressed(key);
                    return;
                }
                if (page != Page.NONE) {
                    e.consume();
                    pageKey(key);
                    return;
                }
                pressedKeys.add(key);
                if (key == KeyEvent.VK_ESCAPE) {
                    e.consume();
                    openPage(Page.PAUSE);
                } else if (controls().matches(Controls.Action.GRID, key)) {
                    showGrid = !showGrid;
                } else if (controls().matches(Controls.Action.RESTART, key)) {
                    restart();
                } else if (controls().matches(Controls.Action.SWAP, key)) {
                    world.switchDrop();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                pressedKeys.remove(e.getKeyCode());
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                gearHovered = gearBounds().contains(e.getPoint());
                if (page == Page.CONTROLS) {
                    controlsEditor.mouseMoved(e.getX(), e.getY());
                    return;
                }
                int button = buttonAt(e.getX(), e.getY());
                if (page != Page.NONE && button >= 0) {
                    selected = button;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (isVolume(buttonAt(e.getX(), e.getY()))) {
                    pickVolume(e.getX());
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (page == Page.CONTROLS) {
                    controlsEditor.mousePressed(e.getX(), e.getY());
                    return;
                }
                int button = buttonAt(e.getX(), e.getY());
                if (isVolume(button)) {
                    pickVolume(e.getX());
                } else if (page != Page.NONE && button >= 0) {
                    chooseAt(button);
                } else if (page == Page.NONE && gearBounds().contains(e.getPoint())) {
                    openPage(Page.PAUSE);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouseX = -1;
                mouseY = -1;
            }
        });
    }

    /**
     * Starts a level.
     * @param level index of the level
     * @param progress progress to start with
     */
    public void start(int level, Progress progress) {
        this.progress = progress;
        startCheckpoint = progress.checkpoint();
        message = null;
        load(level);
    }

    private void restart() {
        start(levelIndex, game.restartProgress(levelIndex, startCheckpoint));
    }

    /** {@return the world being played, or {@code null} before the first start} */
    World world() {
        return world;
    }

    /** Stops the game loop, when another screen shows. */
    public void stop() {
        timer.stop();
        pressedKeys.clear();
    }

    private void load(int level) {
        levelIndex = level;
        world = new World(Levels.get(level), progress);
        world.setInvincible(game.isInvincible());
        world.cancelShot();
        camera.snap(world, getWidth(), getHeight());
        won = false;
        page = Page.NONE;
        lastFrameTime = System.nanoTime();
        timer.start();
        requestFocusInWindow();
    }

    /** Runs one frame of the game loop: reads the keys, moves the world and the camera, and reacts to it. */
    void update() {
        if (page != Page.NONE) {
            return; // paused
        }
        long now = System.nanoTime();
        double dt = Math.min((now - lastFrameTime) / 1e9, MAX_FRAME_TIME);
        lastFrameTime = now;

        boolean steering = !won && !world.isPlayerDead();
        world.holdDash(steering && isHeld(Controls.Action.DASH));
        world.holdShot(steering && isHeld(Controls.Action.SHOOT), game.gamepad().aimX(), game.gamepad().aimY());
        world.turnAim((isPressed(Controls.Action.AIM_RIGHT) ? 1 : 0) - (isPressed(Controls.Action.AIM_LEFT) ? 1 : 0), dt);
        world.holdSlide(steering && isHeld(Controls.Action.SLIDE));
        world.update(dt, steering ? inputX() : 0, steering ? inputY() : 0);
        camera.follow(dt, world, getWidth(), getHeight());
        game.music().play(Track.ofZone(world.planAt(world.player().x(), world.player().y()).name()));
        if (progress.takeChanged()) {
            game.saveProgress(levelIndex, progress);
        }
        messageTime -= dt;
        String light = world.takeNewLight();
        if (light != null) {
            lightTaken(light);
            return;
        }
        // back to the last checkpoint
        if (world.isPlayerDead() && world.timeSinceDeath() >= RESPAWN_DELAY) {
            load(levelIndex);
            return;
        }
        if (!won && world.isComplete()) {
            won = true;
            game.completeLevel(levelIndex);
            openPage(Page.WIN);
        }
    }

    /**
     * Reacts to a light the player has just taken. A light that teaches an ability opens a pop-up that explains
     * it and offers its training room; another light sends the player back to the hub; the exit of the training
     * room goes back to the world.
     * @param light name of the light
     */
    void lightTaken(String light) {
        if (levelIndex == Levels.TRAINING) {
            game.continueGame();
            return;
        }
        Ability ability = Ability.unlockedBy(light);
        if (ability != null) {
            learnt = ability;
            openPage(Page.ABILITY);
            return;
        }
        showMessage("Lumière « " + light + " » récupérée : retour au hub");
        load(levelIndex);
    }

    private void showMessage(String text) {
        message = text;
        messageTime = MESSAGE_TIME;
    }

    /**
     * Handles a controller button while the game is shown: in game, Start pauses and the buttons chosen in the
     * controls change drop, restart or show the grid, the dash button being read every frame instead; in a pop-up,
     * up and down choose, A validates and B or Start go back, while sideways pushes only change the volume in the
     * settings; in the controls, the table gets the button.
     * @param button the button pressed
     */
    @Override
    public void pressed(GamepadButton button) {
        if (world == null || !timer.isRunning()) {
            return;
        }
        switch (page) {
            case NONE -> playButton(button);
            case CONTROLS -> controlsEditor.gamepadPressed(button);
            default -> pageButton(button);
        }
    }

    private void playButton(GamepadButton button) {
        if (button == GamepadButton.START) {
            openPage(Page.PAUSE);
        } else if (controls().matches(Controls.Action.GRID, button)) {
            showGrid = !showGrid;
        } else if (controls().matches(Controls.Action.RESTART, button)) {
            restart();
        } else if (controls().matches(Controls.Action.SWAP, button)) {
            world.switchDrop();
        }
    }

    private void pageButton(GamepadButton button) {
        if (isTooSoon()) {
            return;
        }
        switch (button) {
            case UP -> moveSelection(-1);
            case DOWN -> moveSelection(1);
            case LEFT -> turnVolume(-1);
            case RIGHT -> turnVolume(1);
            case A -> chooseAt(selected);
            case B, START -> goBack();
            default -> {
                if (page == Page.WIN && controls().matches(Controls.Action.RESTART, button)) {
                    restart();
                }
            }
        }
    }

    private int inputX() {
        int keys = (isSteering(Controls.Action.RIGHT) ? 1 : 0) - (isSteering(Controls.Action.LEFT) ? 1 : 0);
        return Integer.signum(keys + game.gamepad().directionX());
    }

    private int inputY() {
        int keys = (isSteering(Controls.Action.DOWN) ? 1 : 0) - (isSteering(Controls.Action.UP) ? 1 : 0);
        return Integer.signum(keys + game.gamepad().directionY());
    }

    private boolean isSteering(Controls.Action action) {
        if (!world.isChargingShot()) {
            return isPressed(action);
        }
        for (int slot = 0; slot < Controls.SLOTS; slot++) {
            int key = controls().key(action, slot);
            if (key != Controls.NONE && pressedKeys.contains(key) && !controls().matches(Controls.Action.AIM_LEFT, key)
                    && !controls().matches(Controls.Action.AIM_RIGHT, key)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPressed(Controls.Action action) {
        return controls().isPressed(action, pressedKeys);
    }

    private boolean isHeld(Controls.Action action) {
        GamepadButton button = controls().button(action);
        return isPressed(action) || button != null && game.gamepad().isHeld(button);
    }

    private Controls controls() {
        return game.controls();
    }

    private void openPage(Page newPage) {
        page = newPage;
        pageOpenedAt = System.nanoTime();
        selected = 0;
        pressedKeys.clear();
        if (world != null) {
            world.cancelDash();
            world.cancelShot();
            world.waitForSlideKey();
        }
        if (newPage == Page.CONTROLS) {
            controlsEditor.open();
        } else if (newPage == Page.WINDOW_SIZES) {
            sizes = WindowSize.available();
            selected = Math.max(0, sizes.indexOf(game.windowSize()));
        }
    }

    private List<String> choices() {
        return switch (page) {
            case WIN -> List.of("Voir la carte", "Rejouer", "Retour au menu");
            case ABILITY -> learnt == Ability.DASH ? List.of("Salle d'entraînement", "Retourner au hub")
                    : List.of("Retourner au hub");
            case SETTINGS -> List.of(CONTROLS_LABEL, WINDOW_SIZE_LABEL + " : " + game.windowSize().label(),
                    VOLUME_LABEL + " : " + game.volume() + " %",
                    "Mode invincible : " + (game.isInvincible() ? "activé" : "désactivé"), BACK_LABEL);
            case WINDOW_SIZES -> sizeChoices();
            default -> levelIndex == Levels.TRAINING
                    ? List.of("Reprendre", "Recommencer", SETTINGS_LABEL, "Retourner au hub", "Retour au menu")
                    : List.of("Reprendre", "Recommencer", SETTINGS_LABEL, "Carte", "Retour au menu");
        };
    }

    private List<String> sizeChoices() {
        List<String> labels = new ArrayList<>();
        for (WindowSize size : sizes) {
            labels.add(size.label());
        }
        labels.add(BACK_LABEL);
        return labels;
    }

    private void chooseAt(int index) {
        switch (page) {
            case SETTINGS -> chooseSetting(index);
            case WINDOW_SIZES -> chooseWindowSize(index);
            default -> choose(choices().get(index));
        }
    }

    private void chooseSetting(int row) {
        switch (row) {
            case 0 -> openPage(Page.CONTROLS);
            case WINDOW_SIZE_ROW -> openPage(Page.WINDOW_SIZES);
            case VOLUME_ROW -> {
            }
            case INVINCIBLE_ROW -> {
                game.setInvincible(!game.isInvincible());
                world.setInvincible(game.isInvincible());
            }
            default -> goBack();
        }
    }

    private void chooseWindowSize(int row) {
        if (row >= sizes.size()) {
            goBack();
            return;
        }
        game.setWindowSize(sizes.get(row));
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void goBack() {
        switch (page) {
            case SETTINGS -> {
                openPage(Page.PAUSE);
                selected = choices().indexOf(SETTINGS_LABEL);
            }
            case WINDOW_SIZES -> {
                openPage(Page.SETTINGS);
                selected = WINDOW_SIZE_ROW;
            }
            default -> choose(backChoice());
        }
    }

    private boolean isVolume(int row) {
        return page == Page.SETTINGS && row == VOLUME_ROW;
    }

    private boolean turnVolume(int steps) {
        if (!isVolume(selected)) {
            return false;
        }
        game.setVolume(Slider.moved(game.volume(), steps));
        return true;
    }

    private void pickVolume(int x) {
        int level = Slider.levelAt(buttonBounds(VOLUME_ROW), x);
        if (level >= 0 && level != game.volume()) {
            game.setVolume(level);
        }
    }

    private void sideways(int step) {
        if (turnVolume(step)) {
            return;
        }
        if (step > 0) {
            chooseAt(selected);
        } else {
            goBack();
        }
    }

    private void choose(String choice) {
        switch (choice) {
            case "Reprendre" -> {
                page = Page.NONE;
                lastFrameTime = System.nanoTime();
            }
            case "Recommencer", "Rejouer" -> restart();
            case SETTINGS_LABEL -> openPage(Page.SETTINGS);
            case "Carte", "Voir la carte" -> game.showMap();
            case "Salle d'entraînement" -> game.playTraining();
            case "Retourner au hub" -> returnToHub();
            default -> game.showMenu();
        }
    }

    private void returnToHub() {
        if (levelIndex == Levels.TRAINING) {
            game.continueGame();
            return;
        }
        showMessage("Compétence « " + learnt.label() + " » apprise : retour au hub");
        load(levelIndex);
    }

    private void pageKey(int key) {
        if (isTooSoon()) {
            return;
        }
        if (page == Page.WIN && controls().matches(Controls.Action.RESTART, key)) {
            restart();
            return;
        }
        switch (key) {
            case KeyEvent.VK_UP, KeyEvent.VK_Z, KeyEvent.VK_W -> moveSelection(-1);
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> moveSelection(1);
            case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> chooseAt(selected);
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> sideways(1);
            case KeyEvent.VK_ESCAPE -> goBack();
            case KeyEvent.VK_LEFT, KeyEvent.VK_Q, KeyEvent.VK_A -> sideways(-1);
            default -> {
            }
        }
    }

    private boolean isTooSoon() {
        return page == Page.ABILITY && System.nanoTime() - pageOpenedAt < ABILITY_INPUT_DELAY;
    }

    private void moveSelection(int step) {
        selected = Math.floorMod(selected + step, choices().size());
    }

    private String backChoice() {
        return switch (page) {
            case PAUSE -> "Reprendre";
            case ABILITY -> "Retourner au hub";
            default -> "Retour au menu";
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (world == null) {
            super.paintComponent(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Ui.paintBackground(g2, getWidth(), getHeight());
        // the world moved by the camera, on whole pixels so the water and the walls stay sharp
        Rectangle view = new Rectangle((int) Math.round(camera.x()), (int) Math.round(camera.y()),
                getWidth(), getHeight());
        if (!showGrid) {
            Backdrop.draw(g2, view.x, view.y, view.width, view.height);
        }
        Plan plan = world.planAt(world.player().x(), world.player().y());
        Graphics2D inWorld = (Graphics2D) g2.create();
        inWorld.translate(-view.x, -view.y);
        world.draw(inWorld, view);
        if (showGrid) {
            Grid.draw(inWorld, view, plan);
        }
        inWorld.dispose();
        drawHud(g2, plan);
        if (showGrid) {
            Grid.drawCompass(g2, plan, getWidth() - COMPASS_INSET, getHeight() - COMPASS_INSET);
            drawPointer(g2, view, plan);
        }
        if (page != Page.NONE) {
            drawPage(g2);
        } else {
            if (world.isPlayerDead()) {
                g2.setColor(FluidRenderer.NEON);
                g2.setFont(DEATH_FONT);
                drawCentered(g2, world.hasExploded() ? "Surcharge !" : "Touché !", getHeight() / 2);
            }
            drawGear(g2);
        }
        Toolkit.getDefaultToolkit().sync();
    }

    private void drawHud(Graphics2D g, Plan plan) {
        Droplet player = world.player();
        int bottom = getHeight();

        g.setColor(Color.WHITE);
        g.setFont(TITLE_FONT);
        String title = levelIndex == Levels.TRAINING ? world.level().name()
                : "Niveau " + (levelIndex + 1) + " · " + world.level().name();
        g.drawString(title, 12, 24);
        g.setFont(getFont());
        g.setColor(Color.LIGHT_GRAY);
        g.drawString(world.level().objective(), 12, 44);
        int line = 64;
        if (levelIndex == Levels.WORLD && world.lightCount() > 0) {
            List<String> obtained = world.obtainedLights();
            g.setColor(LIGHT_TEXT);
            g.drawString("Lumières " + obtained.size() + " / " + world.lightCount()
                    + (obtained.isEmpty() ? "" : " : " + String.join(", ", obtained)), 12, line);
            line += 20;
        }
        g.setColor(GAUGE_DANGER);
        g.drawString("Touché " + progress.hits() + " fois" + (world.isInvincible() ? "    Mode invincible" : ""), 12, line);
        if (world.progress().hasAbility(Ability.DASH)) {
            line += 20;
            drawDashGauge(g, line);
        }
        if (world.progress().hasAbility(Ability.SLIDE)) {
            line += 20;
            drawSlideState(g, line);
        }
        if (world.progress().hasAbility(Ability.SHOOT)) {
            drawShotGauge(g, line + 20);
        }
        if (message != null && messageTime > 0 && page == Page.NONE) {
            Popup.toast(g, message, MESSAGE_FONT, LIGHT_TEXT, getWidth(), 92);
        }

        g.setFont(getFont());
        g.setColor(Color.LIGHT_GRAY);
        g.drawString(controlsHint(), 12, bottom - 56);
        String stats = String.format("Vitesse %.0f px/s    Weber %.1f    Eau %.0f %%    Gouttes %d",
                player.speed(), player.weber(), world.playerWaterShare() * 100, world.droplets().size());
        if (showGrid) {
            stats += "    Bulle " + planPoint(plan, player.x(), player.y());
        }
        g.drawString(stats, 12, bottom - 36);

        double distortion = Math.min(player.distortion(), 1);
        g.drawString("Déformation", 12, bottom - 14);
        g.drawRect(92, bottom - 24, 150, 12);
        g.setColor(distortion < 0.7 ? GAUGE_OK : GAUGE_DANGER);
        g.fillRect(93, bottom - 23, (int) (149 * distortion), 11);
    }

    private String controlsHint() {
        boolean dash = world.progress().hasAbility(Ability.DASH);
        boolean severalDrops = world.droplets().size() > 1;
        boolean shoot = world.progress().hasAbility(Ability.SHOOT);
        boolean slide = world.progress().hasAbility(Ability.SLIDE);
        Gamepad gamepad = game.gamepad();
        if (gamepad.isConnected()) {
            return "Manette " + gamepad.name() + " : stick ou flèches : bouger    "
                    + (dash ? padName(Controls.Action.DASH) + " : dash    " : "")
                    + (slide ? padName(Controls.Action.SLIDE) + " : slide    " : "")
                    + (severalDrops ? padName(Controls.Action.SWAP) + " : changer de goutte    " : "")
                    + (shoot ? padName(Controls.Action.SHOOT) + " : tirer    " : "")
                    + padName(Controls.Action.RESTART) + " : recommencer    "
                    + padName(Controls.Action.GRID) + " : repère    "
                    + gamepad.buttonName(GamepadButton.START) + " : pause";
        }
        Controls controls = controls();
        return controls.describeMovement() + " : bouger    "
                + (dash ? controls.describe(Controls.Action.DASH) + " : dash    " : "")
                + (slide && hasKey(Controls.Action.SLIDE)
                        ? controls.describe(Controls.Action.SLIDE) + " : slide    " : "")
                + (severalDrops ? controls.describe(Controls.Action.SWAP) + " : changer de goutte    " : "")
                + (shoot && hasKey(Controls.Action.SHOOT) ? controls.describe(Controls.Action.SHOOT) + " : tirer    "
                        + aimKeys() + " : viser    " : "")
                + controls.describe(Controls.Action.RESTART) + " : recommencer    "
                + controls.describe(Controls.Action.GRID) + " : repère    Échap : pause";
    }

    private String padName(Controls.Action action) {
        GamepadButton button = controls().button(action);
        return button == null ? "—" : game.gamepad().buttonName(button);
    }

    private String describe(Controls.Action action) {
        return game.gamepad().isConnected() || !hasKey(action) ? padName(action) : controls().describe(action);
    }

    private void drawDashGauge(Graphics2D g, int y) {
        double tension = world.dashTension();
        boolean anchored = world.player().isAnchored();
        String label = "Dash (" + describe(Controls.Action.DASH) + ")";
        g.setColor(tension >= TEAR_WARNING ? GAUGE_DANGER : anchored ? LIGHT_TEXT : DASH_WAITING);
        g.drawString(label, 12, y);
        int left = 12 + g.getFontMetrics().stringWidth(label) + 8;
        g.drawRect(left, y - 10, 80, 10);
        g.fillRect(left + 1, y - 9, (int) (79 * tension), 9);
        String state = tension >= TEAR_WARNING ? "va casser !" : anchored ? "tendu : relâche pour partir"
                : "maintiens pour figer l'arrière";
        g.drawString(state, left + 90, y);
    }

    private void drawSlideState(Graphics2D g, int y) {
        boolean sliding = world.isSliding();
        String state = sliding ? "vitesse gardée : appuie pour reprendre la main"
                : world.player().speed() >= Slide.MIN_SPEED ? "appuie pour garder ta vitesse"
                : "prends de la vitesse d'abord";
        g.setColor(sliding ? LIGHT_TEXT : DASH_WAITING);
        g.drawString(Ability.SLIDE.label() + " (" + describe(Controls.Action.SLIDE) + ")    " + state, 12, y);
    }

    private void drawShotGauge(Graphics2D g, int y) {
        double power = world.shotPower();
        boolean charging = world.isChargingShot();
        String label = "Tir (" + describe(Controls.Action.SHOOT) + ")";
        g.setColor(power >= Shoot.DANGER_POWER ? GAUGE_DANGER : charging ? LIGHT_TEXT : DASH_WAITING);
        g.drawString(label, 12, y);
        int left = 12 + g.getFontMetrics().stringWidth(label) + 8;
        g.drawRect(left, y - 10, 80, 10);
        g.fillRect(left + 1, y - 9, (int) (79 * power), 9);
        String state = power >= Shoot.DANGER_POWER ? "va exploser !" : charging ? "relâche pour tirer"
                : "maintiens pour charger";
        g.drawString(state, left + 90, y);
    }

    private static String planPoint(Plan plan, double worldX, double worldY) {
        return String.format(Locale.ROOT, "%s  x %.1f  y %.1f", plan.name(), plan.planX(worldX, worldY),
                plan.planY(worldX, worldY));
    }

    private void drawPointer(Graphics2D g, Rectangle view, Plan plan) {
        if (mouseX < 0 || page != Page.NONE) {
            return;
        }
        String text = planPoint(plan, view.x + mouseX, view.y + mouseY);
        g.setFont(POINTER_FONT);
        int width = g.getFontMetrics().stringWidth(text) + 12;
        int left = Math.min(mouseX + 16, getWidth() - width - 4);
        int top = Math.max(mouseY - 30, 4);
        g.setColor(POINTER_BACK);
        g.fillRoundRect(left, top, width, 22, 8, 8);
        g.setColor(POINTER_TEXT);
        g.drawString(text, left + 6, top + 16);
    }

    private void drawPage(Graphics2D g) {
        Popup.dim(g, getWidth(), getHeight());
        if (page == Page.CONTROLS) {
            Rectangle box = Popup.centered(ControlsEditor.WIDTH + 2 * CONTROLS_PADDING,
                    ControlsEditor.HEIGHT + Popup.HEADER_HEIGHT + CONTROLS_PADDING, getWidth(), getHeight());
            controlsEditor.paint(g, Popup.draw(g, box, CONTROLS_LABEL));
            return;
        }
        Rectangle content = Popup.draw(g, popupBounds(), pageTitle());
        g.setFont(POPUP_TEXT_FONT);
        List<String> text = pageText();
        for (int i = 0; i < text.size(); i++) {
            g.setColor(page == Page.ABILITY && i == 0 ? LIGHT_TEXT : Ui.TEXT);
            drawCentered(g, text.get(i), content.y + POPUP_PADDING + 16 + i * TEXT_LINE);
        }

        List<String> choices = choices();
        for (int i = 0; i < choices.size(); i++) {
            Rectangle box = buttonBounds(i);
            Shape shape = new RoundRectangle2D.Double(box.x, box.y, box.width, box.height, 8, 8);
            g.setColor(i == selected ? buttonColor(i).brighter() : buttonColor(i));
            g.fill(shape);
            if (i == selected) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2f));
                g.draw(shape);
            }
            g.setColor(Color.WHITE);
            g.setFont(BUTTON_FONT);
            if (isVolume(i)) {
                Slider.draw(g, box, VOLUME_LABEL, game.volume());
                continue;
            }
            int textWidth = g.getFontMetrics().stringWidth(choices.get(i));
            g.drawString(choices.get(i), box.x + (box.width - textWidth) / 2, box.y + box.height / 2 + 6);
        }

        g.setColor(Ui.MUTED_TEXT);
        g.setFont(HINT_FONT);
        Rectangle last = buttonBounds(choices.size() - 1);
        drawCentered(g, pageHint(), last.y + last.height + POPUP_HINT_SPACE - 8);
    }

    private Color buttonColor(int index) {
        boolean current = page == Page.WINDOW_SIZES && index < sizes.size() && sizes.get(index) == game.windowSize()
                || page == Page.SETTINGS && index == INVINCIBLE_ROW && game.isInvincible();
        return current ? CURRENT_COLOR : BUTTON_COLOR;
    }

    private boolean isSettings() {
        return page == Page.SETTINGS || page == Page.WINDOW_SIZES;
    }

    private int buttonWidth() {
        return isSettings() ? SETTINGS_BUTTON_WIDTH : BUTTON_WIDTH;
    }

    private String pageTitle() {
        return switch (page) {
            case WIN -> "Niveau réussi !";
            case ABILITY -> "Nouvelle compétence : " + learnt.label();
            case SETTINGS -> SETTINGS_LABEL;
            case WINDOW_SIZES -> WINDOW_SIZE_LABEL;
            default -> "Pause";
        };
    }

    private List<String> pageText() {
        return switch (page) {
            case WIN -> List.of("Tu as terminé « " + world.level().name() + " »");
            case ABILITY -> abilityText();
            default -> List.of();
        };
    }

    private List<String> abilityText() {
        List<String> lines = new ArrayList<>();
        lines.add("Tu as rapporté la lumière de " + learnt.light() + " : tu sais maintenant faire le "
                + learnt.label() + " !");
        lines.add("");
        lines.addAll(learnt.description());
        lines.add("");
        if (learnt == Ability.SHOOT) {
            lines.add("Tirer : " + keysOf(Controls.Action.SHOOT) + "    Viser : " + aimKeys()
                    + (game.gamepad().isConnected() ? " ou stick droit" : "") + "    (modifiable dans les réglages)");
        } else if (learnt == Ability.SLIDE) {
            lines.add(learnt.label() + " : " + keysOf(Controls.Action.SLIDE) + "    (modifiable dans les réglages)");
        } else {
            lines.add("Dash : " + keysOf(Controls.Action.DASH) + "    Changer de goutte : "
                    + keysOf(Controls.Action.SWAP) + "    (modifiable dans les réglages)");
        }
        return lines;
    }

    private String aimKeys() {
        return controls().describe(Controls.Action.AIM_LEFT) + " " + controls().describe(Controls.Action.AIM_RIGHT);
    }

    private boolean hasKey(Controls.Action action) {
        return controls().key(action, 0) != Controls.NONE || controls().key(action, 1) != Controls.NONE;
    }

    private String keysOf(Controls.Action action) {
        GamepadButton button = controls().button(action);
        String pad = button == null ? null : game.gamepad().buttonName(button);
        if (!hasKey(action)) {
            return pad == null ? "à choisir" : pad + " à la manette";
        }
        String keys = controls().describe(action);
        return game.gamepad().isConnected() && pad != null ? keys + " ou " + pad : keys;
    }

    private String pageHint() {
        Gamepad gamepad = game.gamepad();
        if (gamepad.isConnected()) {
            String back = switch (page) {
                case WIN -> "menu    " + padName(Controls.Action.RESTART) + " : rejouer";
                case ABILITY -> "retourner au hub";
                case SETTINGS, WINDOW_SIZES -> "retour";
                default -> "reprendre";
            };
            return "Stick ou flèches : choisir    " + (page == Page.SETTINGS ? "← → : régler le volume    " : "")
                    + gamepad.buttonName(GamepadButton.A) + " : valider    "
                    + gamepad.buttonName(GamepadButton.B) + " : " + back;
        }
        return switch (page) {
            case WIN -> "Entrée : valider    " + controls().describe(Controls.Action.RESTART)
                    + " : rejouer    Échap : menu";
            case ABILITY -> "Flèches ou ZQSD : choisir    Entrée : valider    Échap : retourner au hub";
            case SETTINGS ->
                    "Flèches ou ZQSD : choisir    ← → : régler le volume    Entrée : valider    Échap : retour";
            case WINDOW_SIZES -> "Flèches ou ZQSD : choisir    Entrée : valider    Échap : retour";
            default -> "Flèches ou ZQSD : choisir    Entrée : valider    Échap : reprendre";
        };
    }

    private int textHeight() {
        int lines = pageText().size();
        return lines == 0 ? 0 : lines * TEXT_LINE + POPUP_PADDING / 2;
    }

    private Rectangle popupBounds() {
        int count = choices().size();
        int height = Popup.HEADER_HEIGHT + POPUP_PADDING + textHeight() + count * (BUTTON_HEIGHT + BUTTON_GAP)
                - BUTTON_GAP + POPUP_PADDING + POPUP_HINT_SPACE;
        FontMetrics metrics = getFontMetrics(POPUP_TEXT_FONT);
        int widest = buttonWidth();
        for (String line : pageText()) {
            widest = Math.max(widest, metrics.stringWidth(line));
        }
        return Popup.centered(widest + 2 * POPUP_SIDE, height, getWidth(), getHeight());
    }

    private Rectangle buttonBounds(int index) {
        Rectangle popup = popupBounds();
        int top = popup.y + Popup.HEADER_HEIGHT + POPUP_PADDING + textHeight();
        return new Rectangle(popup.x + (popup.width - buttonWidth()) / 2, top + index * (BUTTON_HEIGHT + BUTTON_GAP),
                buttonWidth(), BUTTON_HEIGHT);
    }

    private int buttonAt(int x, int y) {
        if (page == Page.NONE || page == Page.CONTROLS) {
            return -1;
        }
        for (int i = 0; i < choices().size(); i++) {
            if (buttonBounds(i).contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    private void drawGear(Graphics2D g) {
        Rectangle box = gearBounds();
        if (gear == null || !box.equals(gearBox)) {
            gearBox = box;
            gear = gearShape(box);
        }
        g.setColor(gearHovered ? FluidRenderer.NEON : GEAR_COLOR);
        g.fill(gear);
    }

    private static Shape gearShape(Rectangle box) {
        double cx = box.getCenterX();
        double cy = box.getCenterY();
        Area gear = new Area(new Ellipse2D.Double(cx - 11, cy - 11, 22, 22));
        Shape tooth = new RoundRectangle2D.Double(cx - 3.5, cy - 16, 7, 9, 2, 2);
        for (int i = 0; i < 8; i++) {
            gear.add(new Area(AffineTransform.getRotateInstance(i * Math.PI / 4, cx, cy).createTransformedShape(tooth)));
        }
        gear.subtract(new Area(new Ellipse2D.Double(cx - 5, cy - 5, 10, 10)));
        return gear;
    }

    private Rectangle gearBounds() {
        return new Rectangle(getWidth() - 46, 8, 36, 36);
    }

    private void drawCentered(Graphics2D g, String text, int y) {
        g.drawString(text, (getWidth() - g.getFontMetrics().stringWidth(text)) / 2, y);
    }
}
