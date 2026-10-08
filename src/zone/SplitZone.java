package zone;

import world.Plan;
import world.Tunnel;
import world.World;
import world.Zone;

/**
 * The Split zone, built from its plan: straight corridors joined by round rooms, and a kaizo shortcut along the
 * diagonal.
 */
public final class SplitZone {
    /** Where the plan of the zone lies in the world. */
    public static final Plan PLAN = new Plan("Split", 50.7, 454.6, 10835.9, 6136.4, -61.903, 2 * 84 / 44.8 * 1.5);
    private static final double CORRIDOR = 49.8;
    private static final double KAIZO = 44.8;

    private SplitZone() {
    }

    /**
     * Digs the corridors and rooms of the zone into the tunnel.
     * @param tunnel the tunnel of the world
     */
    public static void carve(Tunnel tunnel) {
        PLAN.chamber(tunnel, 52, 423, 48);
        PLAN.corridor(tunnel, CORRIDOR, 52, 423, 216.5, 313, 216.5, 428.9);
        PLAN.corridor(tunnel, CORRIDOR, 309, 428.9, 309, 191.4);
        PLAN.corridor(tunnel, CORRIDOR, 400, 191.4, 400, 494.6);
        PLAN.corridor(tunnel, CORRIDOR, 491, 494.6, 491, 150.4);
        PLAN.corridor(tunnel, CORRIDOR, 582, 150.4, 582, 555.6);
        PLAN.corridor(tunnel, CORRIDOR, 673, 555.6, 673, 200.9);
        PLAN.corridor(tunnel, CORRIDOR, 815, 200.9, 815, 871);
        PLAN.chamber(tunnel, 262.8, 428.9, 71.1);
        PLAN.chamber(tunnel, 354.5, 191.4, 70.4);
        PLAN.chamber(tunnel, 445.5, 494.6, 70.4);
        PLAN.chamber(tunnel, 536.5, 150.4, 70.4);
        PLAN.chamber(tunnel, 627.5, 555.6, 70.4);
        PLAN.chamber(tunnel, 744, 200.9, 95.9);
        PLAN.chamber(tunnel, 814, 661, 52);
        PLAN.chamber(tunnel, 815, 871, 55);
        PLAN.corridor(tunnel, KAIZO, 52, 423, 815, 871);
        PLAN.chamber(tunnel, 289, 562.5, 50);
        PLAN.chamber(tunnel, 530, 704, 50);
        PLAN.chamber(tunnel, 690, 797.5, 50);
    }

    /**
     * Adds the checkpoints, the light, the enemies and the rest of the zone to the world.
     * @param world the world to fill
     */
    public static void layOut(World world) {
        Zone zone = new Zone(world, PLAN);
        zone.checkpoint(50.7, 454.6);
        zone.checkpoint(151.8, 356.1);
        zone.checkpoint(308.5, 328);
        zone.checkpoint(581.6, 387.9);
        zone.checkpoint(814.1, 458.6);
        zone.light("Split", 815, 871);
        bouncers(zone);
        speeders(zone);
        gaters(zone);
        spinners(zone);
        barriers(zone);
    }

    private static void bouncers(Zone zone) {
        // Hard zone
        zone.bouncer(529, 705, 0.14, -0.99);
        zone.bouncer(500, 700, 0.14, -0.99);

        zone.bouncer(209, 393, 0.78, 0.62);
        zone.bouncer(340, 156, 0.78, 0.62);
        zone.bouncer(400, 200, 0.78, 0.62);
        zone.bouncer(227, 474, -0.93, -0.37);
        zone.bouncer(354, 133, -0.88, -0.48);
        zone.bouncer(317, 181, -0.93, -0.36);
        zone.bouncer(445, 550, 0.00, 1.00);
        zone.bouncer(445, 477, 0.00, -1.00);
        zone.bouncer(572, 120, 0.94, 0.35);
        zone.bouncer(499, 140, -0.95, -0.32);
        zone.bouncer(589, 602, -0.85, 0.53);
        zone.bouncer(627, 611, 0.01, 1.00);
        zone.bouncer(807, 261, -0.96, -0.29);
        zone.bouncer(707, 140, -0.48, -0.88);
        zone.bouncer(814, 697, 0.00, 1.00);
        zone.bouncer(252, 581, -0.86, 0.51);
        zone.bouncer(450, 450, -0.86, 0.51);
        zone.bouncer(400, 500, -0.86, 0.51);
        zone.bouncer(500, 500, -0.86, 0.51);
        zone.bouncer(450, 450, -0.86, 0.51);
        zone.bouncer(700, 150, -0.86, 0.51);
        zone.bouncer(750, 150, -0.86, 0.51);
        zone.bouncer(750, 200, -0.86, 0.51);
        zone.bouncer(800, 250, -0.86, 0.51);
        zone.bouncer(800, 200, -0.86, 0.51);
        zone.bouncer(800, 650, -0.86, 0.51);
        zone.bouncer(850, 650, -0.86, 0.51);
    }

