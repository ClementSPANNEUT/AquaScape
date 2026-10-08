package zone;

import world.Plan;
import world.Tunnel;
import world.World;
import world.Zone;

/**
 * The Dash zone, built from its plan: straight corridors joined by round rooms,
 * a kaizo shortcut, and doors the player opens by holding one button and
 * touching another.
 */
public final class DashZone {

    /**
     * Where the plan of the zone lies in the world.
     */
    public static final Plan PLAN = new Plan("Dash", 51.1, 450.6, 12373.2, 8348.6, -11.939, 2 * 84 / 44.8 * 1.5);
    private static final double CORRIDOR = 49.8;
    private static final double KAIZO = 44.8;

    private DashZone() {
    }

    /**
     * Digs the corridors and rooms of the zone into the tunnel.
     *
     * @param tunnel the tunnel of the world
     */
    public static void carve(Tunnel tunnel) {
        PLAN.chamber(tunnel, 51, 419, 48);
        PLAN.corridor(tunnel, CORRIDOR, 51, 419, 214, 311, 214, 423.4);
        PLAN.corridor(tunnel, CORRIDOR, 315.5, 423.4, 315.5, 172.6);
        PLAN.corridor(tunnel, CORRIDOR, 417, 172.6, 417, 482.6);
        PLAN.corridor(tunnel, CORRIDOR, 518, 482.6, 518, 152.9);
        PLAN.corridor(tunnel, CORRIDOR, 620, 152.9, 620, 544.6);
        PLAN.corridor(tunnel, CORRIDOR, 721, 544.6, 721, 181.1);
        PLAN.corridor(tunnel, CORRIDOR, 817.5, 181.1, 817.5, 868);
        PLAN.chamber(tunnel, 264.8, 423.4, 75.6);
        PLAN.chamber(tunnel, 366.2, 172.6, 75.6);
        PLAN.chamber(tunnel, 467.5, 482.6, 75.4);
        PLAN.chamber(tunnel, 569, 152.9, 75.9);
        PLAN.chamber(tunnel, 670.5, 544.6, 75.4);
        PLAN.chamber(tunnel, 769.2, 181.1, 73.1);
        PLAN.chamber(tunnel, 817.5, 868, 55);
        PLAN.corridor(tunnel, KAIZO, 51, 419, 817.5, 868);
        PLAN.chamber(tunnel, 338, 587, 48);
    }

    /**
     * Adds the checkpoints, the light, the enemies and the rest of the zone to
     * the world.
     *
     * @param world the world to fill
     */
    public static void layOut(World world) {
        Zone zone = new Zone(world, PLAN);
        zone.checkpoint(51.1, 450.6);
        zone.checkpoint(152.7, 351.7);
        zone.checkpoint(416.5, 342.9);
        zone.checkpoint(619.5, 465.1);
        zone.checkpoint(817.4, 262);
        zone.light("Dash", 817.5, 868);
        doors(zone);
        bouncers(zone);
        speeders(zone);
        gaters(zone);
        spinners(zone);
        barriers(zone);
    }

    private static void doors(Zone zone) {
        int door;
        door = zone.door(287.9, 290, 343.1, 290);
        zone.holdButton(door, 315.5, 340);
        zone.lockButton(door, 315.5, 280);

        door = zone.door(490.4, 360, 545.6, 360);
        zone.holdButton(door, 518, 405.2);
        zone.lockButton(door, 518, 348.2);

        door = zone.door(647.6, 340, 592.4, 340);
        zone.holdButton(door, 620, 281.5);
        zone.lockButton(door, 620, 350);

        door = zone.door(693.4, 410, 748.6, 410);
        zone.holdButton(door, 721, 460);
        zone.lockButton(door, 721, 400);

        door = zone.door(845, 790, 790, 790);
        zone.holdButton(door, 817.5, 277);
        zone.lockButton(door, 817.5, 800);

        door = zone.door(251.9, 507.7, 226.6, 550.9);
        zone.holdButton(door, 225.5, 521.2);
        zone.holdButton(door, 253.1, 537.4);

        door = zone.door(505.6, 656.2, 480.2, 699.5);
        zone.holdButton(door, 479.1, 669.8);
        zone.holdButton(door, 506.7, 685.9);

        door = zone.door(680.7, 758.8, 655.4, 802.1);
        zone.holdButton(door, 654.2, 772.4);
        zone.holdButton(door, 681.8, 788.5);

        door = zone.door(114.1, 345.3, 146, 389);
        zone.holdButton(door, 96.0, 389.2);
        zone.lockButton(door, 138.5, 361.0);
    }

