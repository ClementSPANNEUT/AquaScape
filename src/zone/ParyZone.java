package zone;

import world.Plan;
import world.Tunnel;
import world.World;
import world.Zone;

/**
 * The Pary zone, built from its plan: straight corridors joined by round rooms, a kaizo shortcut, and doors
 * opened by buttons hidden behind narrow slits.
 */
public final class ParyZone {
    /** Where the plan of the zone lies in the world. */
    public static final Plan PLAN = new Plan("Pary", 48.5, 449.4, 9007.9, 12068.8, 92.185, 2 * 84 / 42.1 * 1.5);
    private static final double CORRIDOR = 46.8;
    private static final double KAIZO = 42.1;

    private ParyZone() {
    }

    /**
     * Digs the corridors and rooms of the zone into the tunnel.
     * @param tunnel the tunnel of the world
     */
    public static void carve(Tunnel tunnel) {
        PLAN.chamber(tunnel, 50, 419, 48);
        PLAN.corridor(tunnel, CORRIDOR, 50, 419, 211.5, 311.2, 211.5, 428.1);
        PLAN.corridor(tunnel, CORRIDOR, 308.5, 428.1, 308.5, 181.4);
        PLAN.corridor(tunnel, CORRIDOR, 398.5, 181.4, 398.5, 492.4);
        PLAN.corridor(tunnel, CORRIDOR, 489, 492.4, 489, 141.6);
        PLAN.corridor(tunnel, CORRIDOR, 581.5, 141.6, 581.5, 553.6);
        PLAN.corridor(tunnel, CORRIDOR, 671.5, 553.6, 671.5, 192.9);
        PLAN.corridor(tunnel, CORRIDOR, 814.5, 192.9, 814.5, 870);
        PLAN.chamber(tunnel, 260, 428.1, 71.9);
        PLAN.chamber(tunnel, 353.5, 181.4, 68.4);
        PLAN.chamber(tunnel, 443.8, 492.4, 68.6);
        PLAN.chamber(tunnel, 535.2, 141.6, 69.6);
        PLAN.chamber(tunnel, 626.5, 553.6, 68.4);
        PLAN.chamber(tunnel, 743, 192.9, 94.9);
        PLAN.chamber(tunnel, 813, 658, 52);
        PLAN.chamber(tunnel, 815, 870, 55);
        PLAN.corridor(tunnel, KAIZO, 50, 419, 815, 870);
        PLAN.chamber(tunnel, 274, 551, 50);
        PLAN.chamber(tunnel, 520, 696, 50);
        PLAN.chamber(tunnel, 676, 790, 50);
        PLAN.chamber(tunnel, 126.1, 312.2, 11);
        PLAN.corridor(tunnel, 3.7, 126.1, 312.2, 141.2, 334.8);
        PLAN.chamber(tunnel, 261.3, 315.2, 11);
        PLAN.corridor(tunnel, 3.7, 261.3, 315.2, 289.1, 315.2);
        PLAN.chamber(tunnel, 352.8, 282.3, 11);
        PLAN.corridor(tunnel, 3.7, 352.8, 282.3, 327.9, 282.3);
        PLAN.chamber(tunnel, 443.9, 302, 11);
        PLAN.corridor(tunnel, 3.7, 443.9, 302, 469.6, 302);
        PLAN.chamber(tunnel, 535.4, 236.9, 11);
        PLAN.corridor(tunnel, 3.7, 535.4, 236.9, 562.1, 236.9);
        PLAN.chamber(tunnel, 626.7, 282.3, 11);
        PLAN.corridor(tunnel, 3.7, 626.7, 282.3, 652.1, 282.3);
        PLAN.chamber(tunnel, 860, 743.7, 11);
        PLAN.corridor(tunnel, 3.7, 860, 743.7, 833.9, 743.7);
        PLAN.chamber(tunnel, 180.1, 544.6, 11);
        PLAN.corridor(tunnel, 3.7, 180.1, 544.6, 192.8, 523);
        PLAN.chamber(tunnel, 381, 663.9, 11);
        PLAN.corridor(tunnel, 3.7, 381, 663.9, 394.1, 641.7);
        PLAN.chamber(tunnel, 563.9, 772.6, 11);
        PLAN.corridor(tunnel, 3.7, 563.9, 772.6, 577.4, 749.7);
        PLAN.chamber(tunnel, 716.7, 863, 11);
        PLAN.corridor(tunnel, 3.7, 716.7, 863, 730.3, 839.9);
    }

    /**
     * Adds the checkpoints, the light, the enemies and the rest of the zone to the world.
     * @param world the world to fill
     */
    public static void layOut(World world) {
        Zone zone = new Zone(world, PLAN);
        zone.checkpoint(48.5, 449.4);
        zone.checkpoint(580.9, 383.4);
        zone.checkpoint(814.4, 459.7);
        zone.light("Pary", 815, 870);
        doors(zone);
        bouncers(zone);
        speeders(zone);
        gaters(zone);
        spinners(zone);
        barriers(zone);
    }

