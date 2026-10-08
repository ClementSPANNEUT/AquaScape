package enemy;

import java.util.List;


/**
 * What an enemy can see of the world it moves in.
 * <p>The {@code enemy} package only knows the world through this interface and {@link Obstacle}, so enemies
 * don't depend on the rest of the game: the world implements it.
 */
public interface Arena {
    /** {@return the width of the world, in px} */
    int width();

    /** {@return the height of the world, in px} */
    int height();

    /**
     * Returns the walls an enemy could hit around a point: tunnel edges, neon walls and closed doors.
     * @param x x of the point, in world px
     * @param y y of the point, in world px
     * @param reach distance around the point to look at, in px
     * @return the obstacles near the point; the list may also hold farther ones
     */
    List<? extends Obstacle> obstaclesNear(double x, double y, double reach);

    /**
     * Returns the invisible barriers around a point. They only stop the enemies that {@linkplain
     * Enemy#stopsAtBarriers() respect them}, never the player.
     * @param x x of the point, in world px
     * @param y y of the point, in world px
     * @param reach distance around the point to look at, in px
     * @return the barriers near the point; the list may also hold farther ones
     */
    List<? extends Obstacle> barriersNear(double x, double y, double reach);

    /** {@return every enemy of the world} */
    List<? extends Enemy> enemies();
}
