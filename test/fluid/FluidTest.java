package fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FluidTest {
    @Test
    void aStillDropHasNoWeberNumber() {
        assertEquals(0, Fluid.weber(0, 30));
    }

    @Test
    void weberGrowsWithTheSquareOfTheSpeedAndWithTheSize() {
        double base = Fluid.weber(200, 30);
        assertEquals(4 * base, Fluid.weber(400, 30), 1e-12);
        assertEquals(2 * base, Fluid.weber(200, 60), 1e-12);
    }
}
