package screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static screen.Swing.click;
import static screen.Swing.drag;
import static screen.Swing.onEdt;
import static screen.Swing.paint;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SliderTest {
    private static final int WIDTH = 460;
    private static final int HEIGHT = 48;
    private static final int BAR_LEFT = 150;
    private static final int BAR_RIGHT = 360;

    private final List<Integer> told = new ArrayList<>();

    private Slider slider(int level) {
        Slider slider = Ui.slider("Volume", level, told::add);
        slider.setSize(WIDTH, HEIGHT);
        return slider;
    }

    private static int litSegments(Slider slider) {
        BufferedImage image = paint(slider, WIDTH, HEIGHT);
        int lit = 0;
        for (int segment = 0; segment < 10; segment++) {
            int x = BAR_LEFT + segment * (BAR_RIGHT - BAR_LEFT) / 10 + 8;
            if (image.getRGB(x, HEIGHT / 2) == Ui.TEXT.getRGB()) {
                lit++;
            }
        }
        return lit;
    }

    @Test
    void itStartsAtItsLevelAndNamesIt() throws Exception {
        onEdt(() -> {
            Slider slider = slider(70);
            assertEquals(70, slider.level());
            assertEquals("Volume : 70 %", slider.getText());
            assertEquals(List.of(), told, "nothing has changed yet");
        });
    }

    @Test
    void aLevelOutsideIsBroughtBackIn() throws Exception {
        onEdt(() -> {
            assertEquals(100, slider(250).level());
            assertEquals(0, slider(-5).level());
        });
    }

    @Test
    void itMovesOneSegmentAtATimeAndStopsAtBothEnds() throws Exception {
        onEdt(() -> {
            Slider slider = slider(80);
            slider.move(1);
            slider.move(1);
            slider.move(1);
            assertEquals(100, slider.level());
            slider.move(-1);
            assertEquals(90, slider.level());
            assertEquals("Volume : 90 %", slider.getText());
            slider.move(-20);
            assertEquals(0, slider.level());
            slider.move(-1);
            assertEquals(List.of(90, 100, 90, 0), told, "told once per change, and not when it can't move");
        });
    }

    @Test
    void aLevelBetweenTwoSegmentsMovesToTheNextOne() throws Exception {
        onEdt(() -> {
            Slider up = slider(37);
            up.move(1);
            assertEquals(40, up.level());
            Slider down = slider(37);
            down.move(-1);
            assertEquals(30, down.level());
        });
    }

    @Test
    void aClickOnTheBarPicksTheLevelUnderTheMouse() throws Exception {
        onEdt(() -> {
            Slider slider = slider(100);
            click(slider, (BAR_LEFT + BAR_RIGHT) / 2, HEIGHT / 2);
            assertEquals(50, slider.level());
            click(slider, BAR_LEFT + 3 * (BAR_RIGHT - BAR_LEFT) / 10 + 2, HEIGHT / 2);
            assertEquals(30, slider.level());
            click(slider, BAR_LEFT, HEIGHT / 2);
            assertEquals(0, slider.level());
            click(slider, BAR_RIGHT, HEIGHT / 2);
            assertEquals(100, slider.level());
            assertEquals(List.of(50, 30, 0, 100), told);
        });
    }

    @Test
    void aClickOnTheNameOrTheValueChangesNothing() throws Exception {
        onEdt(() -> {
            Slider slider = slider(60);
            click(slider, 30, HEIGHT / 2);
            click(slider, WIDTH - 30, HEIGHT / 2);
            assertEquals(60, slider.level());
            click(slider, BAR_LEFT - 8, HEIGHT / 2);
            assertEquals(0, slider.level(), "just before the bar still reaches its first segment");
            click(slider, BAR_RIGHT + 8, HEIGHT / 2);
            assertEquals(100, slider.level(), "and just after reaches the last one");
        });
    }

    @Test
    void draggingFollowsTheMouse() throws Exception {
        onEdt(() -> {
            Slider slider = slider(0);
            for (int x = BAR_LEFT; x <= BAR_RIGHT; x += 3) {
                drag(slider, x, HEIGHT / 2);
            }
            assertEquals(100, slider.level());
            assertEquals(List.of(10, 20, 30, 40, 50, 60, 70, 80, 90, 100), told, "one change per segment");
        });
    }

    @Test
    void itLightsOneSegmentPerTenPercent() throws Exception {
        onEdt(() -> {
            assertEquals(0, litSegments(slider(0)));
            assertEquals(3, litSegments(slider(30)));
            assertEquals(7, litSegments(slider(70)));
            assertEquals(10, litSegments(slider(100)));
        });
    }
}
