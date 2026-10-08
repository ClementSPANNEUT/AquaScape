package screen;

import fluid.Droplet;
import fluid.FluidRenderer;
import input.GamepadButton;
import input.GamepadListener;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import save.WindowSize;

/**
 * Start menu and settings: the content is rebuilt for each page. The arrows or ZQSD move through the buttons,
 * Entrée validates and Échap goes back; on a controller, the stick or the arrows, A, and B or Start. The settings
 * gather the controls, the window size, the volume and the invincible mode; on the volume, left and right change
 * its level instead of going back or validating.
 *
 * @serial exclude
 */
public class MenuPanel extends JPanel implements GamepadListener {
    private static final Font HINT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final String HINT = "Flèches ou ZQSD : choisir    Entrée : valider    Échap : retour";
    private static final String SETTINGS_HINT =
            "Flèches ou ZQSD : choisir    ← → : régler le volume    Entrée : valider    Échap : retour";
    private static final int WINDOW_SIZE_ROW = 1;
    private static final int INVINCIBLE_ROW = 3;

    private final Game game;
    private final JPanel content = new JPanel();
    private final List<JButton> choices = new ArrayList<>();
    private final FluidRenderer renderer = new FluidRenderer();
    private int selected;
    private Runnable onEscape = () -> {
    };

    /**
     * Creates the menu.
     * @param game the game, to switch screens and reach the save
     */
    public MenuPanel(Game game) {
        super(new GridBagLayout()); // keeps the content centered
        this.game = game;
        setBackground(Ui.BACKGROUND);
        setFocusable(true);
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        add(content);

        bind(() -> select(selected - 1), KeyEvent.VK_UP, KeyEvent.VK_Z, KeyEvent.VK_W);
        bind(() -> select(selected + 1), KeyEvent.VK_DOWN, KeyEvent.VK_S);
        bind(this::activate, KeyEvent.VK_ENTER, KeyEvent.VK_SPACE);
        bind(() -> onEscape.run(), KeyEvent.VK_ESCAPE);
        bind(() -> sideways(1), KeyEvent.VK_RIGHT, KeyEvent.VK_D);
        bind(() -> sideways(-1), KeyEvent.VK_LEFT, KeyEvent.VK_Q, KeyEvent.VK_A);
    }

    /** Shows the start menu. */
    public void showMain() {
        showMain(false);
    }

    private void showMain(boolean onSettings) {
        begin(() -> {
        });
        content.add(Ui.label("Aquascape", Ui.TITLE_FONT, Ui.TEXT));
        content.add(Ui.label("Une bulle d'eau et la mécanique des fluides", Ui.TEXT_FONT, Ui.MUTED_TEXT));
        gap(40);
        JButton playButton = addChoice(Ui.button(game.hasStarted() ? "Continuer" : "Jouer", game::continueGame));
        gap(12);
        addChoice(Ui.button("Carte des checkpoints", game::showMap));
        gap(12);
        addChoice(Ui.button("Salle de test", game::playTest));
        gap(12);
        JButton settings = addChoice(Ui.button("Réglages", this::showSettings));
        gap(12);
        addChoice(Ui.button("Quitter", game::quit));
        end(onSettings ? settings : playButton, HINT);
    }

    /** Shows the settings: the controls, the window size, the volume and the invincible mode. */
    public void showSettings() {
        showSettings(0);
    }

    private void showSettings(int row) {
        begin(() -> showMain(true));
        content.add(Ui.label("Réglages", Ui.SUBTITLE_FONT, Ui.TEXT));
        gap(24);
        addChoice(Ui.button("Contrôles", game::showControls));
        gap(12);
        addChoice(Ui.button("Taille de l'écran : " + game.windowSize().label(), this::showWindowSizes));
        gap(12);
        addChoice(Ui.slider("Volume", game.volume(), game::setVolume));
        gap(12);
        Runnable toggle = () -> {
            game.setInvincible(!game.isInvincible());
            showSettings(INVINCIBLE_ROW);
        };
        addChoice(game.isInvincible() ? Ui.selectedButton("Mode invincible : activé", toggle)
                : Ui.button("Mode invincible : désactivé", toggle));
        gap(12);
        addChoice(Ui.button("Retour", () -> showMain(true)));
        end(choices.get(row), SETTINGS_HINT);
    }

    private void showWindowSizes() {
        begin(() -> showSettings(WINDOW_SIZE_ROW));
        content.add(Ui.label("Taille de l'écran", Ui.SUBTITLE_FONT, Ui.TEXT));
        gap(20);
        JButton current = null;
        for (WindowSize size : WindowSize.available()) {
            Runnable choose = () -> {
                game.setWindowSize(size);
                showWindowSizes();
            };
            if (size == game.windowSize()) {
                current = addChoice(Ui.selectedButton(size.label(), choose));
            } else {
                addChoice(Ui.button(size.label(), choose));
            }
            gap(6);
        }
        gap(10);
        addChoice(Ui.button("Retour", () -> showSettings(WINDOW_SIZE_ROW)));
        end(current, HINT);
    }

    private void begin(Runnable escape) {
        content.removeAll();
        choices.clear();
        selected = 0;
        onEscape = escape;
    }

    private void end(JButton preferred, String hint) {
        gap(24);
        content.add(Ui.label(hint, HINT_FONT, Ui.MUTED_TEXT));
        content.revalidate();
        repaint();
        select(Math.max(0, choices.indexOf(preferred)));
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void gap(int height) {
        content.add(Box.createVerticalStrut(height));
    }

    private JButton addChoice(JButton button) {
        content.add(button);
        int index = choices.size();
        choices.add(button);
        button.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (selected != index) {
                    select(index);
                }
            }
        });
        return button;
    }

    private void select(int index) {
        if (choices.isEmpty()) {
            return;
        }
        Ui.highlight(choices.get(selected), false);
        selected = Math.floorMod(index, choices.size());
        Ui.highlight(choices.get(selected), true);
    }

    private void activate() {
        if (!choices.isEmpty()) {
            choices.get(selected).doClick(0);
        }
    }

    private boolean turn(int step) {
        if (choices.isEmpty() || !(choices.get(selected) instanceof Slider slider)) {
            return false;
        }
        slider.move(step);
        return true;
    }

    private void sideways(int step) {
        if (turn(step)) {
            return;
        }
        if (step > 0) {
            activate();
        } else {
            onEscape.run();
        }
    }

    /**
     * Moves through the buttons with the stick or the arrows of a controller; sideways pushes only change the
     * level of a slider, and do nothing on the other buttons.
     * @param button the button pressed
     */
    @Override
    public void pressed(GamepadButton button) {
        switch (button) {
            case UP -> select(selected - 1);
            case DOWN -> select(selected + 1);
            case LEFT -> turn(-1);
            case RIGHT -> turn(1);
            case A -> activate();
            case B, START -> onEscape.run();
            default -> {
            }
        }
    }

    private void bind(Runnable action, int... keys) {
        String name = "menu-" + keys[0];
        for (int key : keys) {
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), name);
        }
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Ui.paintBackground(g2, getWidth(), getHeight());
        renderer.draw(g2, List.of(
                new Droplet(getWidth() * 0.12, getHeight() * 0.22, 42),
                new Droplet(getWidth() * 0.88, getHeight() * 0.72, 60),
                new Droplet(getWidth() * 0.82, getHeight() * 0.2, 16),
                new Droplet(getWidth() * 0.18, getHeight() * 0.8, 24)), 0, 0, getWidth(), getHeight());
    }
}
