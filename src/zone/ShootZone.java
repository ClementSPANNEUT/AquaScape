package zone;

import world.Plan;
import world.Tunnel;
import world.World;
import world.Zone;

/**
 * The Shoot zone, built from its plan: straight corridors joined by round rooms and a kaizo shortcut, guarded
 * by many gaters.
 */
public final class ShootZone {
    /** Where the plan of the zone lies in the world. */
    public static final Plan PLAN = new Plan("Shoot", 49.5, 451.4, 11601.2, 11027.0, 40.202, 2 * 84 / 42.6 * 1.5);
    private static final double CORRIDOR = 47.3;
    private static final double KAIZO = 42.6;

    private ShootZone() {
    }

    /**
     * Digs the corridors and rooms of the zone into the tunnel.
     * @param tunnel the tunnel of the world
     */
    public static void carve(Tunnel tunnel) {
        PLAN.chamber(tunnel, 51, 421, 48);
        PLAN.corridor(tunnel, CORRIDOR, 51, 421, 213.5, 312.4, 213.5, 429.1);
        PLAN.corridor(tunnel, CORRIDOR, 308, 429.1, 308, 184.4);
        PLAN.corridor(tunnel, CORRIDOR, 399.5, 184.4, 399.5, 493.6);
        PLAN.corridor(tunnel, CORRIDOR, 491, 493.6, 491, 143.4);
        PLAN.corridor(tunnel, CORRIDOR, 582.5, 143.4, 582.5, 555.1);
        PLAN.corridor(tunnel, CORRIDOR, 673, 555.1, 673, 194.7);
        PLAN.corridor(tunnel, CORRIDOR, 815, 194.7, 815, 871);
        PLAN.chamber(tunnel, 260.8, 429.1, 70.9);
        PLAN.chamber(tunnel, 353.8, 184.4, 69.4);
        PLAN.chamber(tunnel, 445.2, 493.6, 69.4);
        PLAN.chamber(tunnel, 536.8, 143.4, 69.4);
        PLAN.chamber(tunnel, 627.8, 555.1, 68.9);
        PLAN.chamber(tunnel, 744, 194.7, 94.7);
        PLAN.chamber(tunnel, 815, 661, 52);
        PLAN.chamber(tunnel, 815, 871, 55);
        PLAN.corridor(tunnel, KAIZO, 51, 421, 815, 871);
        PLAN.chamber(tunnel, 276, 553, 50);
        PLAN.chamber(tunnel, 522, 698, 50);
        PLAN.chamber(tunnel, 679, 790.5, 50);
    }

    /**
     * Adds the checkpoints, the light, the enemies and the rest of the zone to the world.
     * @param world the world to fill
     */
    public static void layOut(World world) {
        Zone zone = new Zone(world, PLAN);
        zone.checkpoint(49.5, 451.4);
        zone.checkpoint(308, 385.5);
        zone.checkpoint(581.9, 385.4);
        zone.checkpoint(815.4, 461.7);
        zone.light("Shoot", 815, 871);
        bouncers(zone);
        speeders(zone);
        gaters(zone);
        spinners(zone);
        barriers(zone);
    }

    private static void bouncers(Zone zone) {
        zone.bouncer(294, 479, -0.94, -0.34);
        zone.bouncer(238, 478, 0.49, -0.87);
        zone.bouncer(315, 147, -0.94, -0.35);
        zone.bouncer(392, 147, 0.94, -0.33);
        zone.bouncer(390, 513, -0.95, -0.32);
        zone.bouncer(500, 514, 0.94, -0.33);
        zone.bouncer(536, 87, -0.40, -0.92);
        zone.bouncer(498, 109, -0.86, -0.51);
        zone.bouncer(628, 609, -0.04, 1.00);
        zone.bouncer(593, 583, -1.00, 0.04);
        zone.bouncer(709, 125, -0.12, -0.99);
        zone.bouncer(780, 126, 0.08, -1.00);
        zone.bouncer(815, 706, -0.01, -1.00);
        zone.bouncer(250, 572, 0.85, -0.53);
        zone.bouncer(488, 698, 0.77, -0.64);
        zone.bouncer(718, 801, -0.48, 0.88);
    }

