package input;

import java.util.EnumSet;
import java.util.Set;

/** A controller driven by the tests: they hold and let go of its buttons, then poll it. */
public class FakeGamepad extends Gamepad {
    private final Set<GamepadButton> held = EnumSet.noneOf(GamepadButton.class);
    private final String name;
    private boolean connected = true;
    private double aimX;
    private double aimY;

    public FakeGamepad() {
        this("de test");
    }

    public FakeGamepad(String name) {
        this.name = name;
    }

    public void hold(GamepadButton... buttons) {
        held.addAll(Set.of(buttons));
    }

    public void letGo(GamepadButton... buttons) {
        held.removeAll(Set.of(buttons));
    }

    public void tap(GamepadButton button) {
        hold(button);
        poll();
        letGo(button);
        poll();
    }

    public void aim(double x, double y) {
        aimX = x;
        aimY = y;
    }

    @Override
    public double aimX() {
        return aimX;
    }

    @Override
    public double aimY() {
        return aimY;
    }

    public void unplug() {
        connected = false;
        held.clear();
    }

    @Override
    protected Set<GamepadButton> read() {
        return EnumSet.copyOf(held);
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public String name() {
        return name;
    }
}
