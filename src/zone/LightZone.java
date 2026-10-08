package zone;

import world.Plan;
import world.Tunnel;
import world.World;
import world.Zone;

/**
 * The Lumière zone, built by hand from its plan: a long route of chambers and a narrow kaizo shortcut, both
 * leading to its light.
 */
public final class LightZone {
    /** Where the plan of the zone lies in the world. */
    public static final Plan PLAN = new Plan("Lumière", 80, 431, 6617.2, 10939.5, 142.105, 2 * 84 / 26.0);

    private static final double CORRIDOR = 40;
    private static final double KAIZO = 26;
    private static final double KAIZO_X = 150;
    private static final double KAIZO_Y = 484.5;
    private static final double KAIZO_SLOPE = Math.tan(Math.toRadians(30));
    private static final double SPEEDER_MARGIN = 7;
    private static final double DIAGONAL_MARGIN = 10;

    private LightZone() {
    }

    /**
     * Digs the corridors and rooms of the zone into the tunnel.
     * @param tunnel the tunnel of the world
     */
    public static void carve(Tunnel tunnel) {
        PLAN.chamber(tunnel, 80, 431, 36);
        PLAN.corridor(tunnel, CORRIDOR, 80, 431, 44, 431);

        PLAN.corridor(tunnel, CORRIDOR, 80, 431, 242, 331, 244, 440, 262, 478);
        PLAN.chamber(tunnel, 298, 492, 38);
        PLAN.corridor(tunnel, CORRIDOR, 331.5, 495, 331.5, 270, 348, 228);
        PLAN.chamber(tunnel, 378, 212, 40);
        PLAN.corridor(tunnel, CORRIDOR, 419, 205, 419, 540, 445, 588);
        PLAN.chamber(tunnel, 468, 598, 35);
        PLAN.corridor(tunnel, CORRIDOR, 500, 592, 508.5, 560, 508.5, 175, 528, 125);
        PLAN.chamber(tunnel, 555, 108, 43);
        PLAN.corridor(tunnel, CORRIDOR, 596.5, 110, 596.5, 631, 712, 637, 748.5, 701);
        PLAN.chamber(tunnel, 748.5, 701, 45.5);
        PLAN.corridor(tunnel, CORRIDOR, 748.5, 701, 817.5, 868);

        PLAN.corridor(tunnel, KAIZO, 100, kaizoY(100), 817.5, kaizoY(817.5));
        PLAN.chamber(tunnel, 495, 700, 12);
        PLAN.chamber(tunnel, 536, 689, 12);
        PLAN.chamber(tunnel, 546, 729, 12);
        PLAN.chamber(tunnel, 648, 771, 38.5);

        PLAN.chamber(tunnel, 817.5, 868, 52);
    }

    /**
     * Adds the checkpoints, the light, the enemies and the rest of the zone to the world.
     * @param world the world to fill
     */
    public static void layOut(World world) {
        Zone zone = new Zone(world, PLAN);
        zone.checkpoint(80, 431);
        zone.checkpoint(320.8, 491.5);
        zone.checkpoint(501.7, 595.9);
        zone.checkpoint(610.8, 631.8);
        zone.light("Lumière", 818, 870);
        longRoute(zone);
        kaizo(zone);
        barriers(zone);
    }

