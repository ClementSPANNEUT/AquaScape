package fluid;

/**
 * Physical constants, in game units (pixels, seconds, water density = 1).
 * <p>Real values can't be used as they are: if the ball were a real 2 cm drop, it would only break up around 6
 * m/s, i.e. about 18 000 px/s on screen. The constants are scaled so it breaks up around 400-500 px/s; the
 * formulas that use them are the real ones.
 */
public final class Fluid {
    /** Density of water, the unit of mass of the game. */
    public static final double WATER_DENSITY = 1.0;
    /** Density of the air the drop flies through. */
    public static final double AIR_DENSITY = 0.7;
    /** Surface tension of water, which holds the drop together. */
    public static final double SURFACE_TENSION = 4.0e5;
    /** Viscosity of water, which damps the drop's wobbles. */
    public static final double WATER_VISCOSITY = 800;
    /** Drag coefficient of a sphere. */
    public static final double DRAG_COEFFICIENT = 0.47; // sphere

    private Fluid() {
    }

    /**
     * Weber number: air pressure on the drop versus the surface tension holding it together.
     * @param speed speed of the drop, in px/s
     * @param radius radius of the drop, in px
     * @return the Weber number
     */
    public static double weber(double speed, double radius) {
        return AIR_DENSITY * speed * speed * 2 * radius / SURFACE_TENSION;
    }
}
