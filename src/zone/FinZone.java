package zone;

import world.Plan;
import world.Tunnel;
import world.World;
import world.Zone;

/** The last zone, behind the golden gate: a long serpentine of corridors leading to the final light. */
public final class FinZone {
    /** Where the plan of the zone lies in the world. */
    public static final Plan PLAN = new Plan("Fin", 48.4, 545.5, 7732.0, 6128.0, -114.350, 2 * 84 / 42.6 * 1.5);
    private static final double CORRIDOR = 47.3;

    private FinZone() {
    }

    /**
     * Digs the corridors and rooms of the zone into the tunnel.
     * @param tunnel the tunnel of the world
     */
    public static void carve(Tunnel tunnel) {
        PLAN.chamber(tunnel, 50, 514.5, 48);
        PLAN.corridor(tunnel, CORRIDOR, 50, 514.5, 130, 514.5, 130, 212.2);
        PLAN.corridor(tunnel, CORRIDOR, 207, 212.2, 207, 843.1);
        PLAN.corridor(tunnel, CORRIDOR, 283.5, 843.1, 283.5, 212.2);
        PLAN.corridor(tunnel, CORRIDOR, 360.5, 212.2, 360.5, 842.8);
        PLAN.corridor(tunnel, CORRIDOR, 437.5, 842.8, 437.5, 211.9);
        PLAN.corridor(tunnel, CORRIDOR, 514, 211.9, 514, 842.8);
        PLAN.corridor(tunnel, CORRIDOR, 591, 842.8, 591, 212.2);
        PLAN.corridor(tunnel, CORRIDOR, 668, 212.2, 668, 843.1);
        PLAN.corridor(tunnel, CORRIDOR, 744.5, 843.1, 744.5, 212.2);
        PLAN.corridor(tunnel, CORRIDOR, 821.5, 212.2, 821.5, 875);
        PLAN.chamber(tunnel, 168.5, 212.2, 62.2);
        PLAN.chamber(tunnel, 245.2, 843.1, 61.9);
        PLAN.chamber(tunnel, 322, 212.2, 62.2);
        PLAN.chamber(tunnel, 399, 842.8, 62.2);
        PLAN.chamber(tunnel, 475.8, 211.9, 61.9);
        PLAN.chamber(tunnel, 552.5, 842.8, 62.2);
        PLAN.chamber(tunnel, 629.5, 212.2, 62.2);
        PLAN.chamber(tunnel, 706.2, 843.1, 61.9);
        PLAN.chamber(tunnel, 783, 212.2, 62.2);
        PLAN.chamber(tunnel, 822, 875, 57);
    }

    /**
     * Adds the checkpoints, the light, the enemies and the rest of the zone to the world.
     * @param world the world to fill
     */
    public static void layOut(World world) {
        Zone zone = new Zone(world, PLAN);
        zone.checkpoint(48.4, 545.5);
        zone.checkpoint(513.9, 514.1);
        zone.lastLight("Fin", 822, 875);
        bouncers(zone);
        speeders(zone);
        gaters(zone);
        spinners(zone);
        barriers(zone);
    }

    private static void bouncers(Zone zone) {
        zone.bouncer(167, 169, 0.51, 0.86);
        zone.bouncer(142, 208, 0.28, -0.96);
        zone.bouncer(258, 882, -0.57, -0.82);
        zone.bouncer(260, 838, 0.01, 1.00);
        zone.bouncer(349, 258, -0.86, 0.52);
        zone.bouncer(321, 169, 0.53, 0.85);
        zone.bouncer(372, 871, -0.43, -0.90);
        zone.bouncer(425, 848, 0.45, 0.89);
        zone.bouncer(489, 172, 0.42, 0.91);
        zone.bouncer(501, 206, -0.68, 0.74);
        zone.bouncer(526, 870, -0.46, -0.89);
        zone.bouncer(578, 847, 0.47, 0.89);
        zone.bouncer(650, 177, 0.57, 0.82);
        zone.bouncer(655, 206, -0.81, 0.59);
        zone.bouncer(719, 882, -0.97, -0.24);
        zone.bouncer(685, 877, -0.48, -0.88);
        zone.bouncer(782, 169, 0.56, 0.83);
        zone.bouncer(808, 181, 0.39, 0.92);
    }

