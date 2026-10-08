package zone;

import java.util.ArrayList;
import java.util.List;
import world.Door;
import world.Plan;
import world.Tunnel;
import world.World;
import world.Zone;

/**
 * The centre of the map: the hub, the spokes that lead to every zone, the golden gate before Fin and the start
 * area, a winding corridor guarded by a few enemies. Its plan is the overview of the world, 16 world px per
 * plan px; it is laid out first, so the grid uses it wherever no other zone is dug.
 */
public final class HubZone {
    /** Where the overview lies in the world. */
    public static final Plan PLAN = new Plan("Hub", 0, 0, 2100, 2240, 0, 16);
    /** Width of the whole world, in px. */
    public static final int WORLD_WIDTH = 18_200;
    /** Height of the whole world, in px. */
    public static final int WORLD_HEIGHT = 17_700;

    private static final double HUB_X = 447;
    private static final double HUB_Y = 414;
    private static final double HUB_RADIUS = 22.5;
    private static final double SPOKE_WIDTH = 300;
    private static final double START_X = 182.5;
    private static final double START_Y = 415;
    private static final double START_RADIUS = 15;
    private static final double JUNCTION_X = 331;
    private static final double JUNCTION_Y = 415;
    private static final double JUNCTION_RADIUS = 14;
    private static final double SERPENTINE_WIDTH = 17.5;
    private static final double TOP_Y = 397.5;
    private static final double BOTTOM_Y = 433.8;
    private static final double GATE_SHARE = 0.28;
    private static final double GATE_OVERSHOOT = 14;

    private HubZone() {
    }

    /**
     * Digs the hub, the spokes and the start area into the tunnel.
     * @param tunnel the tunnel of the world
     */
    public static void carve(Tunnel tunnel) {
        PLAN.chamber(tunnel, HUB_X, HUB_Y, HUB_RADIUS);
        for (Plan zone : zones()) {
            spoke(tunnel, zone.anchorX(), zone.anchorY());
        }
        spoke(tunnel, PLAN.x(JUNCTION_X, JUNCTION_Y), PLAN.y(JUNCTION_X, JUNCTION_Y));

        PLAN.chamber(tunnel, START_X, START_Y, START_RADIUS);
        PLAN.chamber(tunnel, JUNCTION_X, JUNCTION_Y, JUNCTION_RADIUS);
        PLAN.corridor(tunnel, SERPENTINE_WIDTH, serpentine());
    }

    /**
     * Adds the start and hub checkpoints, the golden gate and the enemies of the start.
     * @param world the world to fill
     */
    public static void layOut(World world) {
        Zone zone = new Zone(world, PLAN);
        world.setArea("Départ");
        zone.checkpoint(START_X, START_Y);
        world.setArea("Hub");
        world.setHub(zone.checkpoint(HUB_X, HUB_Y));
        gate(world);
        start(zone);
        barriers(zone);
    }

    private static List<Plan> zones() {
        return List.of(SplitZone.PLAN, DashZone.PLAN, ShootZone.PLAN, ParyZone.PLAN, LightZone.PLAN, FinZone.PLAN);
    }

    private static void spoke(Tunnel tunnel, double toX, double toY) {
        tunnel.corridor(SPOKE_WIDTH, PLAN.x(HUB_X, HUB_Y), PLAN.y(HUB_X, HUB_Y), toX, toY);
    }

    private static void gate(World world) {
        double hubX = PLAN.x(HUB_X, HUB_Y);
        double hubY = PLAN.y(HUB_X, HUB_Y);
        double dx = FinZone.PLAN.anchorX() - hubX;
        double dy = FinZone.PLAN.anchorY() - hubY;
        double length = Math.hypot(dx, dy);
        double x = hubX + dx * GATE_SHARE;
        double y = hubY + dy * GATE_SHARE;
        double half = SPOKE_WIDTH / 2 + GATE_OVERSHOOT;
        double acrossX = -dy / length * half;
        double acrossY = dx / length * half;
        world.addDoor(new Door(x - acrossX, y - acrossY, x + acrossX, y + acrossY, true));
    }

    private static double[] serpentine() {
        List<Double> points = new ArrayList<>(List.of(START_X, START_Y, 210.4, START_Y));
        arc(points, 221.65, TOP_Y, 11.25, 180, 360);
        arc(points, 244.55, BOTTOM_Y, 11.65, 180, 0);
        arc(points, 267.9, TOP_Y, 11.7, 180, 360);
        arc(points, 290.5, BOTTOM_Y, 11.1, 180, 0);
        points.addAll(List.of(301.6, JUNCTION_Y, JUNCTION_X, JUNCTION_Y));
        double[] array = new double[points.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = points.get(i);
        }
        return array;
    }

    private static void arc(List<Double> points, double centerX, double centerY, double radius, double from, double to) {
        int steps = 16;
        for (int i = 0; i <= steps; i++) {
            double angle = Math.toRadians(from + (to - from) * i / steps);
            points.add(centerX + radius * Math.cos(angle));
            points.add(centerY + radius * Math.sin(angle));
        }
    }

    private static void barriers(Zone zone) {
        zone.barrier(341.9, 405.8, 341.9, 424.3);
        zone.barrier(319.4, 423.8, 319.4, 406.3);
    }


    private static void start(Zone zone) {
        zone.gater(201.7, 405, 219.2, 405);
        zone.speeder(216.4, 397, 204.5, 397);
        zone.speeder(210, 382, 218, 393);
        zone.speeder(221.5, 392, 221.5, 380);
        zone.speeder(233.5, 382, 225.5, 393);
        zone.speeder(225.5, 397, 239, 397);
        zone.spinner(232.9, 409, 8.4, 0, 60);
        zone.spinner(232.9, 420, 8.4, 0, -60);
        zone.spinner(232.9, 431, 8.4, 0, 60);
        zone.speeder(251.5, 433, 251.5, 398.5);
        zone.speeder(260.5, 398.5, 260.5, 433);
        zone.spinner(256, 423, 5, 0, 180);
        zone.spinner(256, 405, 5, 0, 180);
        zone.gater(270.9, 400, 288.4, 400);
        zone.speeder(286, 403, 272, 430);
        zone.spinner(279.6, 416, 7.4, 120, -60);
        zone.speeder(272, 403, 286, 430);
        zone.gater(270.9, 433, 288.4, 433);
        zone.gater(318, 406, 318, 424);
        zone.bouncer(322.6, 409, 1, -0.6);
        zone.bouncer(330.6, 422, 1, -0.6);
        zone.bouncer(335.6, 409, 1, -0.6);
        zone.bouncer(330.6, 409, 1, -0.6);
        zone.gater(343.4, 405.7,343.5, 424.4);
    }
}