    private static void speeders(Zone zone) {
        zone.speeder(831.1, 258, 798.9, 258);
        zone.speeder(831.1, 727.5, 798.9, 727.5);
        zone.speeder(831.1, 740, 798.9, 740);
        zone.speeder(656.9, 280.4, 689.1, 280.4);
        zone.speeder(656.9, 268, 689.1, 268);
        zone.speeder(598.6, 270, 566.4, 270);
        zone.speeder(598.6, 282.5, 566.4, 282.5);
        zone.speeder(415.6, 281, 383.4, 281);
        zone.speeder(291.9, 344, 324.1, 344);
        zone.speeder(184.5, 483.7, 170.6, 507.3);
        zone.speeder(194.8, 489.8, 180.9, 513.4);
        zone.speeder(333.9, 571.8, 320, 595.4);
        zone.speeder(770.3, 828.8, 756.4, 852.3);
        zone.speeder(504.8, 228.3, 486, 238);
        zone.speeder(399.4, 400, 399, 441);
        zone.speeder(674.2, 487.5, 673, 441);
        zone.speeder(815, 522, 815, 563);
        zone.speeder(830.5, 752.8, 832, 779);
        zone.speeder(291.9, 282.5, 324.1, 282.5);
        zone.speeder(474.9, 393.5, 507.1, 393.5);
        zone.speeder(598.6, 463.7, 566.4, 463.7);
        zone.speeder(656.9, 345.7, 689.1, 345.7);
        zone.speeder(831.1, 372.1, 798.9, 372.1);
        zone.speeder(164.3, 326, 182.1, 352.7);
        zone.speeder(420.6, 622.8, 406.7, 646.4);
    }

    private static void gaters(Zone zone) {
        zone.gater(606.8, 218.1, 558.2, 218.1);
        zone.gater(423.8, 240.7, 375.2, 240.7);
        zone.gater(466.7, 331.3, 515.3, 331.3);
        zone.gater(237.8, 351.2, 189.2, 351.2);
        zone.gater(648.7, 411, 697.3, 411);
        zone.gater(161.5, 460.6, 139.1, 498.4);
        zone.gater(839.3, 583.8, 790.7, 583.8);
        zone.gater(378.6, 588.4, 356.3, 626.3);
        zone.gater(594, 715.3, 571.7, 753.2);
        zone.gater(423.8, 340.5, 375.2, 340.5);
        zone.gater(839.3, 323.1, 790.7, 323.1);
        zone.gater(457.2, 634.7, 434.9, 672.6);
    }

    private static void spinners(Zone zone) {
        zone.spinner(353.8, 184.4, 34.7, 37, 60);
        zone.spinner(445.2, 493.6, 34.7, 74, -60);
        zone.spinner(536.8, 143.4, 34.7, 111, 60);
        zone.spinner(627.8, 555.1, 34.5, 148, -60);
        zone.spinner(744, 194.7, 47.3, 5, 60);
        zone.spinner(815, 661, 26, 42, -60);
        zone.spinner(276, 553, 25, 0, 40);
        zone.spinner(276, 553, 49, 0, -40);
        zone.spinner(276, 553, 49, 90, -40);
        zone.spinner(522, 698, 25, 116, -60);
        zone.spinner(679, 790.5, 25, 153, 60);
        zone.spinner(491, 284.6, 19.9, 0, -60);
        zone.spinner(132.9, 366.3, 19.9, 56.2, 60);
    }

    private static void barriers(Zone zone) {
        zone.barrier(185.8, 360.2, 241.2, 360.2);
        zone.barrier(280.3, 360.2, 335.7, 360.2);
        zone.barrier(280.3, 252.2, 335.7, 252.2);
        zone.barrier(371.8, 252.2, 427.2, 252.2);
        zone.barrier(371.8, 425.8, 427.2, 425.8);
        zone.barrier(463.3, 425.8, 518.7, 425.8);
        zone.barrier(463.3, 211.2, 518.7, 211.2);
        zone.barrier(554.8, 211.2, 610.2, 211.2);
        zone.barrier(554.8, 487.6, 610.2, 487.6);
        zone.barrier(645.3, 487.6, 700.7, 487.6);
        zone.barrier(645.3, 278.7, 700.7, 278.7);
        zone.barrier(787.3, 278.7, 842.7, 278.7);
        zone.barrier(842.7, 612.7, 787.3, 612.7);
        zone.barrier(842.7, 709.3, 787.3, 709.3);
        zone.barrier(248.1, 507.2, 222.5, 550.8);
        zone.barrier(329.5, 555.2, 303.9, 598.8);
        zone.barrier(494.1, 652.2, 468.5, 695.8);
        zone.barrier(575.5, 700.2, 549.9, 743.8);
        zone.barrier(651.1, 744.7, 625.5, 788.3);
        zone.barrier(732.5, 792.7, 706.9, 836.3);
    }
}