    private static void speeders(Zone zone) {
        zone.speeder(113.9, 457, 146.1, 457);
        zone.speeder(113.9, 377.9, 146.1, 377.9);
        zone.speeder(113.9, 281.8, 146.1, 281.8);
        zone.speeder(530.1, 298, 497.9, 298);
        zone.speeder(837.6, 334, 805.4, 334);
        zone.speeder(837.6, 382, 805.4, 382);
        zone.speeder(837.6, 757, 805.4, 757);
        zone.speeder(837.6, 774, 805.4, 774);
        zone.speeder(376.6, 387.5, 344.4, 387.5);
        zone.speeder(376.6, 457, 344.4, 457);
        zone.speeder(376.6, 564, 344.4, 564);
        zone.speeder(223.1, 380, 190.9, 380);
        zone.speeder(223.1, 408, 190.9, 408);
        zone.speeder(223.1, 486.4, 190.9, 486.4);
        zone.speeder(223.1, 515, 190.9, 515);
        zone.speeder(223.1, 769.5, 190.9, 769.5);
        zone.speeder(684.1, 505.5, 651.9, 505.5);
        zone.speeder(267.4, 537.5, 299.6, 537.5);
        zone.speeder(728.4, 736.5, 760.6, 736.5);
        zone.speeder(129, 345, 129, 315);
        zone.speeder(678.5, 280.6, 674, 343);
        zone.speeder(502.7, 380.4, 514, 348);
        zone.speeder(446.8, 441.8, 437, 407);
        zone.speeder(744.9, 504, 745, 474);
        zone.speeder(814.4, 474.9, 816, 502);
        zone.speeder(810, 474.8, 812, 499);
        zone.speeder(834.8, 474.9, 836, 499);
        zone.speeder(578.3, 490.4, 581, 523);
        zone.speeder(603.7, 490.2, 605, 523);
        zone.speeder(744.9, 522, 745, 562);
        zone.speeder(659.3, 577.4, 662, 615);
        zone.speeder(657.1, 588.1, 659, 612);
        zone.speeder(680.7, 588.2, 682, 612);
        zone.speeder(206.9, 700, 206, 650);
        zone.speeder(359.6, 649, 360, 679);
        zone.speeder(590.4, 742, 591, 692);
        zone.speeder(512.8, 671.5, 514, 712);
        zone.speeder(223.1, 336, 190.9, 336);
        zone.speeder(267.4, 751.2, 299.6, 751.2);
        zone.speeder(267.4, 667.4, 299.6, 667.4);
        zone.speeder(267.4, 478, 299.6, 478);
        zone.speeder(267.4, 391.3, 299.6, 391.3);
        zone.speeder(267.4, 304.5, 299.6, 304.5);
        zone.speeder(376.6, 298.6, 344.4, 298.6);
        zone.speeder(376.6, 606.5, 344.4, 606.5);
        zone.speeder(421.4, 700.9, 453.6, 700.9);
        zone.speeder(421.4, 654.9, 453.6, 654.9);
        zone.speeder(421.4, 563, 453.6, 563);
        zone.speeder(530.1, 435, 497.9, 435);
        zone.speeder(530.1, 614.9, 497.9, 614.9);
        zone.speeder(574.9, 629.2, 607.1, 629.2);
        zone.speeder(574.9, 355.9, 607.1, 355.9);
        zone.speeder(574.9, 309.4, 607.1, 309.4);
        zone.speeder(684.1, 446, 651.9, 446);
        zone.speeder(728.4, 685.1, 760.6, 685.1);
        zone.speeder(728.4, 649.8, 760.6, 649.8);
        zone.speeder(728.4, 359.5, 760.6, 359.5);
        zone.speeder(837.6, 290.1, 805.4, 290.1);
        zone.speeder(837.6, 646.4, 805.4, 646.4);
    }

