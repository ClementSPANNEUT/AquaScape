package screen;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/** Drives the screens in the tests: runs code on the Swing thread, sends keys and mouse events, paints. */
final class Swing {
    interface Body {
        void run() throws Exception;
    }

    private Swing() {
    }

    static void onEdt(Body body) throws Exception {
        Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                body.run();
            } catch (Throwable e) {
                failure[0] = e;
            }
        });
        if (failure[0] instanceof Exception e) {
            throw e;
        }
        if (failure[0] instanceof Error e) {
            throw e;
        }
    }

    static void press(Component component, int key) {
        KeyEvent event = new KeyEvent(component, KeyEvent.KEY_PRESSED, 0, 0, key, KeyEvent.CHAR_UNDEFINED);
        for (KeyListener listener : component.getKeyListeners()) {
            listener.keyPressed(event);
        }
    }

    static void release(Component component, int key) {
        KeyEvent event = new KeyEvent(component, KeyEvent.KEY_RELEASED, 0, 0, key, KeyEvent.CHAR_UNDEFINED);
        for (KeyListener listener : component.getKeyListeners()) {
            listener.keyReleased(event);
        }
    }

    static void binding(JComponent component, int key) {
        Object name = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(key, 0));
        component.getActionMap().get(name).actionPerformed(null);
    }

    static void move(Component component, int x, int y) {
        MouseEvent event = new MouseEvent(component, MouseEvent.MOUSE_MOVED, 0, 0, x, y, 0, false);
        for (MouseMotionListener listener : component.getMouseMotionListeners()) {
            listener.mouseMoved(event);
        }
    }

    static void drag(Component component, int x, int y) {
        MouseEvent event = new MouseEvent(component, MouseEvent.MOUSE_DRAGGED, 0, 0, x, y, 0, false);
        for (MouseMotionListener listener : component.getMouseMotionListeners()) {
            listener.mouseDragged(event);
        }
    }

    static void click(Component component, int x, int y) {
        MouseEvent event = new MouseEvent(component, MouseEvent.MOUSE_PRESSED, 0, 0, x, y, 1, false);
        for (MouseListener listener : component.getMouseListeners()) {
            listener.mousePressed(event);
        }
    }

    static void leave(Component component) {
        MouseEvent event = new MouseEvent(component, MouseEvent.MOUSE_EXITED, 0, 0, -1, -1, 0, false);
        for (MouseListener listener : component.getMouseListeners()) {
            listener.mouseExited(event);
        }
    }

    static BufferedImage paint(JComponent component, int width, int height) {
        component.setSize(width, height);
        layOut(component);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        component.printAll(g);
        g.dispose();
        return image;
    }

    static int colours(BufferedImage image) {
        Set<Integer> colours = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y += 3) {
            for (int x = 0; x < image.getWidth(); x += 3) {
                colours.add(image.getRGB(x, y));
            }
        }
        return colours.size();
    }

    static boolean same(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void layOut(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container inner) {
                layOut(inner);
            }
        }
    }
}
