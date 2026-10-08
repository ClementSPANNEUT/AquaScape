package screen;

import input.GamepadButton;
import input.GamepadListener;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * The controls screen opened from the settings of the start menu: a {@link ControlsEditor} in a pop-up. Leaving
 * it goes back to the settings.
 *
 * @serial exclude
 */
public class ControlsPanel extends JPanel implements GamepadListener {
    private static final int PADDING = 28;

    private final ControlsEditor editor;

    /**
     * Creates the controls screen.
     * @param game the game, to switch screens and reach the save
     */
    public ControlsPanel(Game game) {
        editor = new ControlsEditor(game.controls(), game.gamepad(), game::saveControls, game::showSettings);
        setBackground(Ui.BACKGROUND);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                e.consume();
                editor.keyPressed(e.getKeyCode());
                repaint();
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                editor.mouseMoved(e.getX(), e.getY());
                repaint();
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                editor.mousePressed(e.getX(), e.getY());
                repaint();
            }
        });
    }

    /** Opens the screen with the first action selected. */
    public void open() {
        editor.open();
        repaint();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    /**
     * Passes a controller button to the table, which can then bind it.
     * @param button the button pressed
     */
    @Override
    public void pressed(GamepadButton button) {
        editor.gamepadPressed(button);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Ui.paintBackground(g2, getWidth(), getHeight());
        Rectangle box = Popup.centered(ControlsEditor.WIDTH + 2 * PADDING, ControlsEditor.HEIGHT + Popup.HEADER_HEIGHT + PADDING,
                getWidth(), getHeight());
        editor.paint(g2, Popup.draw(g2, box, "Contrôles"));
    }
}
