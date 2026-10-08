package input;

/** Told when the buttons of a {@link Gamepad} are pressed and released. */
public interface GamepadListener {
    /**
     * Called once when a button starts being held.
     * @param button the button
     */
    void pressed(GamepadButton button);

    /**
     * Called once when a button is let go; does nothing by default.
     * @param button the button
     */
    default void released(GamepadButton button) {
    }
}
