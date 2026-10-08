package enemy;

import java.util.ArrayList;
import java.util.List;
import world.Wall;

final class TestArena implements Arena {
    final List<Wall> walls = new ArrayList<>();
    final List<Wall> barriers = new ArrayList<>();
    final List<Enemy> enemies = new ArrayList<>();
    private final int width;
    private final int height;

    TestArena(int width, int height) {
        this.width = width;
        this.height = height;
    }

    void run(double seconds) {
        int frames = (int) Math.round(seconds * 60);
        for (int i = 0; i < frames; i++) {
            for (Enemy enemy : enemies) {
                enemy.update(1 / 60.0, this);
            }
        }
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public List<Wall> obstaclesNear(double x, double y, double reach) {
        return walls;
    }

    @Override
    public List<Wall> barriersNear(double x, double y, double reach) {
        return barriers;
    }

    @Override
    public List<Enemy> enemies() {
        return enemies;
    }
}
