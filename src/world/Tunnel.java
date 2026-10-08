package world;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The tunnel a level is dug in: a union of corridors, chambers and shapes. Its outline gives the deadly walls,
 * found quickly through a {@link WallGrid}, and is drawn as neon.
 */
public class Tunnel {
    private static final double FLATNESS = 1.5;
    private static final double MIN_EDGE = 1e-6;
    private static final double SHORT_EDGE = 5;
    private static final double SIDE_PROBE = 1.5;
    private static final Color INSIDE = new Color(14, 24, 42, 225);
    private static final float[] GLOW_WIDTHS = {24f, 14f, 8f};
    private static final Color[] GLOW_COLORS = {
        new Color(60, 200, 230, 16),
        new Color(70, 210, 240, 30),
        new Color(90, 220, 245, 60),
    };
    private static final float EDGE_WIDTH = 3f;
    private static final Color EDGE_COLOR = new Color(110, 230, 250);

    private final Area area = new Area();
    private List<Wall> edges;
    private WallGrid edgeGrid;
    private List<Chain> edgeChains;
    private Path2D outlinePath;
    private final TileCache tiles = new TileCache(this::paint);

    /** Creates an empty tunnel. */
    public Tunnel() {
    }

    /**
     * Digs a corridor along a line, with round ends and joints.
     * @param width width of the corridor, in px
     * @param points x and y of each point of the line in turn, in world px
     * @return the dug part, in world px
     */
    public Shape corridor(double width, double... points) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i + 1 < points.length; i += 2) {
            path.lineTo(points[i], points[i + 1]);
        }
        BasicStroke stroke = new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        return add(new Area(stroke.createStrokedShape(path)));
    }

    /**
     * Digs a round chamber.
     * @param x centre x, in world px
     * @param y centre y, in world px
     * @param radius radius, in px
     * @return the dug part, in world px
     */
    public Shape chamber(double x, double y, double radius) {
        return add(new Area(new Ellipse2D.Double(x - radius, y - radius, 2 * radius, 2 * radius)));
    }

    /**
     * Digs any shape.
     * @param shape the shape, in world px
     * @return the dug part, in world px
     */
    public Shape shape(Shape shape) {
        return add(new Area(shape));
    }

    private Area add(Area part) {
        area.add(part);
        edges = null;
        edgeGrid = null;
        edgeChains = null;
        outlinePath = null;
        tiles.clear();
        return part;
    }

    /**
     * Tells whether a point is inside the tunnel.
     * @param x x, in world px
     * @param y y, in world px
     * @return {@code true} if the point is dug
     */
    public boolean contains(double x, double y) {
        return area.contains(x, y);
    }

    /**
     * Gives the walls of the outline. The union is made of horizontal slabs; the seams between them are inside
     * the tunnel and are left out.
     * @return the walls of the outline
     */
    public List<Wall> edges() {
        if (edges == null) {
            edges = Collections.unmodifiableList(outline());
        }
        return edges;
    }

    /**
     * Adds the outline walls near a point to a list.
     * @param x x, in world px
     * @param y y, in world px
     * @param reach distance around the point, in px
     * @param into list that receives the walls
     */
    public void collectEdges(double x, double y, double reach, List<Wall> into) {
        if (edgeGrid == null) {
            edgeGrid = new WallGrid();
            edgeGrid.addAll(edges());
        }
        edgeGrid.collect(x, y, reach, into);
    }

    private List<Wall> outline() {
        List<double[]> segments = withoutSeams(segments());
        List<Wall> walls = new ArrayList<>();
        for (double[] segment : segments) {
            if (!isInside(segment)) {
                walls.add(new Wall(segment[0], segment[1], segment[2], segment[3]));
            }
        }
        edgeChains = chain(segments);
        return walls;
    }

    private boolean isInside(double[] segment) {
        double dx = segment[2] - segment[0];
        double dy = segment[3] - segment[1];
        double length = Math.hypot(dx, dy);
        if (length > SHORT_EDGE) {
            return false;
        }
        double middleX = (segment[0] + segment[2]) / 2;
        double middleY = (segment[1] + segment[3]) / 2;
        double normalX = length == 0 ? 0 : -dy / length * SIDE_PROBE;
        double normalY = length == 0 ? SIDE_PROBE : dx / length * SIDE_PROBE;
        return area.contains(middleX + normalX, middleY + normalY) && area.contains(middleX - normalX, middleY - normalY);
    }

    private List<double[]> segments() {
        List<double[]> segments = new ArrayList<>();
        double[] coords = new double[6];
        double startX = 0;
        double startY = 0;
        double lastX = 0;
        double lastY = 0;
        for (PathIterator it = area.getPathIterator(null, FLATNESS); !it.isDone(); it.next()) {
            switch (it.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO -> {
                    startX = coords[0];
                    startY = coords[1];
                    lastX = startX;
                    lastY = startY;
                }
                case PathIterator.SEG_LINETO -> {
                    segments.add(new double[] {lastX, lastY, coords[0], coords[1]});
                    lastX = coords[0];
                    lastY = coords[1];
                }
                case PathIterator.SEG_CLOSE -> {
                    if (lastX != startX || lastY != startY) {
                        segments.add(new double[] {lastX, lastY, startX, startY});
                    }
                    lastX = startX;
                    lastY = startY;
                }
                default -> {
                }
            }
        }
        return segments;
    }

    private static List<double[]> withoutSeams(List<double[]> segments) {
        Map<Double, List<double[]>> rows = new HashMap<>();
        for (double[] segment : segments) {
            if (segment[1] == segment[3]) {
                rows.computeIfAbsent(segment[1], y -> new ArrayList<>()).add(segment);
            }
        }
        List<double[]> kept = new ArrayList<>();
        for (double[] segment : segments) {
            if (segment[1] != segment[3]) {
                kept.add(segment);
                continue;
            }
            List<double[]> pieces = List.of(segment);
            for (double[] other : rows.get(segment[1])) {
                if (Math.signum(other[2] - other[0]) != Math.signum(segment[2] - segment[0])) {
                    pieces = subtract(pieces, Math.min(other[0], other[2]), Math.max(other[0], other[2]));
                }
            }
            for (double[] piece : pieces) {
                if (Math.abs(piece[2] - piece[0]) > MIN_EDGE) {
                    kept.add(piece);
                }
            }
        }
        return kept;
    }

    private static List<double[]> subtract(List<double[]> pieces, double from, double to) {
        List<double[]> left = new ArrayList<>();
        for (double[] piece : pieces) {
            double y = piece[1];
            double low = Math.min(piece[0], piece[2]);
            double high = Math.max(piece[0], piece[2]);
            boolean rightward = piece[2] > piece[0];
            if (to <= low || from >= high) {
                left.add(piece);
                continue;
            }
            if (low < from) {
                left.add(rightward ? new double[] {low, y, from, y} : new double[] {from, y, low, y});
            }
            if (to < high) {
                left.add(rightward ? new double[] {to, y, high, y} : new double[] {high, y, to, y});
            }
        }
        return left;
    }

    private static List<Chain> chain(List<double[]> segments) {
        Map<Point2D, Deque<double[]>> byStart = new HashMap<>();
        for (double[] segment : segments) {
            byStart.computeIfAbsent(new Point2D.Double(segment[0], segment[1]), p -> new ArrayDeque<>()).add(segment);
        }
        List<Chain> chains = new ArrayList<>();
        for (double[] first : segments) {
            Deque<double[]> starting = byStart.get(new Point2D.Double(first[0], first[1]));
            if (!starting.remove(first)) {
                continue;
            }
            List<Double> points = new ArrayList<>(List.of(first[0], first[1]));
            double[] segment = first;
            boolean closed = false;
            while (true) {
                points.add(segment[2]);
                points.add(segment[3]);
                if (segment[2] == first[0] && segment[3] == first[1]) {
                    closed = true;
                    break;
                }
                Deque<double[]> next = byStart.get(new Point2D.Double(segment[2], segment[3]));
                if (next == null || next.isEmpty()) {
                    break;
                }
                segment = next.poll();
            }
            double[] array = new double[points.size()];
            for (int i = 0; i < array.length; i++) {
                array[i] = points.get(i);
            }
            chains.add(new Chain(array, closed));
        }
        return chains;
    }

    /** {@return the outline of the tunnel as a path, to fill its inside} */
    public Path2D contour() {
        if (outlinePath == null) {
            outlinePath = new Path2D.Double();
            outlinePath.append(area.getPathIterator(null, FLATNESS), false);
        }
        return new Path2D.Double(outlinePath);
    }

    /** {@return the outline walls as a path, without the inside seams, to draw them} */
    public Path2D edgeContour() {
        edges();
        Path2D.Double path = new Path2D.Double();
        for (Chain chain : edgeChains) {
            path.append(chain.toPath(), false);
        }
        return path;
    }

    /**
     * Draws the inside of the tunnel and the neon of its walls over the view. The tunnel never changes once it
     * is dug, so it is drawn from the image tiles of a {@link TileCache} instead of being painted at every frame.
     * @param g graphics drawing in the world
     * @param view the visible area, in world px
     */
    public void draw(Graphics2D g, Rectangle2D view) {
        tiles.draw(g, view);
    }

    private void paint(Graphics2D g, Rectangle2D view) {
        if (outlinePath == null) {
            contour();
        }
        edges();
        Rectangle2D reach = new Rectangle2D.Double(view.getX() - GLOW_WIDTHS[0], view.getY() - GLOW_WIDTHS[0],
                view.getWidth() + 2 * GLOW_WIDTHS[0], view.getHeight() + 2 * GLOW_WIDTHS[0]);
        Path2D.Double visible = new Path2D.Double();
        for (Chain chain : edgeChains) {
            chain.appendVisible(visible, reach);
        }
        drawNeon(g, outlinePath, visible);
    }

    /**
     * Draws a shape like a tunnel: dark inside and glowing neon outline.
     * @param g graphics drawing in the world
     * @param outlinePath the shape
     */
    public static void drawNeon(Graphics2D g, Shape outlinePath) {
        drawNeon(g, outlinePath, outlinePath);
    }

    private static void drawNeon(Graphics2D g, Shape inside, Shape outlinePath) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(INSIDE);
        g2.fill(inside);
        for (int i = 0; i < GLOW_WIDTHS.length; i++) {
            g2.setColor(GLOW_COLORS[i]);
            g2.setStroke(new BasicStroke(GLOW_WIDTHS[i], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(outlinePath);
        }
        g2.setColor(EDGE_COLOR);
        g2.setStroke(new BasicStroke(EDGE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(outlinePath);
        g2.dispose();
    }

    private record Chain(double[] points, boolean closed, Rectangle2D bounds) {
        Chain(double[] points, boolean closed) {
            this(points, closed, boundsOf(points));
        }

        private static Rectangle2D boundsOf(double[] points) {
            Path2D.Double path = new Path2D.Double();
            path.moveTo(points[0], points[1]);
            for (int i = 2; i + 1 < points.length; i += 2) {
                path.lineTo(points[i], points[i + 1]);
            }
            return path.getBounds2D();
        }

        void appendVisible(Path2D path, Rectangle2D view) {
            if (!bounds.intersects(view)) {
                return;
            }
            int segments = points.length / 2 - 1;
            int start = 0;
            if (closed) {
                start = -1;
                for (int i = 0; i < segments && start < 0; i++) {
                    if (!isVisible(i, view)) {
                        start = i;
                    }
                }
                if (start < 0) {
                    path.append(toPath(), false);
                    return;
                }
            }
            boolean drawing = false;
            for (int k = 0; k < segments; k++) {
                int i = (start + k) % segments;
                boolean visible = isVisible(i, view);
                if (visible) {
                    if (!drawing) {
                        path.moveTo(points[2 * i], points[2 * i + 1]);
                    }
                    path.lineTo(points[2 * i + 2], points[2 * i + 3]);
                }
                drawing = visible;
            }
        }

        private boolean isVisible(int segment, Rectangle2D view) {
            double x1 = points[2 * segment];
            double y1 = points[2 * segment + 1];
            double x2 = points[2 * segment + 2];
            double y2 = points[2 * segment + 3];
            return Math.max(x1, x2) >= view.getMinX() && Math.min(x1, x2) <= view.getMaxX()
                    && Math.max(y1, y2) >= view.getMinY() && Math.min(y1, y2) <= view.getMaxY();
        }

        private Path2D toPath() {
            Path2D.Double path = new Path2D.Double();
            path.moveTo(points[0], points[1]);
            for (int i = 2; i + 1 < points.length; i += 2) {
                path.lineTo(points[i], points[i + 1]);
            }
            if (closed) {
                path.closePath();
            }
            return path;
        }
    }
}
