package fluid;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * How a drop looks, ported from the "Fluide 2D" prototype: the drop's centre plays the prototype's point, a
 * body follows it on a soft spring with some grip on the ground, fluid particles slosh around the body and a
 * thread joins the point to the body.
 * <p>The prototype's values are kept (per frame at 60 fps); its lengths are multiplied by {@code scale}, the
 * size of the drop compared to the prototype's.
 */
public class SoftBody {
    /** Radius of the prototype's drop at rest, in px: a drop this size has a scale of 1. */
    public static final double REFERENCE_RADIUS = 27.5;
    private static final double FRAME = 1.0 / 60;
    private static final int MAX_BLOBS = 256;

    /** Longest thread between the centre and the body of the prototype's drop, in px. */
    public static final double MAX_STRETCH = 200;
    private static final double BODY_SPRING = 0.005;
    private static final double BODY_DRAG = 0.95;
    private static final double SNAP = 0.1;
    private static final double THREAD_THINNING = 0.1;
    private static final double GROUND_GRIP = 0.5;
    private static final double BODY_STATIC_FRICTION = 0.20;
    private static final double BODY_KINETIC_FRICTION = 0.05;
    private static final double STATIC_FRICTION = 0.25;
    private static final double KINETIC_FRICTION = 0.06;
    private static final double EDGE_GRIP_BONUS = 0.6;

    private static final int PARTICLES = 30;
    private static final double BODY_RADIUS = 10;
    private static final double SPRING_CORE = 0.030;
    private static final double SPRING_EDGE = 0.010;
    private static final double DRAG = 0.92;
    private static final double VISCOSITY = 0.12;
    private static final double PRESSURE = 0.05;
    private static final double INTERACTION_RADIUS = 45;
    private static final double SLOSH = 0.35;
    private static final double WAVE_AMOUNT = 0.07;

    private static final double META_RADIUS = 34;
    private static final double BODY_MASS = 2.0;
    private static final double BODY_CORE_SHARE = 0.4;
    private static final double FRONT_RADIUS = 42;
    private static final double FRONT_WEIGHT = 1.2;
    private static final double THREAD_RADIUS = 30;
    private static final double THREAD_WEIGHT = 0.60;
    private static final double THREAD_SPACING = 12;
    private static final double THREAD_MIN_WEIGHT = 0.30;
    private static final double THREAD_PINCH_WEIGHT = 0.34;
    private static final double THREAD_PINCH_STRETCH = 170;
    private static final double THREAD_PINCH_SHAPE = 1;

    private static final class Particle {
        private final double homeX; 
        private final double homeY;
        private final double phase;
        private double x;
        private double y;
        private double vx;
        private double vy;

        private Particle(double homeX, double homeY, double x, double y) {
            this.homeX = homeX;
            this.homeY = homeY;
            this.phase = Math.random() * Math.PI * 2;
            this.x = x;
            this.y = y;
        }
    }

    private final List<Particle> particles = new ArrayList<>();
    private final double[] blobX = new double[MAX_BLOBS];
    private final double[] blobY = new double[MAX_BLOBS];
    private final double[] blobRadius = new double[MAX_BLOBS];
    private final double[] blobWeight = new double[MAX_BLOBS];
    private int blobCount;

    private double pointX; 
    private double pointY;
    private double pointVx; 
    private double pointVy;
    private double bodyX;
    private double bodyY;
    private double bodyVx;
    private double bodyVy;
    private double previousBodyVx;
    private double previousBodyVy;
    private double threadX; 
    private double threadY;
    private double time;
    private double pendingTime;
    private double scale;
    private boolean pinned;

