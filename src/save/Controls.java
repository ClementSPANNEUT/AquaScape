package save;

import input.GamepadButton;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * The keys of each action of the game, two per action, and its controller button, chosen in the controls menu and
 * saved with the game. Échap and Start are kept for the pause and can't be bound; on the controller, the left stick
 * and the arrows always move, and the right stick always aims.
 */
public class Controls {
    /** Number of keys an action can have. */
    public static final int SLOTS = 2;
    /** Key code of an empty slot. */
    public static final int NONE = KeyEvent.VK_UNDEFINED;
    private static final String PREFIX = "controls.";
    private static final String GAMEPAD_PREFIX = "gamepad.";

    /** An action the player can bind to keys and to a controller button. */
    public enum Action {
        /** Moves the drop up. */
        UP("Haut", "Stick ↑", KeyEvent.VK_UP, KeyEvent.VK_Z),
        /** Moves the drop down. */
        DOWN("Bas", "Stick ↓", KeyEvent.VK_DOWN, KeyEvent.VK_S),
        /** Moves the drop left. */
        LEFT("Gauche", "Stick ←", KeyEvent.VK_LEFT, KeyEvent.VK_Q),
        /** Moves the drop right. */
        RIGHT("Droite", "Stick →", KeyEvent.VK_RIGHT, KeyEvent.VK_D),
        /** Held to stick the back of the drop and stretch it, let go to dash, once the Dash ability is learnt. */
        DASH("Dash", GamepadButton.A, KeyEvent.VK_SPACE, KeyEvent.VK_UNDEFINED),
        /** Steers the next drop, to choose a half once the drop has torn in two. */
        SWAP("Changer de goutte", GamepadButton.X, KeyEvent.VK_E, KeyEvent.VK_UNDEFINED),
        /** Held to charge a shot, let go to fire it, once the Shoot ability is learnt. */
        SHOOT("Tirer", GamepadButton.LT, KeyEvent.VK_CONTROL, KeyEvent.VK_UNDEFINED),
        /** Turns the aim of the shot being charged one step to the left. */
        AIM_LEFT("Viser à gauche", "Stick droit", KeyEvent.VK_LEFT, KeyEvent.VK_UNDEFINED),
        /** Turns the aim of the shot being charged one step to the right. */
        AIM_RIGHT("Viser à droite", "Stick droit", KeyEvent.VK_RIGHT, KeyEvent.VK_UNDEFINED),
        /**
         * Pressed to make the drop keep its speed and its direction, pressed again to steer it again, once the
         * Slide ability is learnt.
         */
        SLIDE("Slide", GamepadButton.RT, KeyEvent.VK_F, KeyEvent.VK_UNDEFINED),
        /** Restarts from the checkpoint the game started from. */
        RESTART("Recommencer", GamepadButton.Y, KeyEvent.VK_R, KeyEvent.VK_UNDEFINED),
        /** Shows or hides the coordinate grid. */
        GRID("Repère", GamepadButton.BACK, KeyEvent.VK_G, KeyEvent.VK_UNDEFINED);

        private final String label;
        private final GamepadButton button;
        private final String stick;
        private final int[] defaults;

        Action(String label, GamepadButton button, int... defaults) {
            this.label = label;
            this.button = button;
            this.stick = null;
            this.defaults = defaults;
        }

        Action(String label, String stick, int... defaults) {
            this.label = label;
            this.button = null;
            this.stick = stick;
            this.defaults = defaults;
        }

        /** {@return the name of the action shown in the menu} */
        public String label() {
            return label;
        }

        /** {@return whether the action follows a stick of the controller, instead of a button} */
        public boolean usesStick() {
            return stick != null;
        }

        /** {@return how the controller does the action with a stick, such as "Stick ↑", or {@code null}} */
        public String stickLabel() {
            return stick;
        }

        /** {@return whether the action moves the drop} */
        public boolean isMovement() {
            return this == UP || this == DOWN || this == LEFT || this == RIGHT;
        }

        /** {@return whether the action turns the aim of a shot} */
        public boolean isAim() {
            return this == AIM_LEFT || this == AIM_RIGHT;
        }

        /**
         * Tells whether a key can do this action and another one: the aim is only turned while a shot charges,
         * when its keys stop moving the drop, so a key can both move and aim.
         * @param other the other action
         * @return {@code true} if one moves the drop and the other turns the aim
         */
        public boolean sharesKeysWith(Action other) {
            return isMovement() && other.isAim() || isAim() && other.isMovement();
        }
    }

