package enemy;

import java.awt.geom.Point2D;


/** Something an enemy bounces on: a segment with a thickness, such as a wall or a barrier. */
public interface Obstacle {
    /**
     * Finds the point of the obstacle closest to a position.
     * @param x x of the position, in world px
     * @param y y of the position, in world px
     * @return the closest point of the obstacle's centre line
     */
    Point2D closestPoint(double x, double y);

    /** {@return half the thickness of the obstacle, in px} */
    double halfThickness();

    /**
     * Tells whether a disc overlaps the obstacle.
     * @param x centre x of the disc, in world px
     * @param y centre y of the disc, in world px
     * @param radius radius of the disc, in px
     * @return {@code true} if the disc touches the obstacle
     */
    default boolean touches(double x, double y, double radius) {
        return closestPoint(x, y).distance(x, y) < radius + halfThickness();
    }
}