    private static void speeders(Zone zone) {
        zone.speeder(292.1, 299.5, 325.9, 299.5);
        zone.speeder(292.1, 284.5, 325.9, 284.5);
        zone.speeder(233.4, 365.5, 199.6, 365.5);
        zone.speeder(474.1, 420, 507.9, 420);
        zone.speeder(145.5, 461.2, 130.9, 486);
        zone.speeder(160.8, 470.2, 146.2, 495);
        zone.speeder(176, 479.1, 161.4, 504);
        zone.speeder(191.3, 488.1, 176.7, 512.9);
        zone.speeder(206.6, 497.1, 192, 521.9);
        zone.speeder(221.9, 506.1, 207.3, 530.9);
        zone.speeder(609.8, 733.8, 595.2, 758.6);
        zone.speeder(627.2, 744.1, 612.7, 768.9);
        zone.speeder(644.5, 754.2, 629.9, 779);
        zone.speeder(598.9, 501.5, 565.1, 501.5);
        zone.speeder(831.9, 504.1, 798.1, 504.1);
        zone.speeder(831.9, 519.5, 798.1, 519.5);
        zone.speeder(831.9, 774.5, 798.1, 774.5);
        zone.speeder(831.9, 789.5, 798.1, 789.5);
        zone.speeder(813.6, 551.6, 814, 594);
        zone.speeder(416.9, 283.1, 383.1, 283.1);
        zone.speeder(474.1, 279.6, 507.9, 279.6);
        zone.speeder(598.9, 251.3, 565.1, 251.3);
        zone.speeder(598.9, 298.3, 565.1, 298.3);
        zone.speeder(656.1, 370.9, 689.9, 370.9);
        zone.speeder(831.9, 310, 798.1, 310);
        zone.speeder(831.9, 350, 798.1, 350);
        zone.speeder(831.9, 390, 798.1, 390);
        zone.speeder(592.5, 723.7, 578, 748.5);
        zone.speeder(500, 403.5, 500, 290);
        zone.speeder(480, 290, 480, 403.5);
        zone.speeder(685, 480, 685, 390);
        zone.speeder(665, 390, 665, 480);
    }

    private static void gaters(Zone zone) {
        // Hard zone
        zone.gater(381.7, 589.8, 358.3, 629.7);


        zone.gater(425.6, 320.9, 374.4, 320.9);
        zone.gater(607.6, 457.6, 556.4, 457.6);
    }

    private static void spinners(Zone zone) {
        // Hard zone
        zone.spinner(289, 562.5, 50, 0, 60);
        zone.spinner(289, 562.5, 50, 60, 60);
        zone.spinner(289, 562.5, 50, 120, 60);
        zone.spinner(352, 598, 15, 140, 60);
        zone.spinner(421.5, 639.9, 18.8, 5, 60);
        zone.spinner(449.9, 656.6, 18.8, 55, -60);
        zone.spinner(478.2, 673.3, 18.8, 5, 60);
        zone.spinner(530, 704, 50, 0, -60);
        zone.spinner(530, 704, 50, 60, -60);
        zone.spinner(530, 704, 50, 120, -60);
        zone.spinner(690, 797.5, 50, 0, 50);
        zone.spinner(690, 797.5, 50, 45, 50);
        zone.spinner(690, 797.5, 50, 90, 50);
        zone.spinner(690, 797.5, 50, 135, 50);


        zone.spinner(130, 370.5, 20, 25, 60);
        zone.spinner(354.5, 191.4, 70, 0, 40);
        zone.spinner(354.5, 191.4, 70, 90, 40);
        zone.spinner(263, 429, 70, 0, -40);
        zone.spinner(263, 429, 70, 90, -40);
        zone.spinner(309, 246.5, 20.9, 25, 60);
        zone.spinner(673, 279.5, 20.9, 85, -60);
        zone.spinner(400, 375.5, 20.9, 75, 60);
        zone.spinner(445.5, 494.6, 70, 0, -40);
        zone.spinner(445.5, 494.6, 70, 90, -40);
        zone.spinner(627.5, 555.6, 70, 0, -40);
        zone.spinner(627.5, 555.6, 70, 60, -40);
        zone.spinner(627.5, 555.6, 70, 120, -40);
        
        
        zone.spinner(814, 661, 51, 90, 60);
        
        
        zone.spinner(536.5, 150.4, 70, 0, 40);
        zone.spinner(536.5, 150.4, 70, 60, 40);
        zone.spinner(536.5, 150.4, 70, 120, 40);
        zone.spinner(744, 200.9, 95, 0, 40);
        zone.spinner(744, 200.9, 95, 90, 40);
        zone.spinner(491, 242.1, 20.9, 0, -60);
        zone.spinner(673, 333.6, 20.9, 0, 60);
    }

    private static void barriers(Zone zone) {
        zone.barrier(187.6, 359, 245.4, 359);
        zone.barrier(280.1, 359, 337.9, 359);
        zone.barrier(280.1, 260.7, 337.9, 260.7);
        zone.barrier(371.1, 260.7, 428.9, 260.7);
        zone.barrier(371.1, 425.3, 428.9, 425.3);
        zone.barrier(462.1, 425.3, 519.9, 425.3);
        zone.barrier(462.1, 219.7, 519.9, 219.7);
        zone.barrier(553.1, 219.7, 610.9, 219.7);
        zone.barrier(553.1, 486.3, 610.9, 486.3);
        zone.barrier(644.1, 486.3, 701.9, 486.3);
        zone.barrier(644.1, 287, 701.9, 287);
        zone.barrier(786.1, 287, 843.9, 287);
        zone.barrier(842.9, 613.3, 785.1, 613.3);
        zone.barrier(842.9, 708.7, 785.1, 708.7);
        zone.barrier(262.1, 516.1, 235.4, 561.6);
        zone.barrier(342.6, 563.4, 315.9, 608.9);
        zone.barrier(503.1, 657.6, 476.4, 703.1);
        zone.barrier(583.6, 704.9, 556.9, 750.4);
        zone.barrier(663.1, 751.1, 636.4, 796.6);
        zone.barrier(743.6, 798.4, 716.9, 843.9);
    }
}
