package save;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/** Window sizes offered in the settings. */
public enum WindowSize {
    /** 800 × 600. */
    SMALL(800, 600),
    /** 1024 × 768. */
    MEDIUM(1024, 768),
    /** 1280 × 720. */
    HD(1280, 720),
    /** 1600 × 900. */
    LARGE(1600, 900),
    /** 1920 × 1080. */
    FULL_HD(1920, 1080),
    /** The whole screen, without a title bar. */
    FULL_SCREEN(0, 0);

    /** Size used when none is saved. */
    public static final WindowSize DEFAULT = SMALL;

    private final int width;
    private final int height;

    WindowSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /** {@return the width, in px; 0 for full screen} */
    public int width() {
        return width;
    }

    /** {@return the height, in px; 0 for full screen} */
    public int height() {
        return height;
    }

    /** {@return the text shown in the settings} */
    public String label() {
        return this == FULL_SCREEN ? "Plein écran" : width + " × " + height;
    }

    /** {@return the sizes that fit on this screen, plus full screen} */
    public static List<WindowSize> available() {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        List<WindowSize> sizes = new ArrayList<>();
        for (WindowSize size : values()) {
            if (size == FULL_SCREEN || size.width <= screen.width && size.height <= screen.height) {
                sizes.add(size);
            }
        }
        return sizes;
    }
}
