package enemy.ability;

import fluid.Droplet;
import java.util.List;

/**
 * The Dash ability: while its key is held, the back of the drop sticks to the ground and its head stretches away
 * like an elastic; letting go throws the drop from its back toward its head. Pulled too far, the drop tears in
 * two.
 * <p>The stretching itself is the drop's physics ({@link Droplet#anchor()}, {@link Droplet#release()},
 * {@link Droplet#tear()}); this class decides when it happens. After a tear or a {@linkplain #cancel cancel},
 * the key must be let go before the next dash.
 */
public final class Dash {
    private boolean armed = true;

    /** Creates the ability, ready to dash. */
    public Dash() {
    }

    /**
     * Tells the ability whether the dash key is held, once a frame.
     * @param drop the player's drop
     * @param keyHeld whether the dash key is held
     * @param allowed whether the player can dash; the drop never sticks otherwise
     */
    public void hold(Droplet drop, boolean keyHeld, boolean allowed) {
        if (!keyHeld) {
            drop.release();
            armed = true;
        } else if (armed && allowed) {
            drop.anchor();
        }
    }

    /**
     * Lets go of the ground without throwing the drop, e.g. when the game pauses or the player takes another
     * drop.
     * @param drop the player's drop
     */
    public void cancel(Droplet drop) {
        drop.detach();
        armed = false;
    }

    /**
     * Tears the overstretched drop in two halves.
     * @param drop the player's drop, {@linkplain Droplet#isTearing() stretched as far as it goes}
     * @return its head, which the player keeps, then its back
     */
    public List<Droplet> tear(Droplet drop) {
        armed = false;
        return drop.tear();
    }
}
