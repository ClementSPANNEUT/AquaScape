package enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import world.Wall;

class BouncerTest {
    private static final double SPEED = 170;

    @Test
    void keepsAConstantSpeed() {
        TestArena arena = new TestArena(2000, 2000);
        Bouncer bouncer = new Bouncer(1000, 1000, 3, 4);
        arena.enemies.add(bouncer);
        for (int i = 0; i < 20; i++) {
            arena.run(0.5);
            assertEquals(SPEED, Math.hypot(bouncer.vx, bouncer.vy), 1e-9);
        }
    }

    @Test
    void bouncesOnAWall() {
        TestArena arena = new TestArena(2000, 2000);
        arena.walls.add(new Wall(600, 0, 600, 2000));
        Bouncer bouncer = new Bouncer(500, 1000, 1, 0);
        arena.enemies.add(bouncer);
        arena.run(2);
        assertTrue(bouncer.vx < 0, "goes back after the wall");
        assertTrue(bouncer.x() < 600 - bouncer.radius(), "never crosses the wall");
    }

    @Test
    void stopsAtTheInvisibleBarriers() {
        TestArena arena = new TestArena(2000, 2000);
        arena.barriers.add(new Wall(600, 0, 600, 2000));
        Bouncer bouncer = new Bouncer(500, 1000, 1, 0);
        arena.enemies.add(bouncer);
        arena.run(2);
        assertTrue(bouncer.x() < 600, "barriers stop bouncers");
    }

    @Test
    void bouncesOffAnotherBouncer() {
        TestArena arena = new TestArena(2000, 2000);
        Bouncer left = new Bouncer(900, 1000, 1, 0);
        Bouncer right = new Bouncer(1100, 1000, -1, 0);
        arena.enemies.add(left);
        arena.enemies.add(right);
        arena.run(1.5);
        assertTrue(left.vx < 0 && right.vx > 0, "they go back where they came from");
        assertTrue(right.x() - left.x() >= left.radius() + right.radius() - 1e-6, "they don't overlap");
    }

    @Test
    void staysInsideTheWorld() {
        TestArena arena = new TestArena(400, 300);
        Bouncer bouncer = new Bouncer(200, 150, 1, 0.7);
        arena.enemies.add(bouncer);
        for (int i = 0; i < 600; i++) {
            arena.run(1 / 60.0);
            assertTrue(bouncer.x() >= bouncer.radius() && bouncer.x() <= 400 - bouncer.radius());
            assertTrue(bouncer.y() >= bouncer.radius() && bouncer.y() <= 300 - bouncer.radius());
        }
    }
}
