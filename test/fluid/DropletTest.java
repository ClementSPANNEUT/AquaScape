package fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class DropletTest {
    private static final double FRAME = 1 / 60.0;
    private static final int BIG = 100_000;

    @Test
    void volumeIsTheCubeOfTheRadius() {
        assertEquals(27_000, new Droplet(0, 0, 30).volume(), 1e-9);
    }

    @Test
    void thrustMovesTheDropAndDragSlowsItDown() {
        Droplet drop = new Droplet(5000, 5000, 30);
        for (int i = 0; i < 30; i++) {
            drop.update(FRAME, 800, 0, BIG, BIG);
        }
        double speed = drop.speed();
        assertTrue(drop.vx() > 0 && drop.x() > 5000);
        for (int i = 0; i < 30; i++) {
            drop.update(FRAME, 0, 0, BIG, BIG);
        }
        assertTrue(drop.speed() < speed);
    }

    private static Droplet movingDrop() {
        Droplet drop = new Droplet(5000, 5000, 30);
        for (int i = 0; i < 40; i++) {
            drop.update(FRAME, 800, 0, BIG, BIG);
        }
        return drop;
    }

    @Test
    void aSlidingDropIgnoresTheThrustAndTheDrag() {
        Droplet drop = movingDrop();
        double speed = drop.vx();
        double x = drop.x();
        drop.slide();
        assertTrue(drop.isSliding());
        for (int i = 0; i < 120; i++) {
            drop.update(FRAME, -800, 800, BIG, BIG);
        }
        assertEquals(speed, drop.vx(), 1e-9, "neither slowed down nor pushed back");
        assertEquals(0, drop.vy(), 1e-9, "nor pushed sideways");
        assertEquals(x + speed * 2, drop.x(), 1e-6, "it went straight on for two seconds");
    }

    @Test
    void aDropThatStopsSlidingSlowsDownAgain() {
        Droplet drop = movingDrop();
        double speed = drop.speed();
        drop.slide();
        drop.stopSliding();
        assertFalse(drop.isSliding());
        for (int i = 0; i < 60; i++) {
            drop.update(FRAME, 0, 0, BIG, BIG);
        }
        assertTrue(drop.speed() < speed / 2);
    }

    @Test
    void stickingItsBackEndsTheSlideAndAStuckDropCannotStartOne() {
        Droplet drop = movingDrop();
        drop.slide();
        drop.anchor();
        assertFalse(drop.isSliding());
        drop.slide();
        assertFalse(drop.isSliding(), "its back is stuck to the ground");
        drop.detach();
        drop.slide();
        assertTrue(drop.isSliding());
    }

    @Test
    void hittingAnEdgeOfTheWorldEndsTheSlide() {
        Droplet drop = new Droplet(500, 500, 30);
        for (int i = 0; i < 40; i++) {
            drop.update(FRAME, 800, 0, 1000, 1000);
        }
        drop.slide();
        for (int i = 0; i < 300 && drop.isSliding(); i++) {
            drop.update(FRAME, 0, 0, 1000, 1000);
        }
        assertFalse(drop.isSliding());
        assertTrue(drop.x() <= 1000 - drop.radius() + 1e-6, "stopped by the right edge, at " + drop.x());
        assertTrue(drop.vx() <= 0, "it bounced back or stopped");
    }

    @Test
    void staysInsideTheWorld() {
        Droplet drop = new Droplet(100, 100, 30);
        for (int i = 0; i < 180; i++) {
            drop.update(FRAME, -2000, -2000, 1000, 1000);
            assertTrue(drop.x() >= 0 && drop.y() >= 0);
        }
    }

    @Test
    void mergingKeepsTheVolumeAndTheMomentum() {
        Droplet fast = new Droplet(1000, 1000, 30);
        for (int i = 0; i < 20; i++) {
            fast.update(FRAME, 1000, 0, BIG, BIG);
        }
        Droplet still = new Droplet(fast.x() + 20, fast.y(), 15);
        double volume = fast.volume() + still.volume();
        double momentum = fast.volume() * fast.vx() + still.volume() * still.vx();
        assertTrue(fast.canMergeWith(still));
        fast.absorb(still);
        assertEquals(volume, fast.volume(), 1e-6);
        assertEquals(momentum, fast.volume() * fast.vx(), 1e-6);
    }

    @Test
    void breakingUpKeepsTheVolumeAndTheMomentum() {
        Droplet drop = new Droplet(50_000, 50_000, 30);
        for (int i = 0; i < 1200 && !drop.isBreakingUp(); i++) {
            drop.update(FRAME, 4000, 0, BIG, BIG);
        }
        assertTrue(drop.isBreakingUp(), "a drop pushed fast enough breaks up");
        List<Droplet> fragments = drop.breakUp(new Random(7));
        assertTrue(fragments.size() >= 2);
        double volume = 0;
        double momentumX = 0;
        double momentumY = 0;
        for (Droplet fragment : fragments) {
            volume += fragment.volume();
            momentumX += fragment.volume() * fragment.vx();
            momentumY += fragment.volume() * fragment.vy();
        }
        assertEquals(drop.volume(), volume, 1e-6);
        assertEquals(drop.volume() * drop.vx(), momentumX, 1e-3);
        assertEquals(drop.volume() * drop.vy(), momentumY, 1e-3);
    }

    @Test
    void freshFragmentsWaitBeforeMergingAgain() {
        Droplet drop = new Droplet(50_000, 50_000, 30);
        for (int i = 0; i < 1200 && !drop.isBreakingUp(); i++) {
            drop.update(FRAME, 4000, 0, BIG, BIG);
        }
        List<Droplet> fragments = drop.breakUp(new Random(7));
        assertFalse(fragments.get(0).canMergeWith(fragments.get(1)));
    }

    @Test
    void farDropsDontMerge() {
        assertFalse(new Droplet(0, 0, 10).canMergeWith(new Droplet(100, 0, 10)));
    }

    private static void push(Droplet drop, int frames, double thrustX, double thrustY) {
        for (int i = 0; i < frames; i++) {
            drop.update(FRAME, thrustX, thrustY, BIG, BIG);
        }
    }

    @Test
    void anAnchoredDropStretchesAndItsBackPullsItBack() {
        Droplet drop = new Droplet(1000, 1000, 30);
        assertEquals(0, drop.tension(), 1e-9, "no tension without an anchor");
        drop.anchor();
        assertTrue(drop.isAnchored());
        push(drop, 40, 800, 0);
        double stretched = drop.x();
        double tension = drop.tension();
        assertTrue(stretched > 1040 && tension > 0.1 && tension < 1);
        assertEquals(1000, drop.bodyX(), 1e-9, "the back stays where it was stuck");
        push(drop, 180, 0, 0);
        assertTrue(drop.x() < stretched - 20, "without a push, the head comes back");
        assertTrue(drop.isAnchored());
    }

    @Test
    void lettingGoThrowsTheDropFromItsBackTowardItsHead() {
        Droplet drop = new Droplet(1000, 1000, 30);
        drop.anchor();
        push(drop, 50, 0, 800);
        double tension = drop.tension();
        double y = drop.y();
        drop.release();
        assertFalse(drop.isAnchored());
        assertEquals(0, drop.vx(), 1e-6);
        assertEquals(Droplet.LAUNCH_SPEED * tension, drop.vy(), 1e-6, "the more stretched, the faster");
        for (int i = 0; i < 90; i++) {
            drop.update(FRAME, 0, 0, BIG, BIG);
            assertTrue(drop.distortion() < 1, "the thrown drop holds together");
        }
        assertTrue(drop.y() > y + 100);
    }

    @Test
    void aDropHardlyStretchedIsNotThrown() {
        Droplet drop = new Droplet(1000, 1000, 30);
        drop.anchor();
        push(drop, 5, 800, 0);
        double speed = drop.speed();
        drop.release();
        assertEquals(speed, drop.speed(), 1e-9);
        drop.release();
        assertEquals(speed, drop.speed(), 1e-9, "letting go twice changes nothing");
    }

    @Test
    void aDropCanLetGoWithoutBeingThrown() {
        Droplet drop = new Droplet(1000, 1000, 30);
        drop.anchor();
        push(drop, 50, 800, 0);
        double speed = drop.speed();
        drop.detach();
        assertFalse(drop.isAnchored());
        assertEquals(speed, drop.speed(), 1e-9);
    }

    @Test
    void pulledTooFarTheDropTearsInTwoHalves() {
        Droplet drop = new Droplet(1000, 1000, 30);
        drop.anchor();
        for (int i = 0; i < 300 && !drop.isTearing(); i++) {
            drop.update(FRAME, 820, 0, BIG, BIG);
        }
        assertTrue(drop.isTearing(), "pushed long enough, the thread gives way");
        assertEquals(1, drop.tension(), 1e-3);
        List<Droplet> halves = drop.tear();
        Droplet head = halves.get(0);
        Droplet back = halves.get(1);
        assertEquals(drop.volume(), head.volume() + back.volume(), 1e-6);
        assertEquals(head.radius(), back.radius(), 1e-9);
        assertEquals(drop.x(), head.x(), 1e-9);
        assertTrue(head.vx() > 0, "the head goes on");
        assertEquals(1000, back.x(), 1e-9, "the back stays where it was stuck");
        assertEquals(0, back.speed(), 1e-9);
        assertFalse(head.isAnchored() || back.isAnchored());
        assertFalse(head.canMergeWith(back));
    }

    @Test
    void aSmallDropStaysAtTheEndOfItsThread() {
        Droplet drop = new Droplet(1000, 1000, 10);
        double thread = SoftBody.MAX_STRETCH * 10 / SoftBody.REFERENCE_RADIUS;
        drop.anchor();
        for (int i = 0; i < 300; i++) {
            drop.update(FRAME, 820, 0, BIG, BIG);
            assertFalse(drop.isTearing(), "too small to tear");
            assertTrue(drop.x() <= 1000 + thread + 1e-6);
        }
        assertEquals(1, drop.tension(), 1e-6);
    }

    @Test
    void theAnchoredBackCountsAsTheDrop() {
        Droplet drop = new Droplet(1000, 1000, 30);
        assertTrue(drop.isWithin(1040, 1000, 20));
        assertFalse(drop.isWithin(1060, 1000, 20));
        drop.anchor();
        push(drop, 50, 800, 0);
        assertTrue(drop.x() > 1060);
        assertTrue(drop.isWithin(1000, 1000, 5), "the back is still there");
        assertTrue(drop.isWithin(drop.x(), 1000, 5), "and so is the head");
        drop.detach();
        assertFalse(drop.isWithin(1000, 1000, 5), "once let go, only the head counts");
    }
}
