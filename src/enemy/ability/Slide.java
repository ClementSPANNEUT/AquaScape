package enemy.ability;

import fluid.Droplet;

/**
 * The Slide ability: a press of its key makes the drop keep the speed and the direction it has, with no key held
 * and whatever the player steers; another press gives the control back, and the drop slows down again.
 * <p>Keeping its velocity is the drop's physics ({@link Droplet#slide()}, {@link Droplet#stopSliding()}); this
 * class decides when it happens. The key is read once a frame and only a new press counts, so holding it changes
 * nothing. A drop that is too slow has no direction to keep and doesn't start sliding.
 */
public final class Slide {
    /** Speed under which the drop is too slow to start sliding, in px/s. */
    public static final double MIN_SPEED = 60;

    private boolean wasHeld;

    /** Creates the ability, waiting for a press. */
    public Slide() {
    }

    /**
     * Tells the ability whether the slide key is held, once a frame.
     * @param drop the player's drop
     * @param keyHeld whether the slide key is held
     * @param allowed whether the player can slide; the drop never starts otherwise, but a drop that slides can
     *     always stop
     */
    public void hold(Droplet drop, boolean keyHeld, boolean allowed) {
        boolean pressed = keyHeld && !wasHeld;
        wasHeld = keyHeld;
        if (!pressed) {
            return;
        }
        if (drop.isSliding()) {
            drop.stopSliding();
        } else if (allowed && drop.speed() >= MIN_SPEED) {
            drop.slide();
        }
    }

    /**
     * Makes the next press wait until the key has been let go, e.g. when the game pauses: the button that closes
     * a pop-up is still held when the game resumes, and must not count as a press.
     */
    public void waitForRelease() {
        wasHeld = true;
    }
}
