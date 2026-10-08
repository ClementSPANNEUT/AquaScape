package world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TunnelTest {
    private static final double PROBE = 2;

    private static Tunnel sample() {
        Tunnel tunnel = new Tunnel();
        tunnel.corridor(200, 0, 500, 2000, 500);
        tunnel.chamber(1000, 500, 260);
        tunnel.corridor(120, 1000, 500, 1600, 1400, 2400, 1400);
        tunnel.chamber(2400, 1400, 150);
        return tunnel;
    }

    @Test
    void containsWhatIsDugOnly() {
        Tunnel tunnel = sample();
        assertTrue(tunnel.contains(300, 500));
        assertTrue(tunnel.contains(1000, 700));
        assertFalse(tunnel.contains(300, 700));
        assertTrue(tunnel.contains(-50, 500), "the corridor has a round end");
        assertFalse(tunnel.contains(-150, 500), "which stops at half the width");
    }

    @Test
    void everyEdgeIsARealWallWithRockOnOneSide() {
        Tunnel tunnel = sample();
        assertFalse(tunnel.edges().isEmpty());
        for (Wall edge : tunnel.edges()) {
            double x = (edge.minX() + edge.maxX()) / 2;
            double y = (edge.minY() + edge.maxY()) / 2;
            boolean rockNearby = false;
            for (int k = 0; k < 16; k++) {
                double angle = Math.PI * k / 8;
                if (!tunnel.contains(x + PROBE * Math.cos(angle), y + PROBE * Math.sin(angle))) {
                    rockNearby = true;
                }
            }
            assertTrue(rockNearby, "no wall inside the tunnel at " + x + ", " + y);
        }
    }

    @Test
    void aDropInTheMiddleTouchesNoWall() {
        Tunnel tunnel = sample();
        List<Wall> near = new ArrayList<>();
        tunnel.collectEdges(1000, 500, 40, near);
        for (Wall wall : near) {
            assertFalse(wall.touches(1000, 500, 30));
        }
    }

    @Test
    void aDropOnTheEdgeTouchesAWall() {
        Tunnel tunnel = sample();
        List<Wall> near = new ArrayList<>();
        tunnel.collectEdges(300, 400, 40, near);
        assertTrue(near.stream().anyMatch(wall -> wall.touches(300, 400, 30)));
    }

    @Test
    void itsOutlineCoversTheWholeTunnel() {
        Tunnel tunnel = sample();
        assertTrue(tunnel.contour().contains(1000, 700));
        assertFalse(tunnel.edgeContour().getBounds2D().isEmpty());
    }
}
