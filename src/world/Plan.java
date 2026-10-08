package world;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Places a drawn plan in the world: the points of the plan (px of its image) are rotated, scaled and moved to
 * their place in the world. Each zone of the map has its own plan. The plan remembers what it digs, so a point
 * of the world can be traced back to its zone and read in plan px.
 */
public final class Plan {
    private final String name;
    private final AffineTransform toWorld = new AffineTransform();
    private final AffineTransform toPlan;
    private final List<Shape> dug = new ArrayList<>();
    private final double angle;
    private final double scale;
    private final double anchorX;
    private final double anchorY;

    /**
     * Creates the plan so that a point of the plan lands on a point of the world.
     * @param name name of the zone drawn on the plan
     * @param planX x of the anchor in the plan
     * @param planY y of the anchor in the plan
     * @param worldX x of the anchor in the world
     * @param worldY y of the anchor in the world
     * @param angle rotation of the plan, in degrees
     * @param scale world px per plan px, above 0
     */
    public Plan(String name, double planX, double planY, double worldX, double worldY, double angle, double scale) {
        this.name = name;
        this.angle = Math.toRadians(angle);
        this.scale = scale;
        this.anchorX = worldX;
        this.anchorY = worldY;
        toWorld.translate(worldX, worldY);
        toWorld.rotate(this.angle);
        toWorld.scale(scale, scale);
        toWorld.translate(-planX, -planY);
        try {
            toPlan = toWorld.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("the scale of a plan must not be 0", e);
        }
    }

    /** {@return name of the zone drawn on the plan} */
    public String name() {
        return name;
    }

    /** {@return world px per plan px} */
    public double scale() {
        return scale;
    }

    /** {@return x of the anchor in the world} */
    public double anchorX() {
        return anchorX;
    }

    /** {@return y of the anchor in the world} */
    public double anchorY() {
        return anchorY;
    }

    /**
     * Converts a point of the plan to the world.
     * @param planX x in the plan
     * @param planY y in the plan
     * @return x in the world
     */
    public double x(double planX, double planY) {
        return toWorld.getScaleX() * planX + toWorld.getShearX() * planY + toWorld.getTranslateX();
    }

    /**
     * Converts a point of the plan to the world.
     * @param planX x in the plan
     * @param planY y in the plan
     * @return y in the world
     */
    public double y(double planX, double planY) {
        return toWorld.getShearY() * planX + toWorld.getScaleY() * planY + toWorld.getTranslateY();
    }

    /**
     * Converts a point of the world to the plan, to write it in the zone's code.
     * @param worldX x in the world
     * @param worldY y in the world
     * @return x in the plan
     */
    public double planX(double worldX, double worldY) {
        return toPlan.getScaleX() * worldX + toPlan.getShearX() * worldY + toPlan.getTranslateX();
    }

    /**
     * Converts a point of the world to the plan, to write it in the zone's code.
     * @param worldX x in the world
     * @param worldY y in the world
     * @return y in the plan
     */
    public double planY(double worldX, double worldY) {
        return toPlan.getShearY() * worldX + toPlan.getScaleY() * worldY + toPlan.getTranslateY();
    }

    /**
     * Converts points of the plan to the world.
     * @param plan x and y of each point in turn, in plan px
     * @return the same points, in world px
     */
    public double[] points(double... plan) {
        double[] world = new double[plan.length];
        toWorld.transform(plan, 0, world, 0, plan.length / 2);
        return world;
    }

    /**
     * Turns a direction of the plan into a direction of the world; its length is kept.
     * @param planX x of the direction
     * @param planY y of the direction
     * @return x and y of the direction in the world
     */
    public double[] direction(double planX, double planY) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new double[] {cos * planX - sin * planY, sin * planX + cos * planY};
    }

    /**
     * Converts an angle of the plan to the world.
     * @param planDegrees angle in the plan, in degrees
     * @return the angle in the world, in degrees
     */
    public double worldAngle(double planDegrees) {
        return planDegrees + Math.toDegrees(angle);
    }

    /**
     * Converts a length of the plan to the world.
     * @param planLength length in plan px
     * @return the length in world px
     */
    public double length(double planLength) {
        return planLength * scale;
    }

    /**
     * Tells whether a point of the world was dug through this plan.
     * @param worldX x in the world
     * @param worldY y in the world
     * @return {@code true} if one of the plan's corridors, chambers or shapes holds the point
     */
    public boolean covers(double worldX, double worldY) {
        for (Shape part : dug) {
            if (part.contains(worldX, worldY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Digs a corridor given in plan coordinates.
     * @param tunnel the tunnel to dig
     * @param width width, in plan px
     * @param plan x and y of each point in turn, in plan px
     */
    public void corridor(Tunnel tunnel, double width, double... plan) {
        dug.add(tunnel.corridor(length(width), points(plan)));
    }

    /**
     * Digs a round chamber given in plan coordinates.
     * @param tunnel the tunnel to dig
     * @param planX x of the centre in the plan
     * @param planY y of the centre in the plan
     * @param radius radius, in plan px
     */
    public void chamber(Tunnel tunnel, double planX, double planY, double radius) {
        dug.add(tunnel.chamber(x(planX, planY), y(planX, planY), length(radius)));
    }

    /**
     * Digs the shape enclosed by loops of plan points; a loop inside another one makes a hole.
     * @param tunnel the tunnel to dig
     * @param loops each loop as x and y of its points in turn
     */
    public void outline(Tunnel tunnel, double[][] loops) {
        Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        for (double[] loop : loops) {
            path.moveTo(loop[0], loop[1]);
            for (int i = 2; i + 1 < loop.length; i += 2) {
                path.lineTo(loop[i], loop[i + 1]);
            }
            path.closePath();
        }
        dug.add(tunnel.shape(toWorld.createTransformedShape(path)));
    }
}
