package world;

import enemy.Bouncer;
import enemy.Gater;
import enemy.Speeder;
import enemy.Spinner;

/**
 * Builds a zone of the map: its checkpoints, lights, enemies, barriers, doors and buttons, given in plan
 * coordinates and placed in the world through its {@link Plan}.
 */
public final class Zone {
    private static final double BARRIER_OVERSHOOT = 4;

    private final World world;
    private final Plan plan;

    /**
     * Creates a builder for one plan and records the plan in the world, for the coordinate grid.
     * @param world the world to fill
     * @param plan where the plan lies in the world
     */
    public Zone(World world, Plan plan) {
        this.world = world;
        this.plan = plan;
        world.addPlan(plan);
    }

    /**
     * Adds a checkpoint.
     * @param x x in the plan
     * @param y y in the plan
     * @return the index of the checkpoint in the world
     */
    public int checkpoint(double x, double y) {
        return world.addCheckpoint(plan.x(x, y), plan.y(x, y));
    }

    /**
     * Adds a light to bring back to the hub.
     * @param name name of the light, shown to the player
     * @param x x in the plan
     * @param y y in the plan
     */
    public void light(String name, double x, double y) {
        world.addLight(new Light(name, plan.x(x, y), plan.y(x, y), false));
    }

    /**
     * Adds the last light: reaching it wins the level.
     * @param name name of the light, shown to the player
     * @param x x in the plan
     * @param y y in the plan
     */
    public void lastLight(String name, double x, double y) {
        world.addLight(new Light(name, plan.x(x, y), plan.y(x, y), true));
    }

    /**
     * Adds a {@link enemy.Bouncer}.
     * @param x x in the plan
     * @param y y in the plan
     * @param directionX x of its direction in the plan
     * @param directionY y of its direction in the plan
     */
    public void bouncer(double x, double y, double directionX, double directionY) {
        double[] direction = plan.direction(directionX, directionY);
        world.addEnemy(new Bouncer(plan.x(x, y), plan.y(x, y), direction[0], direction[1]));
    }

    /**
     * Adds a {@link enemy.Speeder} patrolling between two points.
     * @param x1 x of the first end, in the plan
     * @param y1 y of the first end, in the plan
     * @param x2 x of the second end, in the plan
     * @param y2 y of the second end, in the plan
     */
    public void speeder(double x1, double y1, double x2, double y2) {
        world.addEnemy(new Speeder(plan.x(x1, y1), plan.y(x1, y1), plan.x(x2, y2), plan.y(x2, y2)));
    }

    /**
     * Adds a {@link enemy.Spinner}.
     * @param x x in the plan
     * @param y y in the plan
     * @param halfLength arm length, in plan px
     * @param angle starting angle in the plan, in degrees
     * @param degreesPerSecond turning speed; a negative speed turns the other way
     */
    public void spinner(double x, double y, double halfLength, double angle, double degreesPerSecond) {
        world.addEnemy(new Spinner(plan.x(x, y), plan.y(x, y), plan.length(halfLength), plan.worldAngle(angle),
                degreesPerSecond));
    }

    /**
     * Adds a {@link enemy.Gater} across a corridor.
     * @param x1 x of the first end, in the plan
     * @param y1 y of the first end, in the plan
     * @param x2 x of the second end, in the plan
     * @param y2 y of the second end, in the plan
     */
    public void gater(double x1, double y1, double x2, double y2) {
        world.addEnemy(new Gater(plan.x(x1, y1), plan.y(x1, y1), plan.x(x2, y2), plan.y(x2, y2)));
    }

    /**
     * Adds an invisible barrier for the bouncers along points; its ends are stretched a little into the walls.
     * @param points x and y of each point in turn, in plan px
     */
    public void barrier(double... points) {
        double[] inWorld = plan.points(points);
        stretch(inWorld, 0, 2);
        stretch(inWorld, inWorld.length - 2, inWorld.length - 4);
        world.addBarrier(inWorld);
    }

    /**
     * Adds a door that buttons open.
     * @param x1 x of the first end, in the plan
     * @param y1 y of the first end, in the plan
     * @param x2 x of the second end, in the plan
     * @param y2 y of the second end, in the plan
     * @return the index of the door
     */
    public int door(double x1, double y1, double x2, double y2) {
        return world.addDoor(new Door(plan.x(x1, y1), plan.y(x1, y1), plan.x(x2, y2), plan.y(x2, y2), false));
    }

    /**
     * Adds a gate: a door that opens once every light is brought back.
     * @param x1 x of the first end, in the plan
     * @param y1 y of the first end, in the plan
     * @param x2 x of the second end, in the plan
     * @param y2 y of the second end, in the plan
     * @return the index of the gate
     */
    public int gate(double x1, double y1, double x2, double y2) {
        return world.addDoor(new Door(plan.x(x1, y1), plan.y(x1, y1), plan.x(x2, y2), plan.y(x2, y2), true));
    }

    /**
     * Adds a button that keeps a door open while a drop is on it.
     * @param door index of the door
     * @param x x in the plan
     * @param y y in the plan
     */
    public void holdButton(int door, double x, double y) {
        world.addButton(new DoorButton(door, plan.x(x, y), plan.y(x, y), false));
    }

    /**
     * Adds a button that opens a door for good.
     * @param door index of the door
     * @param x x in the plan
     * @param y y in the plan
     */
    public void lockButton(int door, double x, double y) {
        world.addButton(new DoorButton(door, plan.x(x, y), plan.y(x, y), true));
    }

    private void stretch(double[] points, int end, int neighbour) {
        double dx = points[end] - points[neighbour];
        double dy = points[end + 1] - points[neighbour + 1];
        double length = Math.hypot(dx, dy);
        if (length == 0) {
            return;
        }
        points[end] += dx / length * plan.length(BARRIER_OVERSHOOT);
        points[end + 1] += dy / length * plan.length(BARRIER_OVERSHOOT);
    }
}
