package screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import world.Level;
import world.Progress;
import world.World;

class CameraTest {
    private static World room(int width, int height, double dropX, double dropY) {
        return new World(new Level("room", "", width, height, w -> w.addDroplet(dropX, dropY, 1)), new Progress());
    }

    @Test
    void snapsOntoAStillDrop() {
        Camera camera = new Camera();
        camera.snap(room(3000, 3000, 1500, 1500), 800, 600);
        assertEquals(1100, camera.x(), 1e-9);
        assertEquals(1200, camera.y(), 1e-9);
    }

    @Test
    void showsAtMostAMarginPastTheEdges() {
        Camera camera = new Camera();
        camera.snap(room(3000, 3000, 40, 40), 800, 600);
        assertEquals(-150, camera.x(), 1e-9);
        assertEquals(-150, camera.y(), 1e-9);
    }

    @Test
    void centresAWorldSmallerThanTheScreen() {
        Camera camera = new Camera();
        camera.snap(room(400, 300, 200, 150), 800, 600);
        assertEquals(-200, camera.x(), 1e-9);
        assertEquals(-150, camera.y(), 1e-9);
    }

    @Test
    void glidesTowardTheDropInsteadOfJumping() {
        Camera camera = new Camera();
        World world = room(6000, 6000, 1500, 1500);
        camera.snap(world, 800, 600);
        World moved = room(6000, 6000, 2500, 1500);
        camera.follow(1 / 60.0, moved, 800, 600);
        assertTrue(camera.x() > 1100 && camera.x() < 2100, "moves part of the way only");
    }

    @Test
    void waitsForTheScreenToHaveASize() {
        Camera camera = new Camera();
        World world = room(3000, 3000, 1500, 1500);
        camera.snap(world, 0, 0);
        camera.follow(1 / 60.0, world, 800, 600);
        assertEquals(1100, camera.x(), 1e-9, "snaps on the first follow with a size");
    }
}
