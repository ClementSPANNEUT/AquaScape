package input;

import com.studiohartman.jamepad.Configuration;
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The real controllers, read through the Jamepad library (SDL). The first controller plugged in drives the game.
 * SDL knows the layout of most controllers; {@code lib/gamecontrollerdb.txt}, looked for in the current folder then
 * beside the compiled code, adds many more.
 * <p>The stick and the arrows move in eight directions, but the listeners only get one direction at a time, so a
 * menu never sees a push down as a push sideways too. The right stick aims, in any direction.
 */
final class SdlGamepad extends Gamepad {
    private static final float DEAD_ZONE = 0.5f;
    private static final float AIM_DEAD_ZONE = 0.3f;
    private static final String MAPPINGS = "lib" + File.separator + "gamecontrollerdb.txt";
    private static final int MAX_CONTROLLERS = 4;

    private final ControllerManager manager;
    private ControllerState state;
    private GamepadButton direction;

    SdlGamepad() {
        Configuration configuration = new Configuration();
        configuration.maxNumControllers = MAX_CONTROLLERS;
        manager = new ControllerManager(configuration, mappings());
        manager.initSDLGamepad();
    }

    private static String mappings() {
        for (File file : List.of(new File(MAPPINGS), besideTheCode())) {
            if (file.isFile()) {
                return file.getPath();
            }
        }
        try {
            File none = File.createTempFile("aquascape-mappings", ".txt");
            none.deleteOnExit();
            Files.writeString(none.toPath(), "# SDL's built-in mappings only\n");
            return none.getPath();
        } catch (IOException e) {
            return MAPPINGS;
        }
    }

    private static File besideTheCode() {
        try {
            File code = new File(SdlGamepad.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return new File(code.getParentFile(), MAPPINGS);
        } catch (URISyntaxException | SecurityException | NullPointerException e) {
            return new File(MAPPINGS);
        }
    }

    @Override
    protected Set<GamepadButton> read() {
        state = firstConnected();
        Set<GamepadButton> held = EnumSet.noneOf(GamepadButton.class);
        if (state == null) {
            direction = null;
            return held;
        }
        direction = dominant(direction, stickX(), stickY());
        if (direction != null) {
            held.add(direction);
        }
        add(held, GamepadButton.A, state.a);
        add(held, GamepadButton.B, state.b);
        add(held, GamepadButton.X, state.x);
        add(held, GamepadButton.Y, state.y);
        add(held, GamepadButton.LB, state.lb);
        add(held, GamepadButton.RB, state.rb);
        add(held, GamepadButton.LT, state.leftTrigger > DEAD_ZONE);
        add(held, GamepadButton.RT, state.rightTrigger > DEAD_ZONE);
        add(held, GamepadButton.L3, state.leftStickClick);
        add(held, GamepadButton.R3, state.rightStickClick);
        add(held, GamepadButton.START, state.start);
        add(held, GamepadButton.BACK, state.back);
        return held;
    }

    @Override
    public int directionX() {
        return state == null ? 0 : step(stickX());
    }

    @Override
    public int directionY() {
        return state == null ? 0 : step(stickY());
    }

    @Override
    public double aimX() {
        return state == null ? 0 : aim(state.rightStickX, state.rightStickY);
    }

    @Override
    public double aimY() {
        return state == null ? 0 : aim(state.rightStickY, state.rightStickX);
    }

    /**
     * Gives the position of the right stick on one axis, ignoring a stick that is hardly pushed.
     * @param position position on the axis, from -1 to 1
     * @param other position on the other axis, from -1 to 1
     * @return the position, or 0 when the stick is inside its dead zone
     */
    static double aim(float position, float other) {
        return Math.hypot(position, other) < AIM_DEAD_ZONE ? 0 : position;
    }

    private float stickX() {
        return axis(state.dpadLeft, state.dpadRight, state.leftStickX);
    }

    private float stickY() {
        return axis(state.dpadUp, state.dpadDown, state.leftStickY);
    }

    /**
     * Gives the position on one axis: the arrows when one of them is held, the stick otherwise.
     * @param minus whether the arrow toward -1 is held
     * @param plus whether the arrow toward 1 is held
     * @param stick position of the stick on the axis, from -1 to 1
     * @return the position, from -1 to 1
     */
    static float axis(boolean minus, boolean plus, float stick) {
        if (minus != plus) {
            return plus ? 1 : -1;
        }
        return stick;
    }

    /**
     * Turns a position on an axis into a direction to move in.
     * @param position position from -1 to 1
     * @return -1, 0 inside the dead zone, or 1
     */
    static int step(float position) {
        if (Math.abs(position) <= DEAD_ZONE) {
            return 0;
        }
        return position > 0 ? 1 : -1;
    }

    /**
     * Gives the single direction the menus see: the one pushed the most, so that a stick pushed down a little
     * sideways still only goes down. The direction already held stays while it is pushed past the dead zone.
     * @param held the direction held at the last poll, or {@code null}
     * @param x horizontal position, from -1 (left) to 1
     * @param y vertical position, from -1 (up) to 1
     * @return the direction, or {@code null} inside the dead zone
     */
    static GamepadButton dominant(GamepadButton held, float x, float y) {
        if (held != null && push(held, x, y) > DEAD_ZONE) {
            return held;
        }
        if (Math.max(Math.abs(x), Math.abs(y)) <= DEAD_ZONE) {
            return null;
        }
        if (Math.abs(y) >= Math.abs(x)) {
            return y < 0 ? GamepadButton.UP : GamepadButton.DOWN;
        }
        return x < 0 ? GamepadButton.LEFT : GamepadButton.RIGHT;
    }

    private static float push(GamepadButton direction, float x, float y) {
        return switch (direction) {
            case UP -> -y;
            case DOWN -> y;
            case LEFT -> -x;
            case RIGHT -> x;
            default -> 0;
        };
    }

    private ControllerState firstConnected() {
        for (int i = 0; i < MAX_CONTROLLERS; i++) {
            ControllerState candidate = manager.getState(i);
            if (candidate.isConnected) {
                return candidate;
            }
        }
        return null;
    }

    private static void add(Set<GamepadButton> held, GamepadButton button, boolean down) {
        if (down) {
            held.add(button);
        }
    }

    @Override
    public boolean isConnected() {
        return state != null;
    }

    @Override
    public String name() {
        return state == null ? "aucune" : state.controllerType;
    }

    @Override
    public void close() {
        manager.quitSDLGamepad();
    }
}
