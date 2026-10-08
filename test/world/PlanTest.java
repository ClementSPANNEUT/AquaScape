package world;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlanTest {
    private static final double EPSILON = 1e-9;
    private final Plan plan = new Plan("zone", 10, 20, 1000, 2000, 90, 2);

    @Test
    void theAnchorLandsOnItsWorldPoint() {
        assertEquals(1000, plan.x(10, 20), EPSILON);
        assertEquals(2000, plan.y(10, 20), EPSILON);
        assertEquals(1000, plan.anchorX());
        assertEquals(2000, plan.anchorY());
    }

    @Test
    void pointsAreTurnedAndScaledAroundTheAnchor() {
        assertEquals(1000, plan.x(11, 20), EPSILON);
        assertEquals(2002, plan.y(11, 20), EPSILON);
        assertArrayEquals(new double[] {1000, 2002, 998, 2000}, plan.points(11, 20, 10, 21), EPSILON);
    }

    @Test
    void lengthsAreScaledAndDirectionsOnlyTurned() {
        assertEquals(30, plan.length(15), EPSILON);
        assertArrayEquals(new double[] {0, 1}, plan.direction(1, 0), EPSILON);
        assertEquals(135, plan.worldAngle(45), EPSILON);
    }

    @Test
    void itDigsInWorldCoordinates() {
        Tunnel tunnel = new Tunnel();
        plan.chamber(tunnel, 10, 20, 5);
        plan.corridor(tunnel, 4, 10, 20, 40, 20);
        assertTrue(tunnel.contains(1000, 2000 + 9));
        assertTrue(tunnel.contains(1000, 2000 + 50), "the corridor goes along the turned x axis");
    }

    @Test
    void worldPointsAreReadBackInPlanPx() {
        double worldX = plan.x(37.5, -12.25);
        double worldY = plan.y(37.5, -12.25);
        assertEquals(37.5, plan.planX(worldX, worldY), EPSILON);
        assertEquals(-12.25, plan.planY(worldX, worldY), EPSILON);
        assertEquals("zone", plan.name());
        assertEquals(2, plan.scale());
    }

    @Test
    void itCoversOnlyWhatItDug() {
        Tunnel tunnel = new Tunnel();
        plan.chamber(tunnel, 10, 20, 5);
        plan.corridor(tunnel, 4, 10, 20, 40, 20);
        assertTrue(plan.covers(1000, 2000 + 9));
        assertTrue(plan.covers(1000, 2000 + 50));
        assertFalse(plan.covers(1000 + 30, 2000 + 50));
    }

    @Test
    void anOutlineInsideAnotherOneMakesAHole() {
        Plan flat = new Plan("flat", 0, 0, 0, 0, 0, 1);
        Tunnel tunnel = new Tunnel();
        flat.outline(tunnel, new double[][] {{0, 0, 100, 0, 100, 100, 0, 100}, {40, 40, 60, 40, 60, 60, 40, 60}});
        assertTrue(tunnel.contains(20, 20));
        assertFalse(tunnel.contains(50, 50));
        assertTrue(flat.covers(20, 20));
        assertFalse(flat.covers(50, 50));
        assertFalse(flat.covers(200, 200));
    }

    @Test
    void aFlatPlanIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Plan("flat", 0, 0, 0, 0, 0, 0));
    }
}
