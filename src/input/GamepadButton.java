package input;

/**
 * A button of a game controller; the four directions of the left stick and of the arrows count as buttons.
 * <p>Each button knows the name printed on it on Xbox controllers and on PlayStation controllers.
 */
public enum GamepadButton {
    /** The stick or the arrows pushed up. */
    UP("↑", "↑"),
    /** The stick or the arrows pushed down. */
    DOWN("↓", "↓"),
    /** The stick or the arrows pushed left. */
    LEFT("←", "←"),
    /** The stick or the arrows pushed right. */
    RIGHT("→", "→"),
    /** The bottom face button: Cross on PlayStation, A on Xbox. */
    A("A", "Croix"),
    /** The right face button: Circle on PlayStation, B on Xbox. */
    B("B", "Rond"),
    /** The left face button: Square on PlayStation, X on Xbox. */
    X("X", "Carré"),
    /** The top face button: Triangle on PlayStation, Y on Xbox. */
    Y("Y", "Triangle"),
    /** The left bumper: L1 on PlayStation, LB on Xbox. */
    LB("LB", "L1"),
    /** The right bumper: R1 on PlayStation, RB on Xbox. */
    RB("RB", "R1"),
    /** The left trigger pressed more than halfway: L2 on PlayStation, LT on Xbox. */
    LT("LT", "L2"),
    /** The right trigger pressed more than halfway: R2 on PlayStation, RT on Xbox. */
    RT("RT", "R2"),
    /** The left stick pressed in: L3 on PlayStation, LS on Xbox. */
    L3("LS", "L3"),
    /** The right stick pressed in: R3 on PlayStation, RS on Xbox. */
    R3("RS", "R3"),
    /** The menu button on the right: Options on PlayStation, Menu or Start on Xbox. */
    START("Start", "Options"),
    /** The button on the left: Create on PlayStation, View or Back on Xbox. */
    BACK("Select", "Create");

    private final String xboxName;
    private final String playStationName;

    GamepadButton(String xboxName, String playStationName) {
        this.xboxName = xboxName;
        this.playStationName = playStationName;
    }

    /** {@return whether this is one of the four directions} */
    public boolean isDirection() {
        return this == UP || this == DOWN || this == LEFT || this == RIGHT;
    }

    /**
     * Gives the name printed on the button.
     * @param playStation whether the controller is a PlayStation one
     * @return the name, such as "A" or "Croix"
     */
    public String label(boolean playStation) {
        return playStation ? playStationName : xboxName;
    }
}
