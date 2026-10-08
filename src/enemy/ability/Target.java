package enemy.ability;

/**
 * Something a {@link Shot} can hit as it flies over it, such as the button of a door. The world's objects
 * implement it, so the abilities don't depend on the world.
 */
public interface Target {
    /**
     * Tells whether a round object lies on the target.
     * @param x centre x of the object, in world px
     * @param y centre y of the object, in world px
     * @param radius radius of the object, in px
     * @return {@code true} if they overlap
     */
    boolean isUnder(double x, double y, double radius);

    /** Hits the target. */
    void strike();
}
