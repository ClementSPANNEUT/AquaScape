package world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WallGridTest {
    @Test
    void findsTheWallsNearAPointOnly() {
        Wall near = new Wall(100, 100, 150, 100);
        Wall far = new Wall(5000, 5000, 5100, 5000);
        WallGrid grid = new WallGrid();
        grid.addAll(List.of(near, far));
        List<Wall> found = new ArrayList<>();
        grid.collect(120, 110, 20, found);
        assertTrue(found.contains(near));
        assertFalse(found.contains(far));
    }

    @Test
    void aLongWallIsFoundAllAlongIt() {
        Wall wall = new Wall(0, 0, 2000, 0);
        WallGrid grid = new WallGrid();
        grid.add(wall);
        for (int x = 0; x <= 2000; x += 250) {
            List<Wall> found = new ArrayList<>();
            grid.collect(x, 10, 15, found);
            assertTrue(found.contains(wall), "found near x = " + x);
        }
    }

    @Test
    void aWallCrossingSeveralCellsIsFoundOnce() {
        Wall wall = new Wall(0, 250, 600, 250);
        WallGrid grid = new WallGrid();
        grid.add(wall);
        List<Wall> found = new ArrayList<>();
        grid.collect(256, 256, 300, found);
        assertEquals(1, found.size());
    }

    @Test
    void aWallIsFoundAcrossNegativeCoordinates() {
        Wall wall = new Wall(-300, -300, -260, -300);
        WallGrid grid = new WallGrid();
        grid.add(wall);
        List<Wall> found = new ArrayList<>();
        grid.collect(-280, -290, 15, found);
        assertTrue(found.contains(wall));
    }
}
