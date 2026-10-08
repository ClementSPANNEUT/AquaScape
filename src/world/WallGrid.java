package world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A spatial grid of walls: each wall is filed in the 256 px cells it crosses, so only the walls near a point
 * are tested instead of all of them.
 */
public final class WallGrid {
    private static final double CELL = 256;

    private final Map<Long, List<Wall>> cells = new HashMap<>();

    /** Creates an empty grid. */
    public WallGrid() {
    }

    /**
     * Files a wall in the cells it crosses.
     * @param wall the wall
     */
    public void add(Wall wall) {
        double reach = wall.halfThickness();
        int left = cell(wall.minX() - reach);
        int right = cell(wall.maxX() + reach);
        int top = cell(wall.minY() - reach);
        int bottom = cell(wall.maxY() + reach);
        for (int row = top; row <= bottom; row++) {
            for (int column = left; column <= right; column++) {
                cells.computeIfAbsent(key(column, row), k -> new ArrayList<>()).add(wall);
            }
        }
    }

    /**
     * Files several walls.
     * @param walls the walls
     */
    public void addAll(List<Wall> walls) {
        for (Wall wall : walls) {
            add(wall);
        }
    }

    /**
     * Adds the walls filed near a point to a list, each one once even when it crosses several cells.
     * @param x x, in world px
     * @param y y, in world px
     * @param reach distance around the point, in px
     * @param into list that receives the walls
     */
    public void collect(double x, double y, double reach, List<Wall> into) {
        int left = cell(x - reach);
        int right = cell(x + reach);
        int top = cell(y - reach);
        int bottom = cell(y + reach);
        if (left == right && top == bottom) {
            List<Wall> walls = cells.get(key(left, top));
            if (walls != null) {
                into.addAll(walls);
            }
            return;
        }
        Set<Wall> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int row = top; row <= bottom; row++) {
            for (int column = left; column <= right; column++) {
                List<Wall> walls = cells.get(key(column, row));
                if (walls == null) {
                    continue;
                }
                for (Wall wall : walls) {
                    if (seen.add(wall)) {
                        into.add(wall);
                    }
                }
            }
        }
    }

    private static int cell(double position) {
        return (int) Math.floor(position / CELL);
    }

    private static long key(int column, int row) {
        return ((long) column << 32) ^ (row & 0xffffffffL);
    }
}
