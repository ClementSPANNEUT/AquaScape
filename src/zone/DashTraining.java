package zone;

import world.Plan;
import world.Tunnel;
import world.World;
import world.Zone;

/**
 * The training room of the dash, offered once the light of Split is brought back: a wide hall with everything the
 * dash is for, then an exit that leads back to the hub. Its plan is the world itself, in world px.
 * <ul>
 * <li>two columns of spinners, easier to cross when the drop is thrown;</li>
 * <li>a door opened like those of the Dash zone: the back stuck on one button, the head stretched to the other;</li>
 * <li>a door whose only button is too far for the thread: the drop must tear, and leave a half on it.</li>
 * </ul>
 */
public final class DashTraining {
    /** Where the room lies: its plan px are world px. */
    public static final Plan PLAN = new Plan("Entraînement", 0, 0, 0, 0, 0, 1);
    /** Width of the room, in px. */
    public static final int WIDTH = 3400;
    /** Height of the room, in px. */
    public static final int HEIGHT = 1600;
    /** Name of the light that ends the training. */
    public static final String EXIT = "Sortie";

    private static final double HALL_Y = 800;
    private static final double HALL_WIDTH = 700;
    private static final double HALL_TOP = HALL_Y - HALL_WIDTH / 2;
    private static final double HALL_BOTTOM = HALL_Y + HALL_WIDTH / 2;
    private static final double HALL_LEFT = 350;
    private static final double HALL_RIGHT = 3050;
    private static final double START_X = 600;
    private static final double STRETCH_DOOR_X = 1600;
    private static final double BUTTON_GAP = 90;
    private static final double TEAR_BUTTON_X = 2330;
    private static final double TEAR_DOOR_X = 2700;
    private static final double EXIT_X = 2900;

    private DashTraining() {
    }

    /**
     * Digs the hall into the tunnel of the room.
     * @param tunnel the tunnel of the room
     */
    public static void carve(Tunnel tunnel) {
        PLAN.corridor(tunnel, HALL_WIDTH, HALL_LEFT, HALL_Y, HALL_RIGHT, HALL_Y);
    }

    /**
     * Adds the start, the spinners, the doors with their buttons and the exit to the room.
     * @param world the world to fill
     */
    public static void layOut(World world) {
        Zone zone = new Zone(world, PLAN);
        zone.checkpoint(START_X, HALL_Y);
        zone.spinner(1150, 590, 105, 0, 140);
        zone.spinner(1150, 800, 105, 60, -140);
        zone.spinner(1150, 1010, 105, 120, 140);

        int stretchDoor = zone.door(STRETCH_DOOR_X, HALL_TOP, STRETCH_DOOR_X, HALL_BOTTOM);
        zone.holdButton(stretchDoor, STRETCH_DOOR_X - BUTTON_GAP, HALL_Y);
        zone.lockButton(stretchDoor, STRETCH_DOOR_X + BUTTON_GAP, HALL_Y);

        zone.spinner(2050, 660, 140, 90, 200);
        zone.spinner(2050, 940, 140, 0, -200);

        int tearDoor = zone.door(TEAR_DOOR_X, HALL_TOP, TEAR_DOOR_X, HALL_BOTTOM);
        zone.holdButton(tearDoor, TEAR_BUTTON_X, HALL_Y);

        zone.light(EXIT, EXIT_X, HALL_Y);
    }
}