    private static void bouncers(Zone zone) {
        zone.bouncer(305, 464, 0.95, -0.31);
        zone.bouncer(211, 404, 0.68, 0.73);
        zone.bouncer(307, 204, -0.02, 1.00);
        zone.bouncer(323, 183, 0.46, -0.89);
        zone.bouncer(467, 540, 0.00, 1.00);
        zone.bouncer(467, 469, 0.00, -1.00);
        zone.bouncer(627, 198, 0.96, -0.29);
        zone.bouncer(525, 211, 0.69, -0.73);
        zone.bouncer(809, 229, -0.87, -0.50);
        zone.bouncer(728, 224, 0.32, -0.95);
        zone.bouncer(298, 588, -0.92, 0.38);
        zone.bouncer(450, 450, -0.92, 0.38);
        zone.bouncer(500, 450, -0.92, 0.38);
        zone.bouncer(480, 450, -0.92, 0.38);
        zone.bouncer(450, 500, -0.92, 0.38);
        zone.bouncer(500, 500, -0.92, 0.38);
        zone.bouncer(475, 500, -0.92, 0.38);
    }

    private static void speeders(Zone zone) {
        //hard zone
        zone.speeder(86.6, 457.2, 101.7, 431.3);
        zone.speeder(93.0, 461.0, 108.2, 435.1);
        zone.speeder(99.5, 464.8, 114.7, 438.9);
        zone.speeder(106.0, 468.6, 121.1, 442.7);
        zone.speeder(112.5, 472.4, 127.6, 446.5);
        zone.speeder(118.9, 476.2, 134.1, 450.3);
        zone.speeder(125.4, 479.9, 140.6, 454.1);
        zone.speeder(314.6, 547.2, 384.1, 586.7);
        zone.speeder(291.2, 584.9, 360.8, 626.5);
        zone.speeder(528.6, 682.1, 514.1, 706.9);
        zone.speeder(535.1, 685.9, 520.5, 710.7);
        zone.speeder(541.6, 689.7, 527.0, 714.5);
        zone.speeder(548.1, 693.5, 533.5, 718.3);
        zone.speeder(554.5, 697.3, 540.0, 722.1);
        zone.speeder(561.0, 701.0, 546.4, 725.9);
        zone.speeder(675.4, 768.1, 660.8, 792.9);
        zone.speeder(704.2, 785.0, 689.6, 809.8);
        zone.speeder(733.1, 801.9, 718.5, 826.7);
        zone.speeder(761.9, 818.8, 747.3, 843.6);
        zone.speeder(661.2, 764.2, 773.3, 829.9);
        zone.speeder(650.6, 782.4, 762.7, 848.1);

        //easy zone
        zone.speeder(220, 375, 305, 375);
        zone.speeder(305, 375, 305, 465);
        zone.speeder(305, 465, 220, 465);
        zone.speeder(220, 465, 220, 375);

        zone.speeder(433.9, 400, 400.1, 400);
        zone.speeder(400.1, 360, 433.9, 360);
        zone.speeder(705, 336, 705, 273);
        zone.speeder(738, 336, 738, 273);
        zone.speeder(298.6, 266, 332.4, 266);
        zone.speeder(298.6, 258, 332.4, 258);
        zone.speeder(298.6, 250, 332.4, 250);

        zone.speeder(316.2, 172.6, 366.2, 122.6);
        zone.speeder(366.2, 122.6, 416.2, 172.6);
        zone.speeder(416.2, 172.6, 366.2, 222.6);
        zone.speeder(366.2, 222.6, 316.2, 172.6);
        zone.speeder(540, 390, 496, 390);
        zone.speeder(540, 370, 496, 370);

        zone.speeder(500, 200, 640, 200);
        zone.speeder(500, 170, 640, 170);
        zone.speeder(500, 140, 640, 140);

        zone.speeder(600, 230, 640, 270);
        zone.speeder(640, 230, 600, 270);

        zone.speeder(610, 370, 610, 450);
        zone.speeder(630, 450, 630, 370);

        zone.speeder(800, 390, 800, 575);
        zone.speeder(835, 390, 835, 575);
        zone.speeder(817.5, 390, 817.5, 468);
        zone.speeder(817.5, 497, 817.5, 575);

        zone.speeder(800, 608, 835, 789);
        zone.speeder(835, 608, 800, 789);
    }

    private static void gaters(Zone zone) {
        //hard zone
        zone.gater(415.6, 607.0, 394.0, 645.8);

        zone.gater(340, 320, 290, 320);
        zone.gater(492.4, 280, 543.6, 280);
        zone.gater(645.6, 409.1, 594.4, 409.1);
        zone.gater(747.7, 800.3, 724.3, 840.2);
        zone.gater(442.6, 250, 391.4, 250);
        zone.gater(492.4, 300, 543.6, 300);
        zone.gater(492.4, 320, 543.6, 320);
        zone.gater(492.4, 260, 543.6, 260);
        zone.gater(492.4, 240, 543.6, 240);
    }

