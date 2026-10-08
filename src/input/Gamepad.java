package input;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A game controller, read once a frame by {@link #poll()}.
 * <p>Subclasses only say which buttons are held ({@link #read()}, Template Method); this class finds what changed
 * since the last poll and tells the {@link GamepadListener listeners} (Observer). When no controller can be used,
 * {@link #none()} stands in for it, so the rest of the game never has to check.
 */
public abstract class Gamepad {
    private static final Pattern PLAYSTATION = Pattern.compile("(?i)ps[345]|dualsense|dualshock|playstation");

    private final List<GamepadListener> listeners = new ArrayList<>();
    private Set<GamepadButton> held = EnumSet.noneOf(GamepadButton.class);

    /** Creates a gamepad with no button held. */
    protected Gamepad() {
    }

    /** {@return a gamepad that is never connected and never pressed} */
    public static Gamepad none() {
        return new Gamepad() {
            @Override
            protected Set<GamepadButton> read() {
                return EnumSet.noneOf(GamepadButton.class);
            }

            @Override
            public boolean isConnected() {
                return false;
            }

            @Override
            public String name() {
                return "aucune";
            }
        };
    }

    /**
     * Opens the controllers plugged in through SDL. If the library or its native part can't be loaded, the game
     * goes on with the keyboard only.
     * @return the controllers, or {@link #none()} if they can't be used
     */
    public static Gamepad open() {
        try {
            return new SdlGamepad();
        } catch (LinkageError | RuntimeException e) {
            System.err.println("Manette indisponible, le clavier reste utilisable : " + e);
            return none();
        }
    }

    /**
     * Reads the controller: the buttons that started being held are told to the listeners as pressed, the ones
     * let go as released.
     */
    public final void poll() {
        Set<GamepadButton> now = EnumSet.noneOf(GamepadButton.class);
        now.addAll(read());
        for (GamepadButton button : GamepadButton.values()) {
            boolean down = now.contains(button);
            boolean before = held.contains(button);
            if (down && !before) {
                for (GamepadListener listener : List.copyOf(listeners)) {
                    listener.pressed(button);
                }
            } else if (!down && before) {
                for (GamepadListener listener : List.copyOf(listeners)) {
                    listener.released(button);
                }
            }
        }
        held = now;
    }

    /** {@return the buttons held right now, read from the controller} */
    protected abstract Set<GamepadButton> read();

    /** {@return whether a controller is plugged in} */
    public abstract boolean isConnected();

    /** {@return the kind of controller plugged in, such as "PS5 Controller"} */
    public abstract String name();

    /**
     * Tells whether a button was held at the last poll.
     * @param button the button
     * @return {@code true} if it is held
     */
    public boolean isHeld(GamepadButton button) {
        return held.contains(button);
    }

    /**
     * Gives the horizontal direction to move in. It can be held together with a vertical one, for the diagonals,
     * while the direction buttons told to the listeners are meant for the menus.
     * @return -1 left, 0 none, 1 right
     */
    public int directionX() {
        return (isHeld(GamepadButton.RIGHT) ? 1 : 0) - (isHeld(GamepadButton.LEFT) ? 1 : 0);
    }

    /**
     * Gives the vertical direction to move in; see {@link #directionX()}.
     * @return -1 up, 0 none, 1 down
     */
    public int directionY() {
        return (isHeld(GamepadButton.DOWN) ? 1 : 0) - (isHeld(GamepadButton.UP) ? 1 : 0);
    }

    /**
     * Gives where the right stick points along x, to aim with.
     * @return from -1 (left) to 1 (right), and 0 when the stick isn't pushed or there is none
     */
    public double aimX() {
        return 0;
    }

    /**
     * Gives where the right stick points along y; see {@link #aimX()}.
     * @return from -1 (up) to 1 (down), and 0 when the stick isn't pushed or there is none
     */
    public double aimY() {
        return 0;
    }

    /** {@return whether the controller plugged in is a PlayStation one, whose buttons have other names} */
    public boolean isPlayStation() {
        return PLAYSTATION.matcher(name()).find();
    }

    /**
     * Gives the name printed on a button of the controller plugged in.
     * @param button the button
     * @return its name, such as "Croix" on a PlayStation controller or "A" on another one
     */
    public String buttonName(GamepadButton button) {
        return button.label(isPlayStation());
    }

    /**
     * Adds a listener; listeners are told in the order they were added.
     * @param listener the listener
     */
    public void addListener(GamepadListener listener) {
        listeners.add(listener);
    }

    /** Lets go of the controllers when the game closes; does nothing by default. */
    public void close() {
    }
}
