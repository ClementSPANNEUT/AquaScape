package world;

import fluid.Droplet;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * A basin to fill: the drops whose centre is inside count, as a share of the level's water. Its position and
 * size are in world px.
 */
public class Goal {
    private static final Color FRAME_COLOR = new Color(120, 200, 255, 170);
    private static final Color FILLED_COLOR = new Color(90, 220, 140);
    private static final Color WATER_COLOR = new Color(60, 150, 230, 60);
    private static final BasicStroke STROKE = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
            10f, new float[] {8f, 6f}, 0f);

    private final double requiredShare;
    private final RoundRectangle2D.Double area;
    private double share;

    /**
     * Creates a basin.
     * @param left left side, in world px
     * @param top top side, in world px
     * @param width width, in px
     * @param height height, in px
     * @param requiredShare share of the level's water it needs, from 0 to 1
     */
    public Goal(double left, double top, double width, double height, double requiredShare) {
        this.area = new RoundRectangle2D.Double(left, top, width, height, 24, 24);
        this.requiredShare = requiredShare;
    }

    /**
     * Tells whether the basin shows in an area.
     * @param view area of the world, in world px
     * @return {@code true} if it shows in that area
     */
    public boolean isVisibleIn(Rectangle2D view) {
        return view.intersects(area.getBounds2D());
    }

    /**
     * Measures how much water is inside.
     * @param droplets the drops of the world
     * @param totalVolume volume of all the level's water
     */
    public void measure(List<Droplet> droplets, double totalVolume) {
        double inside = 0;
        for (Droplet droplet : droplets) {
            if (area.contains(droplet.x(), droplet.y())) {
                inside += droplet.volume();
            }
        }
        share = inside / totalVolume;
    }

    /** {@return whether it holds enough water} */
    public boolean isFilled() {
        return share >= requiredShare - 1e-9;
    }

    /**
     * Draws the basin and its water level.
     * @param g graphics drawing in the world
     */
    public void draw(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        double level = Math.min(1, share / requiredShare);
        g2.clip(area);
        g2.setColor(WATER_COLOR);
        g2.fill(new Rectangle2D.Double(area.x, area.y + area.height * (1 - level), area.width, area.height * level));
        g2.setClip(null);

        g2.setColor(isFilled() ? FILLED_COLOR : FRAME_COLOR);
        g2.setStroke(STROKE);
        g2.draw(area);
        g2.drawString(String.format("%.0f %% / %.0f %%", share * 100, requiredShare * 100),
                (float) area.x + 10, (float) area.y + 20);
        g2.dispose();
    }
}
