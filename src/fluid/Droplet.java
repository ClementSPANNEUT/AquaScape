package fluid;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The water drop: the player's bubble and every fragment it breaks into.
 * <p>Its physics is the real one at game scale (see {@link Fluid}): air drag, the TAB model (Taylor Analogy
 * Breakup) for its deformation, a breakup into fragments when it is deformed too much, coalescence when two
 * drops touch, and squashing against the edges of the world. Its look comes from a {@link SoftBody}.
 * <p>The drop can also {@linkplain #anchor() stick its back to the ground}: its head then stretches away like an
 * elastic, which throws the drop when it is {@linkplain #release() let go} and {@linkplain #tear() tears it in
 * two} when it is pulled too far. And it can {@linkplain #slide() slide}: keep the speed and the direction it
 * has, whatever pushes or slows it.
 */
public class Droplet {
    /** Pull of the stretched drop toward its anchored back when it is about to tear, in px/s². */
    public static final double ELASTIC_PULL = 600;
    /** Speed a drop is thrown at when it is let go just before it tears, in px/s. */
    public static final double LAUNCH_SPEED = 950;
    private static final double MIN_LAUNCH_TENSION = 0.1;
    private static final double TAUT_END_SPEED = 350;
    private static final double MIN_TEAR_RADIUS = 12;
    private static final double FRICTION = 1.5;
    private static final double WALL_RESTITUTION = 0.3;
    private static final double MIN_BOUNCE_SPEED = 60;
    private static final double IMPACT_DEFORMATION = 1.0;
    private static final double MAX_SQUASH = 0.6;
    private static final double SQUASH_RATE = 10; 
    private static final double MIN_SPLIT_RADIUS = 6;
    private static final double MERGE_DELAY = 0.4; 
    private static final double RIPPLE_SPEED = 3;

    private double x; 
    private double y;
    private double vx; // px/s
    private double vy;
    private double radius;


    private double distortionX;
    private double distortionY;
    private double distortionRateX;
    private double distortionRateY;
    private double squashX;
    private double squashY;
    private final SoftBody softBody;
    private double mergeDelay;
    private double ripplePhase;
    private int boundsWidth;
    private int boundsHeight;
    private boolean taut;
    private boolean sliding;

    /**
     * Creates a drop at rest.
     * @param x centre x, in world px
     * @param y centre y, in world px
     * @param radius radius, in px
     */
    public Droplet(double x, double y, double radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        softBody = new SoftBody(x, y, scale());
    }

    /** {@return the centre x, in world px} */
    public double x() {
        return x;
    }

    /** {@return the centre y, in world px} */
    public double y() {
        return y;
    }

    /** {@return the velocity along x, in px/s} */
    public double vx() {
        return vx;
    }

    /** {@return the velocity along y, in px/s} */
    public double vy() {
        return vy;
    }

    /** {@return x of the soft body, which trails behind the centre while the drop moves} */
    public double bodyX() {
        return softBody.bodyX();
    }

    /** {@return y of the soft body, which trails behind the centre while the drop moves} */
    public double bodyY() {
        return softBody.bodyY();
    }

    /** {@return the radius, in px} */
    public double radius() {
        return radius;
    }

    /** {@return a value proportional to the real volume and mass; the 4/3 pi factor cancels out everywhere} */
    public double volume() {
        return radius * radius * radius;
    }

    /** {@return the speed, in px/s} */
    public double speed() {
        return Math.hypot(vx, vy);
    }

    /** {@return the Weber number of the drop at its current speed} */
    public double weber() {
        return Fluid.weber(speed(), radius);
    }

    /** {@return how deformed the drop is, from 0 (a sphere) to 1 (breakup)} */
    public double distortion() {
        return Math.hypot(distortionX, distortionY);
    }

    /** {@return whether the drop is deformed enough to break up, and still big enough to split} */
    public boolean isBreakingUp() {
        return distortion() >= 1 && radius >= MIN_SPLIT_RADIUS;
    }

    /**
     * Tells whether two drops touch and can merge; fresh fragments wait a moment first.
     * @param other the other drop
     * @return {@code true} if they can merge now
     */
    public boolean canMergeWith(Droplet other) {
        return mergeDelay <= 0 && other.mergeDelay <= 0
                && Math.hypot(other.x - x, other.y - y) < radius + other.radius;
    }

    /**
     * Moves the drop by one frame: thrust, the pull of its anchored back, air drag, bounces on the edges of the
     * world, deformation and soft body. A drop that {@linkplain #slide() slides} ignores the thrust and the
     * drag: it goes on at the same velocity.
     * @param dt time since the last frame, in s
     * @param thrustX acceleration along x, in px/s²
     * @param thrustY acceleration along y, in px/s²
     * @param width width of the world, in px; the drop stays inside
     * @param height height of the world, in px
     */
    public void update(double dt, double thrustX, double thrustY, int width, int height) {
        boundsWidth = width;
        boundsHeight = height;
        if (isAnchored()) {
            double pull = ELASTIC_PULL / maxStretch();
            thrustX -= pull * (x - softBody.bodyX());
            thrustY -= pull * (y - softBody.bodyY());
        }
        if (sliding) {
            thrustX = 0;
            thrustY = 0;
        } else {
            double dragRate = FRICTION + 3 * Fluid.AIR_DENSITY * Fluid.DRAG_COEFFICIENT * speed()
                    / (8 * Fluid.WATER_DENSITY * radius);
            vx = (vx + thrustX * dt) / (1 + dragRate * dt);
            vy = (vy + thrustY * dt) / (1 + dragRate * dt);
        }
        x += vx * dt;
        y += vy * dt;
        if (isAnchored()) {
            stayWithinReach();
        }
        hitWalls(dt, thrustX, thrustY);
        if (taut && speed() < TAUT_END_SPEED) {
            taut = false;
        }
        if (!taut) {
            updateDistortion(dt);
        }
        softBody.update(dt, x, y, vx, vy, scale(), width, height);
        mergeDelay -= dt;
        ripplePhase += RIPPLE_SPEED * dt;
    }

    /**
     * Sticks the back of the drop to the ground where it is. Until the drop is {@linkplain #release() let go}, its
     * head moves alone and the back pulls it back like an elastic, harder the further it goes.
     */
    public void anchor() {
        if (!isAnchored()) {
            sliding = false;
            softBody.pin();
        }
    }

    /**
     * Makes the drop keep the speed and the direction it has: until it {@linkplain #stopSliding() stops
     * sliding}, neither a thrust nor the air drag changes its velocity. Sticking its back to the ground or
     * hitting an edge of the world ends the slide, and a drop whose back is stuck can't start one.
     */
    public void slide() {
        if (!isAnchored()) {
            sliding = true;
        }
    }

    /** Gives the drop back to the thrust and the air drag. */
    public void stopSliding() {
        sliding = false;
    }

    /** {@return whether the drop keeps its speed and its direction} */
    public boolean isSliding() {
        return sliding;
    }

    /** {@return whether the back of the drop is stuck to the ground} */
    public boolean isAnchored() {
        return softBody.isPinned();
    }

    /** {@return how stretched the anchored drop is, from 0 to 1 where it tears; 0 when it isn't anchored} */
    public double tension() {
        return isAnchored() ? Math.min(1, stretch() / maxStretch()) : 0;
    }

    /** {@return whether the anchored drop is stretched as far as it goes, and still big enough to tear in two} */
    public boolean isTearing() {
        return isAnchored() && radius >= MIN_TEAR_RADIUS && stretch() >= 0.999 * maxStretch();
    }

    /**
     * Lets go of the ground. The stretched drop snaps like an elastic: it is thrown from its back toward its
     * head at up to {@link #LAUNCH_SPEED}, the faster the more it was stretched, and holds together until it has
     * slowed down. A drop that was hardly stretched just goes on.
     */
    public void release() {
        if (!isAnchored()) {
            return;
        }
        double tension = tension();
        if (tension >= MIN_LAUNCH_TENSION) {
            double stretch = stretch();
            double speed = Math.max(speed(), LAUNCH_SPEED * tension);
            vx = (x - softBody.bodyX()) / stretch * speed;
            vy = (y - softBody.bodyY()) / stretch * speed;
            if (speed >= TAUT_END_SPEED) {
                taut = true;
                distortionX = 0;
                distortionY = 0;
                distortionRateX = 0;
                distortionRateY = 0;
            }
        }
        softBody.release(vx, vy);
    }

    /** Lets go of the ground without throwing the drop, e.g. when the game is paused. */
    public void detach() {
        softBody.release(vx, vy);
    }

    /**
     * The overstretched drop tears in two halves: its head, which keeps its velocity, and its back, left at rest
     * where it was stuck. Volume is conserved.
     * @return the head, then the back
     */
    public List<Droplet> tear() {
        double half = volume() / 2;
        return List.of(piece(half, x, y, vx, vy), piece(half, softBody.bodyX(), softBody.bodyY(), 0, 0));
    }

    /**
     * Tells whether the drop lies within a distance of a point: its head or, while it is anchored, its back
     * stuck to the ground.
     * @param pointX x of the point, in world px
     * @param pointY y of the point, in world px
     * @param distance distance from the edge of the drop, in px
     * @return {@code true} if the head or the anchored back is that close
     */
    public boolean isWithin(double pointX, double pointY, double distance) {
        return Math.hypot(pointX - x, pointY - y) < distance + radius
                || isAnchored() && Math.hypot(pointX - softBody.bodyX(), pointY - softBody.bodyY()) < distance + radius;
    }

    private double stretch() {
        return Math.hypot(x - softBody.bodyX(), y - softBody.bodyY());
    }

    private double maxStretch() {
        return SoftBody.MAX_STRETCH * scale();
    }

    private void stayWithinReach() {
        double dx = x - softBody.bodyX();
        double dy = y - softBody.bodyY();
        double stretch = Math.hypot(dx, dy);
        double max = maxStretch();
        if (stretch <= max) {
            return;
        }
        x = softBody.bodyX() + dx / stretch * max;
        y = softBody.bodyY() + dy / stretch * max;
        double outward = (vx * dx + vy * dy) / stretch;
        if (outward > 0 && radius < MIN_TEAR_RADIUS) {
            vx -= outward * dx / stretch;
            vy -= outward * dy / stretch;
        }
    }

    private double scale() {
        return radius / SoftBody.REFERENCE_RADIUS;
    }

    private double surfaceStiffness() {
        return 8 * Fluid.SURFACE_TENSION / (Fluid.WATER_DENSITY * radius * radius * radius);
    }


    private void updateDistortion(double dt) {
        double speed = speed();
        double force = 2.0 / 3 * Fluid.AIR_DENSITY / Fluid.WATER_DENSITY * speed * speed / (radius * radius);
        double angle = 2 * Math.atan2(vy, vx);
        double forceX = force * Math.cos(angle);
        double forceY = force * Math.sin(angle);
        double stiffness = surfaceStiffness();
        double damping = 5 * Fluid.WATER_VISCOSITY / (Fluid.WATER_DENSITY * radius * radius);

        int steps = (int) Math.max(1, Math.ceil(Math.max(Math.sqrt(stiffness), damping) * dt / 0.3));
        double h = dt / steps;
        for (int i = 0; i < steps; i++) {
            distortionRateX += (forceX - stiffness * distortionX - damping * distortionRateX) * h;
            distortionRateY += (forceY - stiffness * distortionY - damping * distortionRateY) * h;
            distortionX += distortionRateX * h;
            distortionY += distortionRateY * h;
        }
    }


    private void hitWalls(double dt, double thrustX, double thrustY) {
        boolean leftSide = x < boundsWidth / 2.0;
        boolean topSide = y < boundsHeight / 2.0;
        boolean touchesX = leftSide ? x <= radius : x >= boundsWidth - radius;
        boolean touchesY = topSide ? y <= radius : y >= boundsHeight - radius;
        double pushX = touchesX ? Math.max(0, leftSide ? -thrustX : thrustX) : 0;
        double pushY = touchesY ? Math.max(0, topSide ? -thrustY : thrustY) : 0;
        squashX += (squashFor(pushX) - squashX) * Math.min(1, SQUASH_RATE * dt);
        squashY += (squashFor(pushY) - squashY) * Math.min(1, SQUASH_RATE * dt);

        double left = radius - (leftSide ? squashX : 0);
        double right = boundsWidth - radius + (leftSide ? 0 : squashX);
        double top = radius - (topSide ? squashY : 0);
        double bottom = boundsHeight - radius + (topSide ? 0 : squashY);

        double impactX = 0;
        double impactY = 0;
        if (x <= left && vx < 0 || x >= right && vx > 0) {
            impactX = vx;
            vx = bounce(vx);
        }
        if (y <= top && vy < 0 || y >= bottom && vy > 0) {
            impactY = vy;
            vy = bounce(vy);
        }
        if (impactX != 0 || impactY != 0) {
            flatten(impactX, impactY);
            sliding = false;
        }
        x = clamp(x, left, right);
        y = clamp(y, top, bottom);
        if (pushX > 0) {
            x = leftSide ? left : right;
        }
        if (pushY > 0) {
            y = topSide ? top : bottom;
        }
    }


    private double squashFor(double push) {
        if (push <= 0) {
            return 0;
        }
        double elastic = push / surfaceStiffness();
        double capillaryLength = Math.sqrt(Fluid.SURFACE_TENSION / (Fluid.WATER_DENSITY * push));
        double puddle = 2 * (radius - capillaryLength);
        return Math.min(MAX_SQUASH * radius, Math.max(elastic, puddle));
    }

    private static double bounce(double normalSpeed) {
        return Math.abs(normalSpeed) < MIN_BOUNCE_SPEED ? 0 : -normalSpeed * WALL_RESTITUTION;
    }

    private void flatten(double impactVx, double impactVy) {
        double kick = IMPACT_DEFORMATION * Math.hypot(impactVx, impactVy) / radius;
        double angle = 2 * Math.atan2(impactVy, impactVx);
        distortionRateX += kick * Math.cos(angle);
        distortionRateY += kick * Math.sin(angle);
    }

    /**
     * The flattened drop tears apart into two main fragments and a few satellite droplets, thrown sideways at
     * the speed its rim was spreading. Volume and momentum are conserved.
     * @param random source of the random sizes and directions
     * @return the fragments that replace this drop
     */
    public List<Droplet> breakUp(Random random) {
        double distortion = distortion();
        double flatAxis = Math.atan2(distortionY, distortionX) / 2;
        double sideX = -Math.sin(flatAxis);
        double sideY = Math.cos(flatAxis);
        double growthRate = (distortionX * distortionRateX + distortionY * distortionRateY) / distortion;
        double spread = 40 + 0.5 * radius * Math.max(0, growthRate);

        int satellites = radius < 12 ? 0 : 1 + random.nextInt(3);
        double satelliteVolume = volume() * (0.02 + 0.02 * random.nextDouble());
        double mainVolume = volume() - satellites * satelliteVolume;
        double volumeA = mainVolume * (0.35 + 0.3 * random.nextDouble());
        double volumeB = mainVolume - volumeA;
        double gap = Math.cbrt(volumeA) + Math.cbrt(volumeB);

        List<Droplet> fragments = new ArrayList<>();
        fragments.add(fragment(volumeA, sideX, sideY,
                gap * volumeB / mainVolume, 2 * spread * volumeB / mainVolume));
        fragments.add(fragment(volumeB, sideX, sideY,
                -gap * volumeA / mainVolume, -2 * spread * volumeA / mainVolume));
        for (int i = 0; i < satellites; i++) {
            double angle = 2 * Math.PI * random.nextDouble();
            fragments.add(fragment(satelliteVolume, Math.cos(angle), Math.sin(angle),
                    0, spread * (0.5 + random.nextDouble())));
        }

        double extraX = 0;
        double extraY = 0;
        for (Droplet fragment : fragments) {
            extraX += fragment.volume() * (fragment.vx - vx);
            extraY += fragment.volume() * (fragment.vy - vy);
        }
        for (Droplet fragment : fragments) {
            fragment.vx -= extraX / volume();
            fragment.vy -= extraY / volume();
            fragment.settleInAirflow();
        }
        return fragments;
    }

    private Droplet fragment(double fragmentVolume, double dirX, double dirY, double offset, double speed) {
        return piece(fragmentVolume, x + dirX * offset, y + dirY * offset, vx + dirX * speed, vy + dirY * speed);
    }

    private Droplet piece(double pieceVolume, double pieceX, double pieceY, double pieceVx, double pieceVy) {
        Droplet fragment = new Droplet(pieceX, pieceY, Math.cbrt(pieceVolume));
        fragment.vx = pieceVx;
        fragment.vy = pieceVy;
        fragment.softBody.moveWith(fragment.vx, fragment.vy);
        fragment.mergeDelay = MERGE_DELAY;
        fragment.ripplePhase = ripplePhase;
        fragment.boundsWidth = boundsWidth;
        fragment.boundsHeight = boundsHeight;
        return fragment;
    }


    private void settleInAirflow() {
        double[] equilibrium = airflowDistortion();
        distortionX = equilibrium[0];
        distortionY = equilibrium[1];
    }

    private double[] airflowDistortion() {
        double equilibrium = Math.min(0.9, weber() / 24);
        double flow = 2 * Math.atan2(vy, vx);
        return new double[] {equilibrium * Math.cos(flow), equilibrium * Math.sin(flow)};
    }

    /**
     * Coalescence: volumes add up and momentum is conserved. The new drop starts stretched along the line
     * between the two centres, so it wobbles like a real merged drop.
     * @param other the drop that disappears into this one
     */
    public void absorb(Droplet other) {
        double ownVolume = volume();
        double otherVolume = other.volume();
        double total = ownVolume + otherVolume;
        double line = 2 * Math.atan2(other.y - y, other.x - x);
        double stretch = 0.8 * Math.min(ownVolume, otherVolume) / total;

        x = (x * ownVolume + other.x * otherVolume) / total;
        y = (y * ownVolume + other.y * otherVolume) / total;
        vx = (vx * ownVolume + other.vx * otherVolume) / total;
        vy = (vy * ownVolume + other.vy * otherVolume) / total;
        distortionX = (distortionX * ownVolume + other.distortionX * otherVolume) / total
                - stretch * Math.cos(line);
        distortionY = (distortionY * ownVolume + other.distortionY * otherVolume) / total
                - stretch * Math.sin(line);
        distortionRateX = (distortionRateX * ownVolume + other.distortionRateX * otherVolume) / total;
        distortionRateY = (distortionRateY * ownVolume + other.distortionRateY * otherVolume) / total;
        softBody.absorb(other.softBody, otherVolume / total);
        radius = Math.cbrt(total);
    }

    /**
     * Adds this drop to the metaball field sampled at the centre of each cell, the grid starting at (originX,
     * originY) in the world: its soft body, squashed like the drop.
     * @param field the metaball field, row by row
     * @param gridWidth width of the grid, in cells
     * @param gridHeight height of the grid, in cells
     * @param cell size of a cell, in px
     * @param originX world x of the grid's first cell
     * @param originY world y of the grid's first cell
     */
    public void addField(float[] field, int gridWidth, int gridHeight, int cell, int originX, int originY) {
        softBody.addField(field, gridWidth, gridHeight, cell, originX, originY, deformation(), scale());
    }

    /** {@return the area of the world where this drop adds to the metaball field} */
    public Rectangle2D fieldBounds() {
        return softBody.bounds(deformation(), scale());
    }

    private AffineTransform deformation() {
        AffineTransform deformation = shapeTransform();
        deformation.preConcatenate(AffineTransform.getTranslateInstance(-x, -y));
        return deformation;
    }


    private AffineTransform shapeTransform() {
        double[] airflow = airflowDistortion();
        double shownX = distortionX - airflow[0];
        double shownY = distortionY - airflow[1];
        double flatAxis = Math.atan2(shownY, shownX) / 2;
        double widen = 1 + 0.5 * Math.min(Math.hypot(shownX, shownY), 1.2);
        AffineTransform shape = new AffineTransform();
        shape.translate(x, y);
        shape.rotate(flatAxis);
        shape.scale(1 / widen, widen);
        shape.rotate(-flatAxis);
        double spread = spreadAgainstWalls(shape);
        shape.scale(spread, spread);
        return shape;
    }

    private double ripple(double angle) {
        return 0.02 * Math.sin(3 * angle + ripplePhase) + 0.012 * Math.sin(4 * angle - 1.4 * ripplePhase);
    }


    private double[] outline(AffineTransform shape, double scale, boolean pressAgainstWalls) {
        int points = (int) clamp(2 * radius, 16, 64);
        double[] coords = new double[2 * points];
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double r = radius * scale * (1 + ripple(angle));
            coords[2 * i] = r * Math.cos(angle);
            coords[2 * i + 1] = r * Math.sin(angle);
        }
        shape.transform(coords, 0, coords, 0, points);
        if (pressAgainstWalls && boundsWidth > 0 && boundsHeight > 0) {
            for (int i = 0; i < points; i++) {
                coords[2 * i] = clamp(coords[2 * i], 0, boundsWidth);
                coords[2 * i + 1] = clamp(coords[2 * i + 1], 0, boundsHeight);
            }
        }
        return coords;
    }

    private double spreadAgainstWalls(AffineTransform shape) {
        double freeArea = area(outline(shape, 1, false));
        double scale = 1;
        for (int i = 0; i < 4; i++) {
            double area = area(outline(shape, scale, true));
            if (area <= 0 || area >= 0.995 * freeArea) {
                break;
            }
            scale *= Math.sqrt(freeArea / area);
        }
        return scale;
    }

    private static double area(double[] coords) {
        double sum = 0;
        for (int i = 0; i < coords.length; i += 2) {
            int j = (i + 2) % coords.length;
            sum += coords[i] * coords[j + 1] - coords[j] * coords[i + 1];
        }
        return Math.abs(sum) / 2;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
