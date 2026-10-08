package screen;

import input.GamepadButton;
import input.GamepadListener;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import world.Checkpoint;
import world.Light;
import world.Progress;
import world.World;
import zone.Levels;

/**
 * The checkpoint map: the whole world in small, with its checkpoints and lights. The player picks a validated
 * checkpoint with the arrows, the mouse or the stick of a controller, and starts from it.
 *
 * @serial exclude
 */
public class MapPanel extends JPanel implements GamepadListener {
    private static final int SIDE = 40;
    private static final int TOP = 100;
    private static final int BOTTOM = 90;
    private static final double PICK_RADIUS = 18;
    private static final double CHECKPOINT_SIZE = 7;
    private static final double LIGHT_SIZE = 9;
    private static final double RING = 14;
    private static final Font HINT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final Font NAME_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    private static final Font BACK_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    private static final Color INSIDE = new Color(14, 24, 42);
    private static final Color EDGE = new Color(110, 230, 250, 190);
    private static final Color VALIDATED = new Color(120, 240, 255);
    private static final Color VALIDATED_GLOW = new Color(120, 240, 255, 60);
    private static final Color LOCKED = new Color(95, 105, 130);
    private static final Color GOLD = new Color(255, 205, 95);
    private static final Color BACK_COLOR = new Color(40, 90, 160);
    private static final BasicStroke EDGE_STROKE = new BasicStroke(1.2f);
    private static final BasicStroke OUTLINE_STROKE = new BasicStroke(1.5f);
    private static final BasicStroke RING_STROKE = new BasicStroke(2f);

    private final Game game;
    private List<Checkpoint> checkpoints = List.of();
    private List<Light> lights = List.of();
    private Progress progress = new Progress();
    private Path2D outline;
    private Path2D edges;
    private Rectangle2D bounds;
    private BufferedImage mapImage;
    private int selected;
    private boolean backHovered;
    private double scale;
    private double offsetX;
    private double offsetY;

