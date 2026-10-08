package screen;

import input.Gamepad;
import input.GamepadButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import save.Controls;

/**
 * The table of key bindings, used by the controls screen and in game: choose an action and a column, then press
 * the new key, or the new controller button in the « Manette » column.
 * <p>It only draws and handles input; the screen showing it passes it the keys, the controller buttons and the
 * mouse.
 */
public class ControlsEditor {
    private static final Controls.Action[] ACTIONS = Controls.Action.values();
    private static final int BUTTON_ROW = ACTIONS.length;
    private static final int PAD_COLUMN = Controls.SLOTS;
    private static final int COLUMNS = Controls.SLOTS + 1;
    private static final int BUTTONS = 2;
    private static final int ROW_HEIGHT = 28;
    private static final int LABEL_WIDTH = 150;
    private static final int CELL_WIDTH = 150;
    private static final int CELL_HEIGHT = 24;
    private static final int GAP = 14;
    private static final int TITLE_SPACE = 34;
    private static final int FOOTER_SPACE = 88;
    /** Width of the table, in px. */
    public static final int WIDTH = LABEL_WIDTH + COLUMNS * (CELL_WIDTH + GAP);
    /** Height of the table, in px. */
    public static final int HEIGHT = TITLE_SPACE + (BUTTON_ROW + 1) * ROW_HEIGHT + GAP + FOOTER_SPACE;
    private static final Font HEADER_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 17);
    private static final Font CELL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    private static final Font HINT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final Color TEXT = new Color(225, 230, 245);
    private static final Color MUTED = new Color(140, 145, 165);
    private static final Color CELL = new Color(40, 90, 160);
    private static final Color EMPTY_CELL = new Color(45, 50, 70);
    private static final Color FIXED_CELL = new Color(55, 65, 90);
    private static final Color CAPTURE = new Color(35, 140, 100);
    private static final Color MESSAGE = new Color(255, 205, 95);
    private static final BasicStroke SELECTED_STROKE = new BasicStroke(2f);

    private final Controls controls;
    private final Gamepad gamepad;
    private final Runnable onChange;
    private final Runnable onClose;
    private int row;
    private int column;
    private boolean capturing;
    private String message = "";
    private int left;
    private int top;

    /**
     * Creates the table.
     * @param controls the bindings to edit
     * @param gamepad the controller, to name its buttons
     * @param onChange called after each change, to save it
     * @param onClose called when the player leaves the table
     */
    public ControlsEditor(Controls controls, Gamepad gamepad, Runnable onChange, Runnable onClose) {
        this.controls = controls;
        this.gamepad = gamepad;
        this.onChange = onChange;
        this.onClose = onClose;
    }

    /** Resets the selection, before the table shows. */
    public void open() {
        row = 0;
        column = 0;
        capturing = false;
        message = "";
    }

    /**
     * Handles a key: moves the selection, or binds the key when one is awaited. A key pressed while a controller
     * button is awaited cancels it.
     * @param key key code
     */
    public void keyPressed(int key) {
        if (capturing) {
            capturing = false;
            message = "";
            if (column == PAD_COLUMN || !Controls.canBind(key)) {
                return;
            }
            Controls.Action action = ACTIONS[row];
            Controls.Action previous = controls.set(action, column, key);
            message = previous == null || previous == action ? ""
                    : Controls.keyName(key) + " servait à « " + previous.label() + " » : elle y a été retirée.";
            onChange.run();
            return;
        }
        switch (key) {
            case KeyEvent.VK_UP, KeyEvent.VK_Z, KeyEvent.VK_W -> moveRow(-1);
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> moveRow(1);
            case KeyEvent.VK_LEFT, KeyEvent.VK_Q, KeyEvent.VK_A -> moveColumn(-1);
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> moveColumn(1);
            case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> activate();
            case KeyEvent.VK_DELETE, KeyEvent.VK_BACK_SPACE -> clear();
            case KeyEvent.VK_ESCAPE -> onClose.run();
            default -> {
            }
        }
    }

    /**
     * Handles a controller button: the stick or the arrows move the selection, A changes the cell, X empties it and
     * B or Start leave. When a controller button is awaited, the button pressed is bound, and Start cancels; when a
     * key is awaited, any button cancels.
     * @param button the button pressed
     */
    public void gamepadPressed(GamepadButton button) {
        if (capturing) {
            if (button.isDirection()) {
                return;
            }
            capturing = false;
            message = "";
            if (column != PAD_COLUMN || !Controls.canBind(button)) {
                return;
            }
            Controls.Action previous = controls.setButton(ACTIONS[row], button);
            message = previous == null ? ""
                    : "Le bouton " + gamepad.buttonName(button) + " servait à « " + previous.label()
                    + " » : il y a été retiré.";
            onChange.run();
            return;
        }
        switch (button) {
            case UP -> moveRow(-1);
            case DOWN -> moveRow(1);
            case LEFT -> moveColumn(-1);
            case RIGHT -> moveColumn(1);
            case A -> activate();
            case X -> clear();
            case B, START -> onClose.run();
            default -> {
            }
        }
    }

    /**
     * Selects the cell under the mouse.
     * @param x x of the mouse, in px
     * @param y y of the mouse, in px
     */
    public void mouseMoved(int x, int y) {
        if (capturing) {
            return;
        }
        int[] hit = hit(x, y);
        if (hit != null) {
            row = hit[0];
            column = hit[1];
        }
    }

    /**
     * Selects and activates the cell under the mouse.
     * @param x x of the mouse, in px
     * @param y y of the mouse, in px
     */
    public void mousePressed(int x, int y) {
        int[] hit = hit(x, y);
        if (capturing) {
            capturing = false;
            return;
        }
        if (hit != null) {
            row = hit[0];
            column = hit[1];
            activate();
        }
    }

    private void moveRow(int step) {
        row = Math.floorMod(row + step, BUTTON_ROW + 1);
        column = Math.min(column, columns(row) - 1);
    }

    private void moveColumn(int step) {
        column = Math.floorMod(column + step, columns(row));
    }

    private static int columns(int r) {
        return r == BUTTON_ROW ? BUTTONS : COLUMNS;
    }

    private boolean isFixed(int r, int c) {
        return r < BUTTON_ROW && c == PAD_COLUMN && ACTIONS[r].usesStick();
    }

    private void activate() {
        message = "";
        if (isFixed(row, column)) {
            message = ACTIONS[row].isAim() ? "À la manette, c'est toujours le stick droit qui vise."
                    : "Le stick et les flèches de la manette servent toujours à bouger.";
        } else if (row < BUTTON_ROW) {
            capturing = true;
        } else if (column == 0) {
            controls.reset();
            message = "Touches et boutons par défaut rétablis.";
            onChange.run();
        } else {
            onClose.run();
        }
    }

    private void clear() {
        if (row >= BUTTON_ROW || isFixed(row, column)) {
            return;
        }
        if (column == PAD_COLUMN) {
            controls.clearButton(ACTIONS[row]);
        } else {
            controls.clear(ACTIONS[row], column);
        }
        message = "";
        onChange.run();
    }

    private int[] hit(int x, int y) {
        for (int r = 0; r <= BUTTON_ROW; r++) {
            for (int c = 0; c < columns(r); c++) {
                if (cell(r, c).contains(x, y)) {
                    return new int[] {r, c};
                }
            }
        }
        return null;
    }

    private Rectangle cell(int r, int c) {
        int y = top + TITLE_SPACE + r * ROW_HEIGHT + (ROW_HEIGHT - CELL_HEIGHT) / 2;
        if (r == BUTTON_ROW) {
            int buttonsWidth = BUTTONS * CELL_WIDTH + (BUTTONS - 1) * GAP;
            int x = left + (WIDTH - buttonsWidth) / 2 + c * (CELL_WIDTH + GAP);
            return new Rectangle(x, y + GAP, CELL_WIDTH, CELL_HEIGHT);
        }
        int x = left + LABEL_WIDTH + GAP + c * (CELL_WIDTH + GAP);
        return new Rectangle(x, y, CELL_WIDTH, CELL_HEIGHT);
    }

    /**
     * Draws the table in an area.
     * @param g graphics drawing on the screen
     * @param area area to draw in, usually inside a pop-up
     */
    public void paint(Graphics2D g, Rectangle area) {
        left = area.x + (area.width - WIDTH) / 2;
        top = area.y + Math.max(0, (area.height - HEIGHT) / 2);
        int center = area.x + area.width / 2;

        g.setColor(MUTED);
        g.setFont(HEADER_FONT);
        for (int c = 0; c < COLUMNS; c++) {
            Rectangle first = cell(0, c);
            String header = c == PAD_COLUMN ? "Manette" : "Touche " + (c + 1);
            int textWidth = g.getFontMetrics().stringWidth(header);
            g.drawString(header, first.x + (first.width - textWidth) / 2, top + TITLE_SPACE - 6);
        }

        for (int r = 0; r < BUTTON_ROW; r++) {
            Rectangle first = cell(r, 0);
            g.setColor(TEXT);
            g.setFont(LABEL_FONT);
            g.drawString(ACTIONS[r].label(), left, first.y + first.height / 2 + 6);
            for (int c = 0; c < COLUMNS; c++) {
                boolean selected = r == row && c == column;
                drawCell(g, cell(r, c), cellColor(r, c, selected), cellText(r, c, selected), selected);
            }
        }
        drawCell(g, cell(BUTTON_ROW, 0), CELL, "Par défaut", row == BUTTON_ROW && column == 0);
        drawCell(g, cell(BUTTON_ROW, 1), CELL, "Retour", row == BUTTON_ROW && column == 1);

        int footer = cell(BUTTON_ROW, 0).y + CELL_HEIGHT + 28;
        g.setFont(HINT_FONT);
        if (!message.isEmpty()) {
            g.setColor(MESSAGE);
            centered(g, message, center, footer);
        }
        g.setColor(MUTED);
        if (capturing && column == PAD_COLUMN) {
            centered(g, "Appuie sur le bouton de la manette pour « " + ACTIONS[row].label() + " »    "
                    + gamepad.buttonName(GamepadButton.START) + " ou Échap : annuler", center, footer + 22);
        } else if (capturing) {
            centered(g, "Appuie sur la nouvelle touche pour « " + ACTIONS[row].label() + " »    Échap : annuler",
                    center, footer + 22);
        } else {
            centered(g, "Flèches ou ZQSD : choisir    Entrée ou clic : changer    Suppr : effacer    Échap : retour",
                    center, footer + 22);
            centered(g, "Manette : stick ou flèches : choisir    " + gamepad.buttonName(GamepadButton.A)
                    + " : changer    " + gamepad.buttonName(GamepadButton.X) + " : effacer    "
                    + gamepad.buttonName(GamepadButton.B) + " : retour", center, footer + 44);
        }
    }

    private Color cellColor(int r, int c, boolean selected) {
        if (selected && capturing) {
            return CAPTURE;
        }
        if (isFixed(r, c)) {
            return FIXED_CELL;
        }
        boolean empty = c == PAD_COLUMN ? controls.button(ACTIONS[r]) == null
                : controls.key(ACTIONS[r], c) == Controls.NONE;
        return empty ? EMPTY_CELL : CELL;
    }

    private String cellText(int r, int c, boolean selected) {
        if (selected && capturing) {
            return c == PAD_COLUMN ? "Appuie sur un bouton…" : "Appuie sur une touche…";
        }
        if (c != PAD_COLUMN) {
            return Controls.keyName(controls.key(ACTIONS[r], c));
        }
        if (isFixed(r, c)) {
            return ACTIONS[r].stickLabel();
        }
        GamepadButton button = controls.button(ACTIONS[r]);
        return button == null ? "—" : gamepad.buttonName(button);
    }

    private static void drawCell(Graphics2D g, Rectangle box, Color color, String text, boolean selected) {
        RoundRectangle2D shape = new RoundRectangle2D.Double(box.x, box.y, box.width, box.height, 8, 8);
        g.setColor(selected ? color.brighter() : color);
        g.fill(shape);
        if (selected) {
            g.setColor(TEXT);
            g.setStroke(SELECTED_STROKE);
            g.draw(shape);
        }
        g.setColor(TEXT);
        g.setFont(CELL_FONT);
        int textWidth = g.getFontMetrics().stringWidth(text);
        if (textWidth > box.width - 12) {
            g.setFont(HINT_FONT);
            textWidth = g.getFontMetrics().stringWidth(text);
        }
        g.drawString(text, box.x + (box.width - textWidth) / 2, box.y + box.height / 2 + 6);
    }

    private static void centered(Graphics2D g, String text, int center, int y) {
        g.drawString(text, center - g.getFontMetrics().stringWidth(text) / 2, y);
    }
}