    private final Map<Action, int[]> keys = new EnumMap<>(Action.class);
    private final Map<Action, GamepadButton> buttons = new EnumMap<>(Action.class);

    /** Creates the default controls. */
    public Controls() {
        reset();
    }

    /** Puts back the default keys and controller buttons. */
    public void reset() {
        for (Action action : Action.values()) {
            keys.put(action, action.defaults.clone());
            buttons.put(action, action.button);
        }
    }

    /**
     * Gives the controller button of an action.
     * @param action the action
     * @return the button, or {@code null} if the action has none or {@linkplain Action#usesStick() uses a stick}
     */
    public GamepadButton button(Action action) {
        return buttons.get(action);
    }

    /**
     * Tells whether a controller button can be bound to an action.
     * @param button the button
     * @return {@code false} for no button, for the directions, kept for moving, and for Start, kept for the pause
     */
    public static boolean canBind(GamepadButton button) {
        return button != null && !button.isDirection() && button != GamepadButton.START;
    }

    /**
     * Binds a controller button to an action; the button leaves the action that had it.
     * @param action the action, which must not {@linkplain Action#usesStick() use a stick}
     * @param button the button, which must be {@link #canBind(GamepadButton) bindable}
     * @return the action that lost the button, or {@code null}
     * @throws IllegalArgumentException for an action that uses a stick or a button that can't be bound
     */
    public Action setButton(Action action, GamepadButton button) {
        if (action.usesStick() || !canBind(button)) {
            throw new IllegalArgumentException(button + " can't be bound to " + action);
        }
        Action previous = null;
        for (Map.Entry<Action, GamepadButton> entry : buttons.entrySet()) {
            if (entry.getValue() == button && entry.getKey() != action) {
                entry.setValue(null);
                previous = entry.getKey();
            }
        }
        buttons.put(action, button);
        return previous;
    }

    /**
     * Takes the controller button away from an action; the actions that use a stick keep it.
     * @param action the action
     */
    public void clearButton(Action action) {
        if (!action.usesStick()) {
            buttons.put(action, null);
        }
    }

    /**
     * Tells whether a controller button triggers an action.
     * @param action the action
     * @param button the button pressed
     * @return {@code true} if the button is bound to it
     */
    public boolean matches(Action action, GamepadButton button) {
        return button != null && buttons.get(action) == button;
    }

    /**
     * Gives the key in a slot.
     * @param action the action
     * @param slot 0 or 1
     * @return the key code, or {@link #NONE}
     */
    public int key(Action action, int slot) {
        return keys.get(action)[slot];
    }

    /**
     * Tells whether a key can be bound to an action.
     * @param key key code
     * @return {@code false} for no key and for Échap
     */
    public static boolean canBind(int key) {
        return key != NONE && key != KeyEvent.VK_ESCAPE;
    }

    /**
     * Binds a key to a slot; the key leaves the action that had it, unless the two {@linkplain
     * Action#sharesKeysWith(Action) can share it}.
     * @param action the action
     * @param slot 0 or 1
     * @param key key code
     * @return the action that lost the key, or {@code null}
     */
    public Action set(Action action, int slot, int key) {
        Action previous = null;
        for (Map.Entry<Action, int[]> entry : keys.entrySet()) {
            if (entry.getKey().sharesKeysWith(action)) {
                continue;
            }
            int[] slots = entry.getValue();
            for (int i = 0; i < SLOTS; i++) {
                if (slots[i] == key && (entry.getKey() != action || i != slot)) {
                    slots[i] = NONE;
                    previous = entry.getKey();
                }
            }
        }
        keys.get(action)[slot] = key;
        return previous;
    }

    /**
     * Empties a slot.
     * @param action the action
     * @param slot 0 or 1
     */
    public void clear(Action action, int slot) {
        keys.get(action)[slot] = NONE;
    }