    private static void barriers(Zone zone) {
        zone.barrier(182.7, 344.0, 184.8, 351.2, 188.5, 360.5, 192.3, 369.9, 194.4, 377.1);
        zone.barrier(223.1, 373.4, 230.4, 373.0, 240.0, 372.7, 248.3, 372.4, 255.9, 372.3);
        zone.barrier(249.6, 487.1, 251.9, 479.7, 257.2, 472.2, 264.1, 466.6, 270.5, 462.9);
        zone.barrier(307.6, 523.4, 312.4, 516.7, 314.7, 507.1, 314.8, 497.7, 313.8, 488.3, 311.8, 478.7, 310.0, 471.6);
        zone.barrier(311.9, 377.6, 318.2, 377.2, 326.2, 376.0, 334.4, 374.9, 342.6, 374.7, 349.1, 375.6);
        zone.barrier(314.0, 278.3, 320.9, 279.8, 329.1, 281.1, 337.2, 282.6, 343.9, 284.4);
        zone.barrier(338.9, 220.0, 342.0, 227.7, 347.6, 236.6, 354.6, 243.8, 361.2, 248.5);
        zone.barrier(403.6, 187.2, 407.7, 194.1, 411.1, 204.0, 412.7, 213.4, 412.7, 223.0, 410.4, 232.7, 404.7, 240.2);
        zone.barrier(402.1, 266.3, 409.6, 266.9, 419.5, 267.1, 429.4, 267.1, 437.1, 267.1);
        zone.barrier(400.9, 510.2, 408.8, 508.2, 418.1, 508.0, 427.8, 509.0, 435.6, 509.5);
        zone.barrier(428.6, 587.4, 431.6, 580.5, 437.2, 572.7, 444.3, 566.5, 450.7, 562.7);
        zone.barrier(484.5, 621.3, 486.4, 615.1, 488.9, 606.5, 490.3, 598.1, 491.2, 589.1, 490.2, 580.1, 488.4, 573.4);
        zone.barrier(490.7, 435.6, 498.2, 433.7, 507.4, 433.7, 517.3, 434.9, 524.4, 435.9);
        zone.barrier(491.2, 300.1, 498.1, 300.8, 506.6, 301.7, 515.7, 302.8, 522.7, 303.6);
        zone.barrier(509.2, 125.8, 512.7, 133.3, 518.7, 141.2, 526.2, 147.9, 532.9, 152.3);
        zone.barrier(580.5, 143.7, 583.1, 136.7, 587.0, 127.0, 590.3, 117.2, 592.5, 107.5, 592.6, 96.8, 590.1, 88.4);
        zone.barrier(579.4, 158.2, 586.3, 158.2, 595.5, 158.2, 604.7, 158.9, 611.6, 159.8);
        zone.barrier(579.5, 587.8, 587.4, 588.5, 596.5, 589.1, 605.7, 589.9, 612.8, 590.1);
        zone.barrier(623.8, 647.8, 623.4, 640.6, 623.1, 631.5, 624.0, 622.2, 625.2, 615.1);
        zone.barrier(704.2, 655.2, 707.2, 649.7, 710.9, 642.2, 714.8, 634.6, 718.8, 627.3, 722.4, 621.9);
        zone.barrier(712.2, 671.2, 716.9, 663.8, 725.8, 657.7, 735.5, 654.4, 744.0, 653.1);
        zone.barrier(753.7, 745.3, 761.0, 744.0, 769.6, 739.6, 777.3, 734.0, 783.0, 729.3);
        zone.barrier(783.0, 832.8, 788.8, 829.6, 797.0, 825.7, 804.9, 822.7, 813.7, 820.1, 820.1, 818.5);

        zone.barrier(244.7, 554.6, 247.8, 548.8, 252.4, 540.2, 256.4, 534.1);
        zone.barrier(472.2, 677.5, 475.3, 670.6, 479.7, 664.1);
        zone.barrier(607.7, 760.5, 610.9, 754.8, 615.1, 747.6, 618.6, 741.9);
        zone.barrier(674.3, 797.6, 677.3, 792.2, 681.7, 785.5, 683.7, 779.7);
        zone.barrier(702.2, 814.2, 704.5, 808.1, 707.8, 800.5, 711.7, 795.0);
    }