    private static void spinners(Zone zone) {
        // Hard zone
        zone.spinner(176, 492, 20, 0, 120);
        zone.spinner(176, 492, 20, 90, 120);
        zone.spinner(338, 587, 48, 0, -60);
        zone.spinner(338, 587, 48, 60, -60);
        zone.spinner(338, 587, 48, 120, -60);
        zone.spinner(428.7, 639.3, 20, 30.4, 60);
        zone.spinner(451.8, 653.6, 20, 30.4, -60);
        zone.spinner(591.2, 735.0, 20, 30.4, 60);
        zone.spinner(617.0, 749.6, 20, 30.4, -60);
        zone.spinner(591.2, 735.0, 20, 120.4, 60);
        zone.spinner(617.0, 749.6, 20, 120.4, -60);
        zone.spinner(620.0, 310, 20, 0, -60);
        zone.spinner(620.0, 310, 20, 90, -60);

        zone.spinner(817.5, 636, 22, 0, 60);
        zone.spinner(817.5, 698.5, 22, 90, -60);
        zone.spinner(817.5, 761, 22, 90, 60);



        zone.spinner(817.5, 315, 22, 0, 60);
        zone.spinner(817.5, 315, 22, 90, 60);
        zone.spinner(817.5, 360, 22, 0, -60);
        zone.spinner(817.5, 360, 22, 90, -60);


        zone.spinner(207, 325, 30, 0, 60);
        zone.spinner(207, 325, 30, 60, 60);
        zone.spinner(207, 325, 30, 120, 60);
        zone.spinner(264.8, 423.4, 76, 0, -40);
        zone.spinner(264.8, 423.4, 76, 60, -40);
        zone.spinner(264.8, 423.4, 76, 120, -40);
        zone.spinner(366.2, 172.6, 75, 0, 40);
        zone.spinner(366.2, 172.6, 75, 60, 40);
        zone.spinner(366.2, 172.6, 75, 120, 40);
        zone.spinner(467.5, 482.6, 75, 0, -40);
        zone.spinner(467.5, 482.6, 75, 60, -40);
        zone.spinner(467.5, 482.6, 75, 120, -40);
        zone.spinner(769.2, 181.1, 73, 0, 40);
        zone.spinner(769.2, 181.1, 73, 90, 40);
        zone.spinner(769.2, 144.1, 36.5, 90, -40);

        zone.spinner(721, 383.1, 23, 0, 70);
        zone.spinner(721, 383.1, 23, 90, 70);
        zone.spinner(417, 288, 22, 90, -60);
        zone.spinner(417, 288, 22, 0, -60);
        zone.spinner(417, 380, 22, 0, 60);
        zone.spinner(417, 380, 22, 90, 60);
        zone.spinner(569, 152.9, 75, 0, 40);
        zone.spinner(569, 152.9, 75, 60, 40);
        zone.spinner(569, 152.9, 75, 120, 40);
        zone.spinner(670.5, 544.6, 75, 0, -30);
        zone.spinner(632.8, 507.0, 36, 0, 70);
        zone.spinner(708.2, 507.0, 36, 0, 70);
        zone.spinner(632.8, 582.2, 36, 0, 70);
        zone.spinner(708.2, 582.2, 36, 0, 70);
        zone.spinner(670.5, 544.6, 36, 0, 70);
        zone.spinner(721, 430, 23, 0, 70);
        zone.spinner(721, 320, 10, 0, 140);
        zone.spinner(721, 287, 10, 0, -140);
    }

    private static void barriers(Zone zone) {
        zone.barrier(185.1, 350.3, 242.9, 350.3);
        zone.barrier(286.6, 350.3, 344.4, 350.3);
        zone.barrier(286.6, 245.7, 344.4, 245.7);
        zone.barrier(388.1, 245.7, 445.9, 245.7);
        zone.barrier(388.1, 409.7, 445.9, 409.7);
        zone.barrier(489.1, 409.7, 546.9, 409.7);
        zone.barrier(489.1, 226.1, 546.9, 226.1);
        zone.barrier(591.1, 226.1, 648.9, 226.1);
        zone.barrier(590, 473.2, 645.7, 473.2);
        zone.barrier(692.1, 471.7, 749.9, 471.7);
        zone.barrier(692.1, 252.4, 749.9, 252.4);
        zone.barrier(788.6, 252.4, 846.4, 252.4);
        zone.barrier(313, 541.8, 286.3, 587.3);
        zone.barrier(389.7, 586.7, 363, 632.2);
    }
}
