package world;

import enemy.Arena;
import enemy.Enemy;
import enemy.ability.Ability;
import enemy.ability.Slide;
import enemy.ability.Dash;
import enemy.ability.Shoot;
import enemy.ability.Shot;
import fluid.Droplet;
import fluid.FluidRenderer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The world of a level and everything in it: the tunnel, the drops, the enemies, the checkpoints, the lights,
 * the doors and their buttons. It runs the rules of the game each frame and implements {@link Arena} so the
 * enemies can move in it and the balls of the player can fly in it. The abilities of the player's drop are the
 * {@link Dash} and {@link Shoot} objects it holds.
 * <p>A level fills it through its {@link Level#layout() layout}. The player's {@link Progress} outlives it: the
 * world is rebuilt at each death.
 */
public class World implements Arena {
    /** Radius of the player's drop at the start, in px. */
    public static final double START_RADIUS = 30;
    private static final double TOTAL_VOLUME = START_RADIUS * START_RADIUS * START_RADIUS;
    private static final double THRUST = 820;
    private static final int MAX_DROPLETS = 150;
    private static final double WIN_HOLD_TIME = 1.0;
    private static final double DEATH_FLASH_TIME = 0.6; // s
    private static final BasicStroke DEATH_STROKE = new BasicStroke(3f);
    private static final double ENEMY_RANGE = 4_000;
    private static final int MAX_SHOTS = 24;
    private static final Color EXPLOSION_COLOR = new Color(255, 150, 70);

    private final Level level;
    private final Progress progress;
    private final List<Droplet> droplets = new ArrayList<>();
    private final List<Wall> walls = new ArrayList<>();
    private final WallGrid barrierGrid = new WallGrid();
    private final List<Goal> goals = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Checkpoint> checkpoints = new ArrayList<>();
    private final List<Light> lights = new ArrayList<>();
    private final List<Door> doors = new ArrayList<>();
    private final List<DoorButton> buttons = new ArrayList<>();
    private final List<Plan> plans = new ArrayList<>();
    private final Plan worldPlan;
    private final Random random = new Random();
    private final FluidRenderer renderer = new FluidRenderer();
    private final int width; // px
    private final int height;
    private Tunnel tunnel;
    private Droplet player;
    private double filledTime;
    private double time;
    private boolean playerDead;
    private double deathTime;
    private boolean lastLightReached;
    private String newLight;
    private int hub = -1;
    private String area = "";
    private boolean invincible;
    private boolean playerTouching;
    private final Dash dash = new Dash();
    private final Slide slide = new Slide();
    private final Shoot shoot = new Shoot();
    private final List<Shot> shots = new ArrayList<>();
    private boolean exploded;

    /**
     * Creates the world of a level, with no progress yet.
     * @param level the level to lay out
     */
    public World(Level level) {
        this(level, new Progress());
    }

    /**
     * Creates the world of a level with the player's progress. If the layout adds no drop, the player starts on
     * the current checkpoint of the progress.
     * @param level the level to lay out
     * @param progress checkpoints, lights and doors already obtained; the world updates it
     */
    public World(Level level, Progress progress) {
        this.level = level;
        this.progress = progress;
        this.width = level.width();
        this.height = level.height();
        this.worldPlan = new Plan(level.name(), 0, 0, 0, 0, 0, 1);
        level.layout().accept(this);
        for (int i = 0; i < checkpoints.size(); i++) {
            if (progress.hasReached(i)) {
                checkpoints.get(i).reach();
            }
        }
        if (player == null && !checkpoints.isEmpty()) {
            int index = Math.max(0, Math.min(progress.checkpoint(), checkpoints.size() - 1));
            Checkpoint start = checkpoints.get(index);
            start.reach();
            progress.touchCheckpoint(index);
            addDroplet(start.x(), start.y(), 1);
        }
        updateDoors();
    }

    /**
     * Adds a drop; the first one added is the player.
     * @param x centre x, in world px
     * @param y centre y, in world px
     * @param waterShare share of the level's water it holds, from 0 to 1
     */
    public void addDroplet(double x, double y, double waterShare) {
        Droplet droplet = new Droplet(x, y, START_RADIUS * Math.cbrt(waterShare));
        droplets.add(droplet);
        if (player == null) {
            player = droplet;
        }
    }

    /**
     * Adds a deadly neon wall.
     * @param x1 x of the first end, in world px
     * @param y1 y of the first end, in world px
     * @param x2 x of the second end, in world px
     * @param y2 y of the second end, in world px
     */
    public void addWall(double x1, double y1, double x2, double y2) {
        walls.add(new Wall(x1, y1, x2, y2));
    }

    /**
     * Adds an invisible barrier that stops the bouncers, as a line through points.
     * @param points x and y of each point in turn, in world px
     */
    public void addBarrier(double... points) {
        for (int i = 0; i + 3 < points.length; i += 2) {
            barrierGrid.add(new Wall(points[i], points[i + 1], points[i + 2], points[i + 3]));
        }
    }

    /**
     * Sets the tunnel the level is dug in; its edges are deadly walls.
     * @param tunnel the tunnel
     */
    public void setTunnel(Tunnel tunnel) {
        this.tunnel = tunnel;
    }

    /**
     * Names the area of the checkpoints added next, for the checkpoint map.
     * @param area name of the area, such as "Split"
     */
    public void setArea(String area) {
        this.area = area;
    }

    /**
     * Adds a checkpoint to the current area.
     * @param x centre x, in world px
     * @param y centre y, in world px
     * @return the index of the checkpoint
     */
    public int addCheckpoint(double x, double y) {
        checkpoints.add(new Checkpoint(x, y, area));
        return checkpoints.size() - 1;
    }

    /** {@return the checkpoints, in the order they were added} */
    public List<Checkpoint> checkpoints() {
        return Collections.unmodifiableList(checkpoints);
    }

    /** {@return the lights of the level} */
    public List<Light> lights() {
        return Collections.unmodifiableList(lights);
    }

    /** {@return the tunnel of the level, or {@code null} for an open room} */
    public Tunnel tunnel() {
        return tunnel;
    }

    /**
     * Records the plan of a zone, so the coordinate grid can show its plan px. The first plan recorded is the
     * overview of the level.
     * @param plan the plan
     */
    public void addPlan(Plan plan) {
        plans.add(plan);
    }

    /**
     * Gives the plan to read a point in: the plan of the zone dug there, else the overview, else the world
     * itself when the level has no plan.
     * @param x x, in world px
     * @param y y, in world px
     * @return the plan of the point
     */
    public Plan planAt(double x, double y) {
        for (int i = 1; i < plans.size(); i++) {
            if (plans.get(i).covers(x, y)) {
                return plans.get(i);
            }
        }
        return plans.isEmpty() ? worldPlan : plans.get(0);
    }

    /**
     * Chooses the checkpoint the player goes back to after taking a light.
     * @param checkpoint index of the hub checkpoint
     */
    public void setHub(int checkpoint) {
        hub = checkpoint;
    }

    /**
     * Adds a light to reach.
     * @param light the light
     */
    public void addLight(Light light) {
        lights.add(light);
    }

    /**
     * Adds a door. It opens when a button or the progress says so.
     * @param door the door
     * @return the index of the door, used by its buttons
     */
    public int addDoor(Door door) {
        doors.add(door);
        return doors.size() - 1;
    }

    /**
     * Adds a button that opens a door.
     * @param button the button
     */
    public void addButton(DoorButton button) {
        buttons.add(button);
    }

    /**
     * Turns the invincible mode on or off. In this mode the player's drop can't die: a wall, an enemy, a closed
     * door or a shot charged too long only counts as a hit, once per contact. The other drops still die.
     * @param invincible whether the player's drop can't die
     */
    public void setInvincible(boolean invincible) {
        this.invincible = invincible;
    }

    /** {@return whether the player's drop can't die} */
    public boolean isInvincible() {
        return invincible;
    }

    /** {@return the player's progress in this world} */
    public Progress progress() {
        return progress;
    }

    /** {@return how many lights must be brought back, not counting the last one} */
    public int lightCount() {
        int count = 0;
        for (Light light : lights) {
            if (!light.isLast()) {
                count++;
            }
        }
        return count;
    }

    /** {@return the names of the lights already obtained, not counting the last one} */
    public List<String> obtainedLights() {
        List<String> names = new ArrayList<>();
        for (Light light : lights) {
            if (!light.isLast() && progress.hasLight(light.name())) {
                names.add(light.name());
            }
        }
        return names;
    }

    /**
     * Gives the light the player has just taken, once: the game then sends the player back to the hub.
     * @return its name, or {@code null} if none was taken since the last call
     */
    public String takeNewLight() {
        String name = newLight;
        newLight = null;
        return name;
    }

    /**
     * Adds a basin the water must fill.
     * @param left left side, in world px
     * @param top top side, in world px
     * @param width width, in px
     * @param height height, in px
     * @param requiredShare share of the level's water it needs, from 0 to 1
     */
    public void addGoal(double left, double top, double width, double height, double requiredShare) {
        goals.add(new Goal(left, top, width, height, requiredShare));
    }

    /**
     * Adds an enemy.
     * @param enemy the enemy
     */
    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    /** {@return the level this world was built from} */
    public Level level() {
        return level;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public List<Wall> obstaclesNear(double x, double y, double reach) {
        List<Wall> near = new ArrayList<>(walls);
        if (tunnel != null) {
            tunnel.collectEdges(x, y, reach, near);
        }
        for (Door door : doors) {
            if (!door.isOpen()) {
                near.add(door.wall());
            }
        }
        return near;
    }

    @Override
    public List<Wall> barriersNear(double x, double y, double reach) {
        List<Wall> near = new ArrayList<>();
        barrierGrid.collect(x, y, reach, near);
        return near;
    }

    @Override
    public List<Enemy> enemies() {
        return Collections.unmodifiableList(enemies);
    }

    /** {@return the player's drop} */
    public Droplet player() {
        return player;
    }

    /** {@return every drop of the world, the player's included} */
    public List<Droplet> droplets() {
        return Collections.unmodifiableList(droplets);
    }

    /** {@return the share of the level's water the player's drop holds} */
    public double playerWaterShare() {
        return player.volume() / TOTAL_VOLUME;
    }

    /** {@return whether the level is won: the last light is reached, or the basins stayed full long enough} */
    public boolean isComplete() {
        return !playerDead && (lastLightReached || filledTime >= WIN_HOLD_TIME);
    }

    /** {@return whether the player's drop has died} */
    public boolean isPlayerDead() {
        return playerDead;
    }

    /** {@return how long ago the player's drop died, in s} */
    public double timeSinceDeath() {
        return deathTime;
    }

    /**
     * Runs one frame of the game: moves the drops and the enemies near the player, applies the dangers, breaks
     * up and merges the drops, presses the buttons, opens the doors and checks the checkpoints and the lights.
     * @param dt time since the last frame, in s
     * @param dx horizontal input: -1, 0 or 1
     * @param dy vertical input: -1, 0 or 1
     */
    public void update(double dt, int dx, int dy) {
        time += dt;
        if (playerDead) {
            deathTime += dt;
        } else if (shoot.charge(dt)) {
            explode();
        }
        double length = Math.hypot(dx, dy);
        double thrust = length == 0 ? 0 : THRUST / length;
        for (Droplet droplet : droplets) {
            if (droplet == player) {
                movePlayer(dt, dx * thrust, dy * thrust);
            } else {
                droplet.update(dt, 0, 0, width, height);
            }
        }
        if (!playerDead && player.isTearing() && droplets.size() < MAX_DROPLETS) {
            tearPlayer();
        }
        for (Enemy enemy : enemies) {
            if (Math.abs(enemy.x() - player.x()) < ENEMY_RANGE && Math.abs(enemy.y() - player.y()) < ENEMY_RANGE) {
                enemy.update(dt, this);
            }
        }
        touchDangers();
        breakUpUnstableDroplets();
        mergeTouchingDroplets();
        moveShots(dt);
        pressButtons();
        updateDoors();

        boolean filled = !goals.isEmpty();
        for (Goal goal : goals) {
            goal.measure(droplets, TOTAL_VOLUME);
            filled &= goal.isFilled();
        }
        filledTime = filled ? filledTime + dt : 0;

        if (!playerDead) {
            for (int i = 0; i < checkpoints.size(); i++) {
                Checkpoint checkpoint = checkpoints.get(i);
                if (checkpoint.isTouchedBy(player)) {
                    checkpoint.reach();
                    progress.touchCheckpoint(i);
                }
            }
            for (Light light : lights) {
                if (!progress.hasLight(light.name()) && light.isTouchedBy(player)) {
                    reachLight(light);
                }
            }
        }
    }

    /** {@return whether the player can dash: the ability is learnt and the drop is alive} */
    public boolean canDash() {
        return player != null && !playerDead && progress.hasAbility(Ability.DASH);
    }

    /**
     * Tells the world whether the dash key is held, once a frame before {@link #update}. While it is held, the
     * back of the player's drop sticks to the ground and its head stretches away like an elastic; letting go
     * throws the drop from its back toward its head. Pulled too far, the drop tears in two, and the key must be
     * let go before the next dash. Nothing happens if the player {@linkplain #canDash() can't dash}.
     * @param held whether the dash key is held
     */
    public void holdDash(boolean held) {
        if (player != null && !playerDead) {
            dash.hold(player, held, canDash());
        }
    }

    /** Lets go of the ground without throwing the drop; the dash key must be let go before the next dash. */
    public void cancelDash() {
        if (player != null) {
            dash.cancel(player);
        }
    }

    /** {@return how stretched the player's drop is, from 0 to 1 where it tears; 0 when its back isn't stuck} */
    public double dashTension() {
        return player == null || playerDead ? 0 : player.tension();
    }

    /**
     * Gives the player the next drop of the world, to choose which half to steer once the drop has torn in two.
     * The drop left behind stays where it is.
     * @return {@code true} if the player steers another drop
     */
    public boolean switchDrop() {
        if (player == null || playerDead || droplets.size() < 2) {
            return false;
        }
        dash.cancel(player);
        player.stopSliding();
        player = droplets.get((droplets.indexOf(player) + 1) % droplets.size());
        return true;
    }

    /** {@return whether the player can slide: the ability is learnt and the drop is alive} */
    public boolean canSlide() {
        return player != null && !playerDead && progress.hasAbility(Ability.SLIDE);
    }

    /**
     * Tells the world whether the slide key is held, once a frame before {@link #update}. A press makes the
     * player's drop keep the speed and the direction it has, with no key held and whatever is steered; another
     * press gives the control back. A drop slower than {@link Slide#MIN_SPEED} doesn't start, and nothing starts
     * if the player {@linkplain #canSlide() can't slide}. Sticking the back of the drop for a dash, taking
     * another drop or hitting an edge of the world ends the slide.
     * @param held whether the slide key is held
     */
    public void holdSlide(boolean held) {
        if (player != null && !playerDead) {
            slide.hold(player, held, canSlide());
        }
    }

    /**
     * Makes the next press of the slide key wait until the key has been let go, e.g. when the game pauses; the
     * drop goes on sliding if it was.
     */
    public void waitForSlideKey() {
        slide.waitForRelease();
    }

    /** {@return whether the player's drop keeps its speed and its direction} */
    public boolean isSliding() {
        return player != null && !playerDead && player.isSliding();
    }

    /** {@return whether the player can shoot: the ability is learnt and the drop is alive} */
    public boolean canShoot() {
        return player != null && !playerDead && progress.hasAbility(Ability.SHOOT);
    }

    /**
     * Tells the world whether the shoot key is held and where the player aims, once a frame before {@link
     * #update}. While the key is held, the shot charges; letting go fires a ball from the player's drop, the
     * stronger the longer the key was held. Held longer than {@link Shoot#CHARGE_TIME}, the charge blows the
     * drop up. Nothing charges if the player {@linkplain #canShoot() can't shoot}.
     * @param held whether the shoot key is held
     * @param directionX x of the direction aimed at, of any length; 0 with directionY to keep the last one
     * @param directionY y of the direction aimed at
     */
    public void holdShot(boolean held, double directionX, double directionY) {
        if (player == null || playerDead) {
            return;
        }
        Shot shot = shoot.hold(player, held, directionX, directionY, canShoot());
        if (shot != null) {
            shots.add(shot);
            if (shots.size() > MAX_SHOTS) {
                shots.remove(0);
            }
        }
    }

    /**
     * Turns the aim of the shot being charged, once a frame before {@link #update}: one step when a turn key is
     * pressed, then more while it stays held.
     * @param direction -1 to turn left, 1 to turn right, 0 when no turn key is held
     * @param dt time since the last frame, in s
     */
    public void turnAim(int direction, double dt) {
        if (player != null && !playerDead) {
            shoot.turn(direction, dt);
        }
    }

    /** Drops the shot being charged without firing it; the shoot key must be let go before the next shot. */
    public void cancelShot() {
        shoot.cancel();
    }

    /** {@return whether a shot is being charged} */
    public boolean isChargingShot() {
        return shoot.isCharging();
    }

    /** {@return the power of the shot being charged, from 0 to 1 where the drop blows up; 0 when none is} */
    public double shotPower() {
        return shoot.power();
    }

    /** {@return the balls flying in the world} */
    public List<Shot> shots() {
        return Collections.unmodifiableList(shots);
    }

    /** {@return whether the player's drop died from a shot charged too long} */
    public boolean hasExploded() {
        return exploded;
    }

    private void explode() {
        if (invincible) {
            progress.hit();
            return;
        }
        droplets.remove(player);
        playerDead = true;
        exploded = true;
        deathTime = 0;
        progress.hit();
    }

    private void moveShots(double dt) {
        for (Shot shot : shots) {
            shot.update(dt, this, buttons);
        }
        shots.removeIf(shot -> !shot.isAlive());
    }

    private void movePlayer(double dt, double thrustX, double thrustY) {
        int steps = (int) Math.max(1, Math.ceil(player.speed() * dt / (player.radius() / 2)));
        for (int i = 0; i < steps; i++) {
            player.update(dt / steps, thrustX, thrustY, width, height);
            if (steps > 1 && !invincible && touchesDanger(player)) {
                return;
            }
        }
    }

    private void tearPlayer() {
        List<Droplet> halves = dash.tear(player);
        int index = droplets.indexOf(player);
        droplets.remove(index);
        droplets.addAll(index, halves);
        player = halves.get(0);
    }

    private void reachLight(Light light) {
        progress.obtainLight(light.name());
        if (light.isLast()) {
            lastLightReached = true;
            return;
        }
        newLight = light.name();
        if (hub >= 0) {
            progress.touchCheckpoint(hub);
        }
    }

    private void pressButtons() {
        for (DoorButton button : buttons) {
            button.press(droplets);
            if (button.isPressed() && button.locks()) {
                progress.lock(button.door());
            }
        }
    }

    private void updateDoors() {
        boolean allLights = obtainedLights().size() == lightCount();
        for (int i = 0; i < doors.size(); i++) {
            Door door = doors.get(i);
            boolean open = door.isGate() ? allLights : progress.isLocked(i);
            for (DoorButton button : buttons) {
                if (button.door() == i && button.isPressed()) {
                    open = true;
                }
            }
            door.setOpen(open);
        }
    }

    private void touchDangers() {
        for (Droplet droplet : new ArrayList<>(droplets)) {
            boolean touching = touchesDanger(droplet);
            if (droplet == player && invincible) {
                if (touching && !playerTouching) {
                    progress.hit();
                }
                playerTouching = touching;
            } else if (touching) {
                droplets.remove(droplet);
                if (droplet == player) {
                    playerDead = true;
                    deathTime = 0;
                    progress.hit();
                }
            }
        }
    }

    private boolean touchesDanger(Droplet droplet) {
        for (Wall wall : obstaclesNear(droplet.x(), droplet.y(), droplet.radius())) {
            if (wall.touches(droplet)) {
                return true;
            }
        }
        for (Enemy enemy : enemies) {
            double reach = enemy.radius() + droplet.radius();
            if (Math.abs(enemy.x() - droplet.x()) < reach && Math.abs(enemy.y() - droplet.y()) < reach
                    && enemy.touches(droplet.x(), droplet.y(), droplet.radius())) {
                return true;
            }
        }
        return false;
    }

    private void breakUpUnstableDroplets() {
        for (Droplet droplet : new ArrayList<>(droplets)) {
            if (droplet.isBreakingUp() && droplets.size() < MAX_DROPLETS) {
                replace(droplet, droplet.breakUp(random));
            }
        }
    }

    private void replace(Droplet droplet, List<Droplet> pieces) {
        droplets.remove(droplet);
        droplets.addAll(pieces);
        if (droplet == player) {
            player = largest(pieces);
        }
    }

    private void mergeTouchingDroplets() {
        for (int i = 0; i < droplets.size(); i++) {
            Droplet droplet = droplets.get(i);
            for (int j = droplets.size() - 1; j > i; j--) {
                Droplet other = droplets.get(j);
                if (droplet.canMergeWith(other)) {
                    droplet.absorb(other);
                    droplets.remove(j);
                    if (other == player) {
                        player = droplet;
                    }
                }
            }
        }
    }

    private static Droplet largest(List<Droplet> fragments) {
        Droplet largest = fragments.get(0);
        for (Droplet fragment : fragments) {
            if (fragment.radius() > largest.radius()) {
                largest = fragment;
            }
        }
        return largest;
    }

    /**
     * Draws what is inside the view.
     * @param g graphics already drawing in the world
     * @param view the visible area, in world px
     */
    public void draw(Graphics2D g, Rectangle view) {
        if (tunnel != null) {
            tunnel.draw(g, view);
        } else {
            Tunnel.drawNeon(g, new Rectangle(0, 0, width, height));
        }
        for (Checkpoint checkpoint : checkpoints) {
            if (checkpoint.isVisibleIn(view)) {
                checkpoint.draw(g);
            }
        }
        for (Light light : lights) {
            if (light.isVisibleIn(view)) {
                light.draw(g, time, progress.hasLight(light.name()));
            }
        }
        for (Door door : doors) {
            if (door.isVisibleIn(view)) {
                door.draw(g, time);
            }
        }
        for (DoorButton button : buttons) {
            if (button.isVisibleIn(view)) {
                button.draw(g, doors.get(button.door()).isOpen());
            }
        }
        for (Goal goal : goals) {
            if (goal.isVisibleIn(view)) {
                goal.draw(g);
            }
        }
        for (Wall wall : walls) {
            if (wall.isVisibleIn(view)) {
                wall.draw(g, time);
            }
        }
        Graphics2D water = (Graphics2D) g.create();
        water.clipRect(0, 0, width, height);
        renderer.draw(water, droplets, view.x, view.y, view.width, view.height);
        water.dispose();
        for (Enemy enemy : enemies) {
            if (enemy.isVisibleIn(view)) {
                enemy.draw(g, time);
            }
        }
        for (Shot shot : shots) {
            if (shot.isVisibleIn(view)) {
                shot.draw(g);
            }
        }
        if (playerDead) {
            drawDeath(g);
        } else {
            if (player.isAnchored()) {
                FluidRenderer.drawAnchor(g, player.bodyX(), player.bodyY(), player.tension());
            }
            shoot.draw(g, player);
            FluidRenderer.drawMarker(g, player.x(), player.y());
        }
    }

    private void drawDeath(Graphics2D g) {
        double t = Math.min(1, deathTime / DEATH_FLASH_TIME);
        double radius = exploded ? 2 * player.radius() : player.radius();
        double ring = radius * (1 + 3 * t);
        double flash = radius * (1 - t);
        Color color = exploded ? EXPLOSION_COLOR : FluidRenderer.NEON;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(255, 255, 255, (int) (220 * (1 - t))));
        g2.fill(new Ellipse2D.Double(player.x() - flash, player.y() - flash, 2 * flash, 2 * flash));
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * (1 - t))));
        g2.setStroke(DEATH_STROKE);
        g2.draw(new Ellipse2D.Double(player.x() - ring, player.y() - ring, 2 * ring, 2 * ring));
        g2.dispose();
    }
}