    private static void longRoute(Zone zone) {
        zone.bouncer(206, 351.8, 9, -5.8);
        across(zone, 242.8, 375, 1, 0);

        zone.bouncer(277.6, 485.8, 6.4, 7.2);
        zone.bouncer(298, 506, -7, 7);

        zone.bouncer(331.4, 354.1, 3.6, -9.1);
        across(zone, 331.5, 401, 1, 0);
        across(zone, 331.5, 446.5, 1, 0);

        zone.bouncer(384.4, 199.4, -7.5, 6.6);
        zone.bouncer(360.4, 218.8, 7.7, 8.5);
        zone.bouncer(375.6, 228.1, 10, -4.7);

        across(zone, 419, 278, 1, 0);
        across(zone, 419, 342, 1, 0);
        across(zone, 419, 407.5, 1, 0);
        across(zone, 419, 473, 1, 0);
        zone.bouncer(419.4, 489.9, 3.6, -8.9);

        diagonal(zone, 430.8, 569.2, 496.7, 624.2);
        zone.bouncer(452, 588.7, -0.8, -0.6);
        zone.bouncer(476.3, 608.3, -8, 7.5);

        across(zone, 508.5, 236.5, 1, 0);
        across(zone, 508.5, 283, 1, 0);
        zone.bouncer(508.6, 331.6, 3.4, -9.6);
        zone.bouncer(508.7, 403, 3.3, 9);
        across(zone, 508.5, 488, 1, 0);
        across(zone, 508.5, 534.5, 1, 0);

        diagonal(zone, 515.8, 136.7, 588.3, 80);
        zone.bouncer(544.2, 90, -3.4, 5.8);
        zone.bouncer(535, 98.3, 7.5, 7.5);
        zone.bouncer(570, 114.2, -6.7, 7.2);
        zone.bouncer(552.5, 122.5, 9, -5);

        across(zone, 596.5, 180, 1, 0);
        across(zone, 596.5, 224, 1, 0);
        zone.bouncer(596.6, 254.7, 3.4, 9.3);
        across(zone, 596.5, 283, 1, 0);
        across(zone, 596.5, 333.5, 1, 0);
        zone.bouncer(596.6, 372.8, 3.4, 9.2);
        across(zone, 596.5, 396, 1, 0);
        across(zone, 596.5, 475.5, 1, 0);
        zone.bouncer(596.7, 521, 3.3, 9);

        across(zone, 634, 632.9, 0, 1);
        across(zone, 669, 634.8, 0, 1);
        zone.bouncer(688.8, 636.3, 10.2, 3.7);

        diagonal(zone, 701.7, 708.3, 782.5, 666.7);
        diagonal(zone, 715.8, 733.3, 795, 692.5);
        zone.bouncer(735, 690.8, 8.3, 8.4);
        zone.bouncer(748.3, 686.7, 10, -5);
        zone.bouncer(767.5, 691.7, -4.2, 9.1);
        zone.bouncer(730, 712.5, 5.8, -7.8);
        zone.bouncer(762.5, 709.2, -6.7, 6.6);

        zone.bouncer(804.1, 860.5, 5.9, -9.5);
    }

    private static void kaizo(Zone zone) {
        kaizoAcross(zone, 161.1);
        kaizoAcross(zone, 186.5);
        kaizoAcross(zone, 212.1);
        kaizoAcross(zone, 237.6);

        zone.bouncer(271.8, 555.1, -5.8, 8.9);
        zone.bouncer(305.1, 572.8, 5.9, -8.8);
        zone.bouncer(338.3, 594.1, -6.3, 8.9);

        kaizoAcross(zone, 373);
        zone.bouncer(396.1, 628.1, -6.1, 8.9);
        kaizoAcross(zone, 418.9);
        zone.bouncer(443.4, 653.2, 5.6, -9.2);
        kaizoAcross(zone, 462.7);

        zone.speeder(479, kaizoY(479), 573, kaizoY(573));

        kaizoAcross(zone, 606.1);
        zone.bouncer(651.2, 764.8, -7.2, 6.2);
        zone.bouncer(629.3, 768.5, 5.7, 8.5);
        zone.bouncer(659.6, 781.6, 8.4, -5.6);

        kaizoAcross(zone, 682.7);
        kaizoAcross(zone, 723.7);
        kaizoAcross(zone, 746.8);
        zone.bouncer(768.2, 841.5, -5.2, 9.5);
        kaizoAcross(zone, 787.7);
    }

    private static double kaizoY(double x) {
        return KAIZO_Y + KAIZO_SLOPE * (x - KAIZO_X);
    }

    private static void kaizoAcross(Zone zone, double x) {
        double angle = Math.atan(KAIZO_SLOPE);
        double half = KAIZO / 2 - SPEEDER_MARGIN;
        double y = kaizoY(x);
        double acrossX = -Math.sin(angle);
        double acrossY = Math.cos(angle);
        zone.speeder(x - acrossX * half, y - acrossY * half, x + acrossX * half, y + acrossY * half);
    }

    private static void across(Zone zone, double x, double y, double acrossX, double acrossY) {
        double half = CORRIDOR / 2 - SPEEDER_MARGIN;
        zone.speeder(x - acrossX * half, y - acrossY * half, x + acrossX * half, y + acrossY * half);
    }

    private static void diagonal(Zone zone, double x1, double y1, double x2, double y2) {
        double length = Math.hypot(x2 - x1, y2 - y1);
        double pullX = (x2 - x1) / length * DIAGONAL_MARGIN;
        double pullY = (y2 - y1) / length * DIAGONAL_MARGIN;
        zone.speeder(x1 + pullX, y1 + pullY, x2 - pullX, y2 - pullY);
    }
}
