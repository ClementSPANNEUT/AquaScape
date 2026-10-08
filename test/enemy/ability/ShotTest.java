package enemy.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import enemy.Arena;
import enemy.Enemy;
import enemy.Spinner;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import world.Wall;

class ShotTest {
    private static final double FRAME = 1 / 60.0;

    private static final class Room implements Arena {
        private final List<Wall> walls = new ArrayList<>();
        private final List<Enemy> enemies = new ArrayList<>();

        @Override
        public int width() {
            return 4000;
        }

        @Override
        public int height() {
            return 4000;
        }

        @Override
        public List<Wall> obstaclesNear(double x, double y, double reach) {
            return walls;
        }

        @Override
        public List<Wall> barriersNear(double x, double y, double reach) {
            return List.of();
        }

        @Override
        public List<Enemy> enemies() {
            return enemies;
        }
    }

    private static final class Bell implements Target {
        private final double x;
        private final double y;
        private int strikes;

        private Bell(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean isUnder(double otherX, double otherY, double radius) {
            return Math.hypot(otherX - x, otherY - y) < 20 + radius;
        }

        @Override
        public void strike() {
            strikes++;
        }
    }

    private final Room room = new Room();

    private double fly(Shot shot, double dt) {
        double start = shot.x();
        for (int i = 0; i < 2000 && shot.isAlive(); i++) {
            shot.update(dt, room, List.of());
        }
        return shot.x() - start;
    }

    @Test
    void thePowerSetsTheSpeed() {
        assertEquals(Shot.MIN_SPEED, new Shot(0, 0, 1, 0, 0).speed(), 1e-9);
        assertEquals(Shot.MAX_SPEED, new Shot(0, 0, 0, 2, 1).speed(), 1e-9);
        assertEquals(Shot.MAX_SPEED, new Shot(0, 0, 0, 2, 7).speed(), 1e-9, "never more than the strongest");
        Shot half = new Shot(0, 0, 3, 4, 0.5);
        assertEquals((Shot.MIN_SPEED + Shot.MAX_SPEED) / 2, half.speed(), 1e-9);
        assertEquals(0.75, half.vx() / half.vy(), 1e-9, "along the direction, whatever its length");
    }

    @Test
    void aStrongerShotGoesFurtherBeforeDyingOut() {
        double weak = fly(new Shot(100, 2000, 1, 0, 0), FRAME);
        double strong = fly(new Shot(100, 2000, 1, 0, 1), FRAME);
        assertTrue(weak > 100 && weak < 400, "a weak shot dies out close: " + weak);
        assertTrue(strong > 800 && strong < 1400, "a strong one goes far: " + strong);
    }

    @Test
    void theDistanceDoesNotDependOnTheFrames() {
        double smooth = fly(new Shot(100, 2000, 1, 0, 0.6), FRAME);
        double jerky = fly(new Shot(100, 2000, 1, 0, 0.6), 0.05);
        assertEquals(smooth, jerky, 30);
    }

    @Test
    void itStopsOnAWallEvenWithLongFrames() {
        room.walls.add(new Wall(600, 1500, 600, 2500));
        Shot shot = new Shot(100, 2000, 1, 0, 1);
        fly(shot, 0.05);
        assertFalse(shot.isAlive());
        assertTrue(shot.x() < 600, "it didn't jump over the wall");
        assertTrue(shot.x() > 560);
    }

    @Test
    void itStopsOnAnEnemy() {
        room.enemies.add(new Spinner(600, 2000, 80, 90, 0));
        Shot shot = new Shot(100, 2000, 1, 0, 1);
        fly(shot, FRAME);
        assertFalse(shot.isAlive());
        assertTrue(shot.x() < 620);
    }

    @Test
    void itStopsWhenItLeavesTheWorld() {
        Shot shot = new Shot(3900, 2000, 1, 0, 1);
        fly(shot, FRAME);
        assertTrue(shot.x() <= 4000 + Shot.RADIUS);
    }

    @Test
    void itStrikesTheTargetsItFliesOver() {
        Bell onThePath = new Bell(600, 2000);
        Bell aside = new Bell(600, 2200);
        Shot shot = new Shot(100, 2000, 1, 0, 1);
        for (int i = 0; i < 100 && shot.isAlive(); i++) {
            shot.update(0.05, room, List.of(onThePath, aside));
        }
        assertTrue(onThePath.strikes > 0, "even when a long frame would carry it past the target");
        assertEquals(0, aside.strikes);
    }

    @Test
    void itShowsOnlyNearItself() {
        Shot shot = new Shot(500, 500, 1, 0, 1);
        assertTrue(shot.isVisibleIn(new Rectangle2D.Double(450, 450, 100, 100)));
        assertFalse(shot.isVisibleIn(new Rectangle2D.Double(900, 900, 100, 100)));
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.translate(-400, -400);
        shot.draw(g);
        g.dispose();
        assertTrue((image.getRGB(100, 100) & 0xFFFFFF) != 0, "a bright ball where it is");
        assertEquals(0, image.getRGB(190, 190) & 0xFFFFFF);
    }
}