    /**
     * Creates the body at rest around a point.
     * @param x x of the drop's centre, in world px
     * @param y y of the drop's centre, in world px
     * @param scale size of the drop compared to the prototype's
     */
    public SoftBody(double x, double y, double scale) {
        this.scale = scale;
        pointX = x;
        pointY = y;
        bodyX = x;
        bodyY = y;
        threadX = x;
        threadY = y;
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < PARTICLES; i++) {
            double r = BODY_RADIUS * Math.sqrt((i + 0.5) / PARTICLES);
            double a = i * golden;
            double homeX = Math.cos(a) * r;
            double homeY = Math.sin(a) * r;
            particles.add(new Particle(homeX, homeY, x + homeX * scale, y + homeY * scale));
        }
    }

    /** {@return x of the body, which trails behind the drop's centre} */
    public double bodyX() {
        return bodyX;
    }

    /** {@return y of the body, which trails behind the drop's centre} */
    public double bodyY() {
        return bodyY;
    }

    /**
     * Makes the body start out moving with the drop, for fresh fragments.
     * @param vx velocity x of the drop, in px/s
     * @param vy velocity y of the drop, in px/s
     */
    public void moveWith(double vx, double vy) {
        bodyVx = vx * FRAME;
        bodyVy = vy * FRAME;
        previousBodyVx = bodyVx;
        previousBodyVy = bodyVy;
        for (Particle p : particles) {
            p.vx = bodyVx;
            p.vy = bodyVy;
        }
    }

    /**
     * After a merge, moves the body between the two old ones; the particles then flow to it.
     * @param other body of the absorbed drop
     * @param otherShare share of the merged volume that came from the other drop
     */
    public void absorb(SoftBody other, double otherShare) {
        bodyX += (other.bodyX - bodyX) * otherShare;
        bodyY += (other.bodyY - bodyY) * otherShare;
        bodyVx += (other.bodyVx - bodyVx) * otherShare;
        bodyVy += (other.bodyVy - bodyVy) * otherShare;
    }

    /**
     * Runs the prototype's frames (1/60 s each) to catch up with the game time.
     * @param dt time since the last update, in s
     * @param x x of the drop's centre, in world px
     * @param y y of the drop's centre, in world px
     * @param vx velocity x of the drop, in px/s
     * @param vy velocity y of the drop, in px/s
     * @param scale size of the drop compared to the prototype's
     * @param width width of the world, in px
     * @param height height of the world, in px
     */
    public void update(double dt, double x, double y, double vx, double vy, double scale, int width, int height) {
        this.scale = scale;
        pointX = x;
        pointY = y;
        pointVx = vx * FRAME;
        pointVy = vy * FRAME;
        pendingTime = Math.min(pendingTime + dt, 0.1);
        while (pendingTime >= FRAME) {
            step(width, height);
            pendingTime -= FRAME;
        }
    }

    /**
     * Sticks the body to the ground where it is: it stops following the drop's centre, and the thread between
     * them stretches, until the body is {@linkplain #release released}.
     */
    public void pin() {
        pinned = true;
        bodyVx = 0;
        bodyVy = 0;
    }

    /** {@return whether the body is stuck to the ground} */
    public boolean isPinned() {
        return pinned;
    }

    /**
     * Lets the pinned body go: it snaps back toward the drop's centre like a stretched elastic.
     * @param vx velocity x of the drop, in px/s
     * @param vy velocity y of the drop, in px/s
     */
    public void release(double vx, double vy) {
        if (!pinned) {
            return;
        }
        pinned = false;
        bodyVx = vx * FRAME + (pointX - bodyX) * SNAP;
        bodyVy = vy * FRAME + (pointY - bodyY) * SNAP;
    }

    private void step(int width, int height) {
        time += 0.04;
        if (pinned) {
            bodyVx = 0;
            bodyVy = 0;
        } else {
            followPoint(width, height);
        }
        moveParticles();
        previousBodyVx = bodyVx;
        previousBodyVy = bodyVy;
        bendThread();
    }

    private void followPoint(int width, int height) {
        double dx = pointX - bodyX;
        double dy = pointY - bodyY;
        bodyVx = (bodyVx + dx * BODY_SPRING) * BODY_DRAG;
        bodyVy = (bodyVy + dy * BODY_SPRING) * BODY_DRAG;
        double bodySpeed = Math.hypot(bodyVx, bodyVy);
        if (bodySpeed < BODY_STATIC_FRICTION * GROUND_GRIP * scale) {
            bodyVx = 0;
            bodyVy = 0;
        } else {
            double slowed = Math.max(0, bodySpeed - BODY_KINETIC_FRICTION * GROUND_GRIP * scale);
            bodyVx *= slowed / bodySpeed;
            bodyVy *= slowed / bodySpeed;
        }
        bodyX += bodyVx;
        bodyY += bodyVy;

        dx = pointX - bodyX;
        dy = pointY - bodyY;
        double stretch = Math.hypot(dx, dy);
        double maxStretch = MAX_STRETCH * scale;
        if (stretch > maxStretch) {
            bodyX = pointX - dx * maxStretch / stretch;
            bodyY = pointY - dy * maxStretch / stretch;
        }
        if (width > 0 && height > 0) {
            bodyX = Math.max(0, Math.min(width, bodyX));
            bodyY = Math.max(0, Math.min(height, bodyY));
        }
    }


    private void moveParticles() {
        double accelerationX = bodyVx - previousBodyVx;
        double accelerationY = bodyVy - previousBodyVy;
        for (Particle p : particles) {
            double ratio = Math.min(Math.hypot(p.homeX, p.homeY) / BODY_RADIUS, 1.0);
            double angle = Math.atan2(p.homeY, p.homeX);
            double wobble = 1.0 + WAVE_AMOUNT * Math.sin(angle * 3 + time * 1.5 + p.phase);
            double targetX = bodyX + p.homeX * scale * wobble;
            double targetY = bodyY + p.homeY * scale * wobble;
            double k = SPRING_CORE * (1 - ratio) + SPRING_EDGE * ratio;
            p.vx += (targetX - p.x) * k - accelerationX * SLOSH * ratio;
            p.vy += (targetY - p.y) * k - accelerationY * SLOSH * ratio;
        }

        double interaction = INTERACTION_RADIUS * scale;
        for (int i = 0; i < particles.size(); i++) {
            Particle a = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                Particle b = particles.get(j);
                double dx = b.x - a.x;
                double dy = b.y - a.y;
                double d2 = dx * dx + dy * dy;
                if (d2 >= interaction * interaction || d2 < 1e-6) {
                    continue;
                }
                double d = Math.sqrt(d2);
                double w = 1.0 - d / interaction;
                double push = PRESSURE * w * w * scale;
                a.vx -= dx / d * push;
                a.vy -= dy / d * push;
                b.vx += dx / d * push;
                b.vy += dy / d * push;

                double dvx = (b.vx - a.vx) * VISCOSITY * w;
                double dvy = (b.vy - a.vy) * VISCOSITY * w;
                a.vx += dvx;
                a.vy += dvy;
                b.vx -= dvx;
                b.vy -= dvy;
            }
        }

        double groundDrag = Math.max(0.0, DRAG * (1.0 - 0.06 * GROUND_GRIP));
        double maxDistance = BODY_RADIUS * 1.6 * scale;
        for (Particle p : particles) {
            double ratio = Math.min(Math.hypot(p.homeX, p.homeY) / BODY_RADIUS, 1.0);
            double grip = GROUND_GRIP * (1.0 + EDGE_GRIP_BONUS * ratio);
            p.vx *= groundDrag;
            p.vy *= groundDrag;
            double speed = Math.hypot(p.vx, p.vy);
            if (speed < STATIC_FRICTION * grip * scale) {
                p.vx = 0;
                p.vy = 0;
            } else {
                double slowed = Math.max(0.0, speed - KINETIC_FRICTION * grip * scale);
                p.vx *= slowed / speed;
                p.vy *= slowed / speed;
            }
            p.x += p.vx;
            p.y += p.vy;

            double ex = p.x - bodyX;
            double ey = p.y - bodyY;
            double distance = Math.hypot(ex, ey);
            if (distance > maxDistance) {
                p.x = bodyX + ex * maxDistance / distance;
                p.y = bodyY + ey * maxDistance / distance;
                p.vx *= 0.5;
                p.vy *= 0.5;
            }
        }
    }

    private void bendThread() {
        double bowX = (bodyVx - pointVx) * 5;
        double bowY = (bodyVy - pointVy) * 5;
        double bow = Math.hypot(bowX, bowY);
        double maxBow = 50 * scale;
        if (bow > maxBow) {
            bowX = bowX / bow * maxBow;
            bowY = bowY / bow * maxBow;
        }
        threadX = (pointX + bodyX) / 2 + bowX;
        threadY = (pointY + bodyY) / 2 + bowY;
    }

    /**
     * Adds the metaballs to the field sampled at the centre of each cell, the grid starting at (originX,
     * originY) in the world.
     * @param field the metaball field, row by row
     * @param gridWidth width of the grid, in cells
     * @param gridHeight height of the grid, in cells
     * @param cell size of a cell, in px
     * @param originX world x of the grid's first cell
     * @param originY world y of the grid's first cell
     * @param deformation squash of the drop, without translation: every metaball is squashed like it
     * @param scale size of the drop compared to the prototype's
     */
    public void addField(float[] field, int gridWidth, int gridHeight, int cell, int originX, int originY,
            AffineTransform deformation, double scale) {
        this.scale = scale;
        AffineTransform inverse;
        try {
            inverse = deformation.createInverse();
        } catch (NoninvertibleTransformException e) {
            return;
        }
        double m00 = inverse.getScaleX();
        double m01 = inverse.getShearX();
        double m10 = inverse.getShearY();
        double m11 = inverse.getScaleY();
        double extentX = Math.hypot(deformation.getScaleX(), deformation.getShearX());
        double extentY = Math.hypot(deformation.getShearY(), deformation.getScaleY());

        buildBlobs();
        for (int i = 0; i < blobCount; i++) {
            double centerX = blobX[i] - originX; // in the grid
            double centerY = blobY[i] - originY;
            double reach = blobRadius[i];
            double reach2 = reach * reach;
            int left = Math.max(0, (int) Math.floor((centerX - reach * extentX) / cell));
            int right = Math.min(gridWidth - 1, (int) Math.floor((centerX + reach * extentX) / cell));
            int top = Math.max(0, (int) Math.floor((centerY - reach * extentY) / cell));
            int bottom = Math.min(gridHeight - 1, (int) Math.floor((centerY + reach * extentY) / cell));
            for (int gy = top; gy <= bottom; gy++) {
                double dy = (gy + 0.5) * cell - centerY;
                for (int gx = left; gx <= right; gx++) {
                    double dx = (gx + 0.5) * cell - centerX;
                    double lx = m00 * dx + m01 * dy;
                    double ly = m10 * dx + m11 * dy;
                    double q = (lx * lx + ly * ly) / reach2;
                    if (q < 1) {
                        field[gy * gridWidth + gx] += (float) (blobWeight[i] * (1 - q) * (1 - q));
                    }
                }
            }
        }
    }

    /**
     * Finds the area of the world covered by the metaballs.
     * @param deformation squash of the drop, without translation
     * @param scale size of the drop compared to the prototype's
     * @return the covered area, in world px
     */
    public Rectangle2D bounds(AffineTransform deformation, double scale) {
        this.scale = scale;
        double extentX = Math.hypot(deformation.getScaleX(), deformation.getShearX());
        double extentY = Math.hypot(deformation.getShearY(), deformation.getScaleY());
        buildBlobs();
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int i = 0; i < blobCount; i++) {
            minX = Math.min(minX, blobX[i] - blobRadius[i] * extentX);
            maxX = Math.max(maxX, blobX[i] + blobRadius[i] * extentX);
            minY = Math.min(minY, blobY[i] - blobRadius[i] * extentY);
            maxY = Math.max(maxY, blobY[i] + blobRadius[i] * extentY);
        }
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    private void buildBlobs() {
        blobCount = 0;
        double particleWeight = BODY_MASS * (1.0 - BODY_CORE_SHARE) / particles.size();
        for (Particle p : particles) {
            addBlob(p.x, p.y, META_RADIUS * scale, particleWeight);
        }
        addBlob(bodyX, bodyY, META_RADIUS * scale, BODY_MASS * BODY_CORE_SHARE);
        addBlob(pointX, pointY, FRONT_RADIUS * scale, FRONT_WEIGHT);

        double stretch = Math.hypot(bodyX - pointX, bodyY - pointY);
        if (stretch < 2 * scale) {
            return;
        }
        double thin = 1.0 - THREAD_THINNING * Math.min(stretch / (MAX_STRETCH * scale), 1.0);
        double pinch = Math.min(1.0, stretch / (THREAD_PINCH_STRETCH * scale));
        pinch = pinch * pinch * (3 - 2 * pinch);
        double spacing = Math.min(THREAD_SPACING, THREAD_RADIUS * 0.45) * scale;
        int segments = Math.max(1, (int) Math.ceil(stretch / spacing));

        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            double u = 1 - t;
            double x = u * u * pointX + 2 * u * t * threadX + t * t * bodyX;
            double y = u * u * pointY + 2 * u * t * threadY + t * t * bodyY;
            double profile = Math.pow(Math.abs(2 * t - 1), THREAD_PINCH_SHAPE);
            double middleWeight = THREAD_WEIGHT + (THREAD_PINCH_WEIGHT - THREAD_WEIGHT) * pinch;
            double weight = middleWeight + (THREAD_WEIGHT - middleWeight) * profile;
            weight = Math.max(THREAD_MIN_WEIGHT, weight * thin);
            double radius = THREAD_RADIUS * (1.0 - 0.15 * pinch * (1.0 - profile)) * scale;
            addBlob(x, y, radius, weight);
        }
    }

    private void addBlob(double x, double y, double radius, double weight) {
        if (blobCount >= MAX_BLOBS) {
            return;
        }
        blobX[blobCount] = x;
        blobY[blobCount] = y;
        blobRadius[blobCount] = radius;
        blobWeight[blobCount] = weight;
        blobCount++;
    }
}