    /**
     * Tells whether a key triggers an action.
     * @param action the action
     * @param key key code
     * @return {@code true} if the key is bound to it
     */
    public boolean matches(Action action, int key) {
        if (key == NONE) {
            return false;
        }
        for (int bound : keys.get(action)) {
            if (bound == key) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether one of an action's keys is held.
     * @param action the action
     * @param pressed key codes currently held
     * @return {@code true} if one is held
     */
    public boolean isPressed(Action action, Set<Integer> pressed) {
        for (int bound : keys.get(action)) {
            if (bound != NONE && pressed.contains(bound)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Describes the keys of an action, such as "↑ / Z".
     * @param action the action
     * @return the key names, or a dash if it has none
     */
    public String describe(Action action) {
        List<String> names = new ArrayList<>();
        for (int bound : keys.get(action)) {
            if (bound != NONE) {
                names.add(keyName(bound));
            }
        }
        return names.isEmpty() ? "—" : String.join(" / ", names);
    }

    /** {@return the movement keys as sets, such as "↑ ← ↓ → / Z Q S D"} */
    public String describeMovement() {
        List<String> sets = new ArrayList<>();
        for (int slot = 0; slot < SLOTS; slot++) {
            List<String> names = new ArrayList<>();
            for (Action action : List.of(Action.UP, Action.LEFT, Action.DOWN, Action.RIGHT)) {
                if (key(action, slot) != NONE) {
                    names.add(keyName(key(action, slot)));
                }
            }
            if (!names.isEmpty()) {
                sets.add(String.join(" ", names));
            }
        }
        return sets.isEmpty() ? "—" : String.join(" / ", sets);
    }

    /**
     * Gives a short French name for a key.
     * @param key key code
     * @return its name, such as "Maj" or "Espace"
     */
    public static String keyName(int key) {
        return switch (key) {
            case NONE -> "—";
            case KeyEvent.VK_UP -> "↑";
            case KeyEvent.VK_DOWN -> "↓";
            case KeyEvent.VK_LEFT -> "←";
            case KeyEvent.VK_RIGHT -> "→";
            case KeyEvent.VK_SHIFT -> "Maj";
            case KeyEvent.VK_CONTROL -> "Ctrl";
            case KeyEvent.VK_ALT -> "Alt";
            case KeyEvent.VK_SPACE -> "Espace";
            case KeyEvent.VK_ENTER -> "Entrée";
            case KeyEvent.VK_TAB -> "Tab";
            case KeyEvent.VK_BACK_SPACE -> "Retour arrière";
            case KeyEvent.VK_DELETE -> "Suppr";
            case KeyEvent.VK_CAPS_LOCK -> "Verr. maj";
            default -> KeyEvent.getKeyText(key);
        };
    }

    /**
     * Reads the keys and the controller buttons from saved properties; the actions not saved keep theirs, and so
     * do the ones saved with a button that can't be bound. An action added to the game since the save was written
     * gives up a default key or button that the save gave to another action.
     * @param properties the saved properties
     */
    public void load(Properties properties) {
        for (Action action : Action.values()) {
            String value = properties.getProperty(PREFIX + action.name());
            if (value == null) {
                continue;
            }
            String[] parts = value.split(",");
            int[] slots = new int[SLOTS];
            for (int i = 0; i < SLOTS; i++) {
                slots[i] = i < parts.length && !parts[i].isBlank() ? Integer.parseInt(parts[i].trim()) : NONE;
            }
            keys.put(action, slots);
        }
        for (Action action : Action.values()) {
            String value = properties.getProperty(GAMEPAD_PREFIX + action.name());
            if (value == null || action.usesStick()) {
                continue;
            }
            if (value.isBlank()) {
                buttons.put(action, null);
            } else {
                GamepadButton button = parseButton(value.trim());
                if (canBind(button)) {
                    buttons.put(action, button);
                }
            }
        }
        for (Action action : Action.values()) {
            if (properties.getProperty(PREFIX + action.name()) == null) {
                leaveTakenKeys(action);
            }
            if (properties.getProperty(GAMEPAD_PREFIX + action.name()) == null && isButtonTaken(action)) {
                buttons.put(action, null);
            }
        }
    }

    private void leaveTakenKeys(Action action) {
        int[] slots = keys.get(action);
        for (int slot = 0; slot < SLOTS; slot++) {
            for (Action other : Action.values()) {
                if (other != action && !other.sharesKeysWith(action) && matches(other, slots[slot])) {
                    slots[slot] = NONE;
                }
            }
        }
    }

    private boolean isButtonTaken(Action action) {
        for (Action other : Action.values()) {
            if (other != action && matches(other, buttons.get(action))) {
                return true;
            }
        }
        return false;
    }

    private static GamepadButton parseButton(String name) {
        for (GamepadButton button : GamepadButton.values()) {
            if (button.name().equals(name)) {
                return button;
            }
        }
        return null;
    }

    /**
     * Writes the keys and the controller buttons into properties.
     * @param properties the properties to save
     */
    public void store(Properties properties) {
        for (Action action : Action.values()) {
            int[] slots = keys.get(action);
            properties.setProperty(PREFIX + action.name(), slots[0] + "," + slots[1]);
            if (!action.usesStick()) {
                GamepadButton button = buttons.get(action);
                properties.setProperty(GAMEPAD_PREFIX + action.name(), button == null ? "" : button.name());
            }
        }
    }
}
