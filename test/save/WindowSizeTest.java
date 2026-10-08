package save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.List;
import org.junit.jupiter.api.Test;

class WindowSizeTest {
    @Test
    void eachSizeShowsItsDimensions() {
        assertEquals("1280 × 720", WindowSize.HD.label());
        assertEquals(1280, WindowSize.HD.width());
        assertEquals(720, WindowSize.HD.height());
        assertEquals("Plein écran", WindowSize.FULL_SCREEN.label());
        assertEquals(WindowSize.SMALL, WindowSize.DEFAULT);
    }

    @Test
    void onlyTheSizesThatFitAreOffered() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a screen");
        List<WindowSize> sizes = WindowSize.available();
        assertTrue(sizes.contains(WindowSize.FULL_SCREEN), "full screen always fits");
        int width = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width;
        for (WindowSize size : sizes) {
            assertTrue(size == WindowSize.FULL_SCREEN || size.width() <= width, size.label());
        }
    }
}