    private static void doors(Zone zone) {
        int door;
        door = zone.door(237.4, 377.4, 185.6, 377.4);
        zone.lockButton(door, 126.1, 312.2);

        door = zone.door(424.4, 254.4, 372.6, 254.4);
        zone.lockButton(door, 261.3, 315.2);

        door = zone.door(463.1, 215.5, 514.9, 215.5);
        zone.lockButton(door, 352.8, 282.3);

        door = zone.door(607.4, 280.5, 555.6, 280.5);
        zone.lockButton(door, 443.9, 302);

        door = zone.door(645.6, 332.1, 697.4, 332.1);
        zone.lockButton(door, 535.4, 236.9);

        door = zone.door(840.4, 235.4, 788.6, 235.4);
        zone.lockButton(door, 626.7, 282.3);

        door = zone.door(840.4, 793.5, 788.6, 793.5);
        zone.lockButton(door, 860, 743.7);

        door = zone.door(247.3, 508, 223.4, 548.6);
        zone.lockButton(door, 180.1, 544.6);

        door = zone.door(441.4, 622.4, 417.5, 663);
        zone.lockButton(door, 381, 663.9);

        door = zone.door(639.5, 739.2, 615.6, 779.8);
        zone.lockButton(door, 563.9, 772.6);

        door = zone.door(768.8, 815.4, 744.8, 856);
        zone.lockButton(door, 716.7, 863);
    }

    private static void bouncers(Zone zone) {
        zone.bouncer(294, 478, -0.94, -0.34);
        zone.bouncer(238, 477, 0.87, -0.49);
        zone.bouncer(314, 145, -0.94, -0.35);
        zone.bouncer(391, 145, 0.94, -0.33);
        zone.bouncer(499, 512, 0.94, -0.33);
        zone.bouncer(389, 511, -0.95, -0.32);
        zone.bouncer(475, 150, -0.88, 0.47);
        zone.bouncer(595, 149, -0.99, -0.16);
        zone.bouncer(627, 607, -0.04, 1.00);
        zone.bouncer(592, 581, -1.00, 0.04);
        zone.bouncer(685, 259, -0.42, -0.91);
        zone.bouncer(661, 176, -0.94, 0.34);
        zone.bouncer(814, 607, 0.04, -1.00);
        zone.bouncer(255, 577, 0.47, -0.88);
        zone.bouncer(475, 675, 0.99, 0.11);
        zone.bouncer(696, 814, -0.35, -0.94);
    }

    private static void speeders(Zone zone) {
        zone.speeder(830.4, 720, 798.6, 720);
        zone.speeder(830.4, 732.5, 798.6, 732.5);
        zone.speeder(414.4, 302.9, 382.6, 302.9);
        zone.speeder(597.4, 319.9, 565.6, 319.9);
        zone.speeder(292.6, 354, 324.4, 354);
        zone.speeder(292.6, 342, 324.4, 342);
        zone.speeder(175.1, 477, 161.3, 500.3);
        zone.speeder(185.4, 483.1, 171.7, 506.4);
        zone.speeder(330.2, 568.5, 316.5, 591.8);
        zone.speeder(400.4, 175.6, 383, 217);
        zone.speeder(310.6, 257.4, 326, 221);
        zone.speeder(398.2, 411, 398, 446);
        zone.speeder(672.7, 485.5, 672, 442);
        zone.speeder(814, 520, 814, 561);
        zone.speeder(554.8, 693.1, 578, 724);
        zone.speeder(292.6, 299.7, 324.4, 299.7);
        zone.speeder(473.1, 412.2, 504.9, 412.2);
        zone.speeder(597.4, 462.6, 565.6, 462.6);
        zone.speeder(830.4, 322.5, 798.6, 322.5);
        zone.speeder(113, 357.9, 130.6, 384.3);
    }

    private static void gaters(Zone zone) {
        zone.gater(605.6, 210.1, 557.4, 210.1);
        zone.gater(464.9, 368.1, 513.1, 368.1);
        zone.gater(647.4, 413, 695.6, 413);
        zone.gater(838.6, 581.8, 790.4, 581.8);
        zone.gater(374.9, 585.4, 352.9, 622.8);
        zone.gater(422.6, 356.9, 374.4, 356.9);
        zone.gater(838.6, 371, 790.4, 371);
    }

    private static void spinners(Zone zone) {
        zone.spinner(353.5, 181.4, 34.2, 37, 60);
        zone.spinner(443.8, 492.4, 34.3, 74, -60);
        zone.spinner(535.2, 141.6, 34.8, 111, 60);
        zone.spinner(626.5, 553.6, 34.2, 148, -60);
        zone.spinner(813, 658, 26, 42, 60);
        zone.spinner(520, 696, 25, 116, -60);
        zone.spinner(489, 303.1, 19.6, 0, 60);
        zone.spinner(125.6, 463.6, 17.7, 120.5, -60);
    }

    private static void barriers(Zone zone) {
        zone.barrier(184.1, 358.8, 238.9, 358.8);
        zone.barrier(281.1, 358.8, 335.9, 358.8);
        zone.barrier(281.1, 248.3, 335.9, 248.3);
        zone.barrier(371.1, 248.3, 425.9, 248.3);
        zone.barrier(371.1, 425.3, 425.9, 425.3);
        zone.barrier(461.6, 425.3, 516.4, 425.3);
        zone.barrier(461.6, 209.4, 516.4, 209.4);
        zone.barrier(554.1, 209.4, 608.9, 209.4);
        zone.barrier(554.1, 486.7, 608.9, 486.7);
        zone.barrier(644.1, 486.7, 698.9, 486.7);
        zone.barrier(644.1, 276.7, 698.9, 276.7);
        zone.barrier(787.1, 276.7, 841.9, 276.7);
        zone.barrier(840.4, 609.6, 785.6, 609.6);
        zone.barrier(840.4, 706.4, 785.6, 706.4);
        zone.barrier(245.9, 505.4, 220.5, 548.5);
        zone.barrier(327.5, 553.5, 302.1, 596.6);
        zone.barrier(491.9, 650.4, 466.5, 693.5);
        zone.barrier(573.5, 698.5, 548.1, 741.6);
        zone.barrier(647.9, 744.4, 622.5, 787.5);
        zone.barrier(729.5, 792.5, 704.1, 835.6);
    }
}