    /**
     * Creates the map screen.
     * @param game the game, to switch screens and reach the save
     */
    public MapPanel(Game game) {
        this.game = game;
        setBackground(Ui.BACKGROUND);
        setFocusable(true);

        bind(() -> step(0, -1), KeyEvent.VK_UP, KeyEvent.VK_Z, KeyEvent.VK_W);
        bind(() -> step(0, 1), KeyEvent.VK_DOWN, KeyEvent.VK_S);
        bind(() -> step(-1, 0), KeyEvent.VK_LEFT, KeyEvent.VK_Q, KeyEvent.VK_A);
        bind(() -> step(1, 0), KeyEvent.VK_RIGHT, KeyEvent.VK_D);
        bind(() -> game.play(selected), KeyEvent.VK_ENTER, KeyEvent.VK_SPACE);
        bind(game::showMenu, KeyEvent.VK_ESCAPE, KeyEvent.VK_BACK_SPACE);

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                backHovered = backBounds().contains(e.getPoint());
                int under = checkpointAt(e.getX(), e.getY());
                if (under >= 0) {
                    selected = under;
                }
                repaint();
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (backBounds().contains(e.getPoint())) {
                    game.showMenu();
                    return;
                }
                int under = checkpointAt(e.getX(), e.getY());
                if (under >= 0) {
                    game.play(under);
                }
            }
        });
    }

    /** Refreshes the map from the save and selects the last checkpoint touched. */
    public void open() {
        progress = game.savedProgress(0);
        World world = new World(Levels.get(Levels.WORLD), game.savedProgress(0));
        checkpoints = world.checkpoints();
        lights = world.lights();
        if (outline == null) {
            outline = world.tunnel().contour();
            edges = world.tunnel().edgeContour();
            bounds = outline.getBounds2D();
        }
        int last = game.lastCheckpoint();
        selected = last < checkpoints.size() && game.isValidated(last) ? last : 0;
        backHovered = false;
        repaint();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void step(int dx, int dy) {
        Checkpoint from = checkpoints.get(selected);
        int best = -1;
        double bestScore = Double.MAX_VALUE;
        for (int i = 0; i < checkpoints.size(); i++) {
            if (i == selected || !game.isValidated(i)) {
                continue;
            }
            double vx = checkpoints.get(i).x() - from.x();
            double vy = checkpoints.get(i).y() - from.y();
            double along = vx * dx + vy * dy;
            double across = Math.abs(vx * dy - vy * dx);
            if (along <= 0 || across > 2 * along) {
                continue;
            }
            double score = Math.hypot(vx, vy) + across;
            if (score < bestScore) {
                bestScore = score;
                best = i;
            }
        }
        if (best >= 0) {
            selected = best;
            repaint();
        }
    }

    /**
     * Moves between the checkpoints with the stick or the arrows of a controller; A starts from the chosen one and
     * B or Start go back to the menu.
     * @param button the button pressed
     */
    @Override
    public void pressed(GamepadButton button) {
        switch (button) {
            case UP -> step(0, -1);
            case DOWN -> step(0, 1);
            case LEFT -> step(-1, 0);
            case RIGHT -> step(1, 0);
            case A -> game.play(selected);
            case B, START -> game.showMenu();
            default -> {
            }
        }
    }

    private int checkpointAt(int mouseX, int mouseY) {
        int found = -1;
        double nearest = PICK_RADIUS;
        for (int i = 0; i < checkpoints.size(); i++) {
            if (!game.isValidated(i)) {
                continue;
            }
            double distance = Math.hypot(screenX(checkpoints.get(i).x()) - mouseX,
                    screenY(checkpoints.get(i).y()) - mouseY);
            if (distance < nearest) {
                nearest = distance;
                found = i;
            }
        }
        return found;
    }

    private void fit() {
        double width = getWidth() - 2.0 * SIDE;
        double height = getHeight() - TOP - BOTTOM;
        scale = Math.max(1e-6, Math.min(width / bounds.getWidth(), height / bounds.getHeight()));
        offsetX = SIDE + (width - bounds.getWidth() * scale) / 2 - bounds.getX() * scale;
        offsetY = TOP + (height - bounds.getHeight() * scale) / 2 - bounds.getY() * scale;
    }

    private double screenX(double worldX) {
        return offsetX + worldX * scale;
    }

    private double screenY(double worldY) {
        return offsetY + worldY * scale;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Ui.paintBackground(g2, getWidth(), getHeight());
        if (outline == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        fit();
        g2.drawImage(mapImage(), 0, 0, null);
        drawLights(g2);
        drawCheckpoints(g2);
        drawTexts(g2);
        drawBack(g2);
    }

    private BufferedImage mapImage() {
        if (mapImage == null || mapImage.getWidth() != getWidth() || mapImage.getHeight() != getHeight()) {
            mapImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = mapImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            AffineTransform toScreen = new AffineTransform(scale, 0, 0, scale, offsetX, offsetY);
            g.setColor(INSIDE);
            g.fill(toScreen.createTransformedShape(outline));
            g.setColor(EDGE);
            g.setStroke(EDGE_STROKE);
            g.draw(toScreen.createTransformedShape(edges));
            g.dispose();
        }
        return mapImage;
    }

    private void drawLights(Graphics2D g) {
        g.setFont(NAME_FONT);
        for (Light light : lights) {
            double x = screenX(light.x());
            double y = screenY(light.y());
            boolean obtained = progress.hasLight(light.name());
            Color color = obtained ? (light.isLast() ? GOLD : VALIDATED) : LOCKED;
            g.setColor(color);
            if (obtained) {
                g.fill(diamond(x, y, LIGHT_SIZE));
            } else {
                g.setStroke(OUTLINE_STROKE);
                g.draw(diamond(x, y, LIGHT_SIZE));
            }
            int width = g.getFontMetrics().stringWidth(light.name());
            float textX = (float) (x - width / 2.0);
            float textY = (float) (y + LIGHT_SIZE + 15);
            g.setColor(Ui.BACKGROUND);
            g.drawString(light.name(), textX + 1, textY + 1);
            g.setColor(color);
            g.drawString(light.name(), textX, textY);
        }
    }

    private void drawCheckpoints(Graphics2D g) {
        for (int i = 0; i < checkpoints.size(); i++) {
            double x = screenX(checkpoints.get(i).x());
            double y = screenY(checkpoints.get(i).y());
            if (game.isValidated(i)) {
                g.setColor(VALIDATED_GLOW);
                g.fill(new Ellipse2D.Double(x - CHECKPOINT_SIZE, y - CHECKPOINT_SIZE, 2 * CHECKPOINT_SIZE, 2 * CHECKPOINT_SIZE));
                g.setColor(VALIDATED);
                g.fill(diamond(x, y, CHECKPOINT_SIZE));
            } else {
                g.setColor(LOCKED);
                g.setStroke(OUTLINE_STROKE);
                g.draw(diamond(x, y, CHECKPOINT_SIZE * 0.75));
            }
        }
        Checkpoint chosen = checkpoints.get(selected);
        double x = screenX(chosen.x());
        double y = screenY(chosen.y());
        g.setColor(Color.WHITE);
        g.setStroke(RING_STROKE);
        g.draw(new Ellipse2D.Double(x - RING, y - RING, 2 * RING, 2 * RING));
        g.fill(diamond(x, y, CHECKPOINT_SIZE * 1.2));
    }

    private void drawTexts(Graphics2D g) {
        int validated = 0;
        for (int i = 0; i < checkpoints.size(); i++) {
            if (game.isValidated(i)) {
                validated++;
            }
        }
        int otherLights = 0;
        int obtainedLights = 0;
        for (Light light : lights) {
            if (!light.isLast()) {
                otherLights++;
                if (progress.hasLight(light.name())) {
                    obtainedLights++;
                }
            }
        }
        g.setColor(Ui.TEXT);
        g.setFont(Ui.SUBTITLE_FONT);
        drawCentered(g, "Carte des checkpoints", 48);
        g.setColor(Ui.MUTED_TEXT);
        g.setFont(Ui.TEXT_FONT);
        String summary = validated + " / " + checkpoints.size() + " checkpoints validés    Lumières "
                + obtainedLights + " / " + otherLights;
        if (game.isCompleted(Levels.WORLD)) {
            summary += "    Fin atteinte ✔";
        }
        drawCentered(g, summary, 76);

        g.setColor(VALIDATED);
        g.setFont(Ui.TEXT_FONT);
        drawCentered(g, describe(selected), getHeight() - 52);
        g.setColor(Ui.MUTED_TEXT);
        g.setFont(HINT_FONT);
        drawCentered(g, "Flèches ou ZQSD : choisir    Entrée ou clic : jouer depuis ce checkpoint    Échap : retour",
                getHeight() - 24);
    }

    private String describe(int index) {
        String area = checkpoints.get(index).area();
        int count = 0;
        int rank = 0;
        for (int i = 0; i < checkpoints.size(); i++) {
            if (checkpoints.get(i).area().equals(area)) {
                count++;
                if (i <= index) {
                    rank++;
                }
            }
        }
        return count > 1 ? area + " · checkpoint " + rank + " / " + count : area;
    }

    private void drawBack(Graphics2D g) {
        Rectangle box = backBounds();
        Shape shape = new RoundRectangle2D.Double(box.x, box.y, box.width, box.height, 8, 8);
        g.setColor(backHovered ? BACK_COLOR.brighter() : BACK_COLOR);
        g.fill(shape);
        g.setColor(Ui.TEXT);
        g.setFont(BACK_FONT);
        int width = g.getFontMetrics().stringWidth("Retour");
        g.drawString("Retour", box.x + (box.width - width) / 2, box.y + box.height / 2 + 5);
    }

    private Rectangle backBounds() {
        return new Rectangle(16, 16, 110, 36);
    }

    private void drawCentered(Graphics2D g, String text, int y) {
        g.drawString(text, (getWidth() - g.getFontMetrics().stringWidth(text)) / 2, y);
    }

    private static Path2D diamond(double x, double y, double size) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x, y - size);
        path.lineTo(x + 0.62 * size, y);
        path.lineTo(x, y + size);
        path.lineTo(x - 0.62 * size, y);
        path.closePath();
        return path;
    }

    private void bind(Runnable action, int... keys) {
        String name = "map-" + keys[0];
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
}