    private static void gaters(Zone zone) {
        zone.gater(231.3, 292, 182.7, 292);
        zone.gater(413.2, 309.7, 461.8, 309.7);
        zone.gater(845.8, 412.4, 797.2, 412.4);
        zone.gater(105.7, 416.3, 154.3, 416.3);
        zone.gater(566.7, 465, 615.3, 465);
        zone.gater(384.8, 494.4, 336.2, 494.4);
        zone.gater(413.2, 501, 461.8, 501);
        zone.gater(231.3, 555.3, 182.7, 555.3);
        zone.gater(538.3, 558.2, 489.7, 558.2);
        zone.gater(566.7, 566.5, 615.3, 566.5);
        zone.gater(259.2, 569, 307.8, 569);
        zone.gater(845.8, 583.1, 797.2, 583.1);
        zone.gater(720.2, 598.4, 768.8, 598.4);
        zone.gater(259.2, 609.5, 307.8, 609.5);
        zone.gater(692.3, 688.5, 643.7, 688.5);
        zone.gater(384.8, 711.7, 336.2, 711.7);
        zone.gater(384.8, 748.5, 336.2, 748.5);
        zone.gater(231.3, 602.6, 182.7, 602.6);
        zone.gater(259.2, 434.7, 307.8, 434.7);
        zone.gater(384.8, 335, 336.2, 335);
        zone.gater(413.2, 609, 461.8, 609);
        zone.gater(538.3, 761, 489.7, 761);
        zone.gater(692.3, 402.5, 643.7, 402.5);
        zone.gater(720.2, 408.7, 768.8, 408.7);
        zone.gater(845.8, 693.7, 797.2, 693.7);
    }

    private static void spinners(Zone zone) {
        zone.spinner(168.5, 212.2, 31.1, 0, 60);
        zone.spinner(245.2, 843.1, 31, 37, -60);
        zone.spinner(322, 212.2, 31.1, 74, 60);
        zone.spinner(399, 842.8, 31.1, 111, -60);
        zone.spinner(475.8, 211.9, 31, 148, 60);
        zone.spinner(552.5, 842.8, 31.1, 5, -60);
        zone.spinner(629.5, 212.2, 31.1, 42, 60);
        zone.spinner(706.2, 843.1, 31, 79, -60);
        zone.spinner(783, 212.2, 31.1, 116, 60);
        zone.spinner(283.5, 709.3, 19.9, 0, -60);
        zone.spinner(283.5, 347.9, 19.9, 0, 60);
        zone.spinner(437.5, 746.9, 19.9, 0, -60);
        zone.spinner(437.5, 358.4, 19.9, 0, 60);
        zone.spinner(591, 402.4, 19.9, 0, -60);
        zone.spinner(668, 749.4, 19.9, 0, 60);
        zone.spinner(744.5, 310.3, 19.9, 0, -60);
    }

    private static void barriers(Zone zone) {
        zone.barrier(102.3, 274.5, 157.7, 274.5);
        zone.barrier(179.3, 274.5, 234.7, 274.5);
        zone.barrier(179.3, 780.9, 234.7, 780.9);
        zone.barrier(255.8, 780.9, 311.2, 780.9);
        zone.barrier(255.8, 274.5, 311.2, 274.5);
        zone.barrier(332.8, 274.5, 388.2, 274.5);
        zone.barrier(332.8, 780.5, 388.2, 780.5);
        zone.barrier(409.8, 780.5, 465.2, 780.5);
        zone.barrier(409.8, 274.1, 465.2, 274.1);
        zone.barrier(486.3, 274.1, 541.7, 274.1);
        zone.barrier(486.3, 780.5, 541.7, 780.5);
        zone.barrier(563.3, 780.5, 618.7, 780.5);
        zone.barrier(563.3, 274.5, 618.7, 274.5);
        zone.barrier(640.3, 274.5, 695.7, 274.5);
        zone.barrier(640.3, 780.9, 695.7, 780.9);
        zone.barrier(716.8, 780.9, 772.2, 780.9);
        zone.barrier(716.8, 274.5, 772.2, 274.5);
        zone.barrier(793.8, 274.5, 849.2, 274.5);
    }
}
