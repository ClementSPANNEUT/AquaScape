package world;

import enemy.ability.Ability;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * What the player has achieved in a game: the reached checkpoints, the checkpoint to restart from, the obtained
 * lights, the doors locked open and the number of hits. It outlives the {@link World}, which is rebuilt at each
 * death, and is saved by {@link save.SaveData}.
 */
public class Progress {
    private final Set<Integer> reachedCheckpoints = new HashSet<>();
    private final Set<String> lights = new LinkedHashSet<>();
    private final Set<Integer> lockedDoors = new HashSet<>();
    private int checkpoint;
    private int hits;
    private boolean changed;

    /** Creates an empty progress, starting at the first checkpoint. */
    public Progress() {
    }

    /**
     * Creates a progress from saved data.
     * @param reachedCheckpoints indices of the reached checkpoints
     * @param lights names of the obtained lights
     * @param lockedDoors indices of the doors locked open
     * @param checkpoint index of the checkpoint to start from; it counts as reached
     */
    public Progress(Collection<Integer> reachedCheckpoints, Collection<String> lights, Collection<Integer> lockedDoors,
            int checkpoint) {
        this.reachedCheckpoints.addAll(reachedCheckpoints);
        this.lights.addAll(lights);
        this.lockedDoors.addAll(lockedDoors);
        this.checkpoint = checkpoint;
        this.reachedCheckpoints.add(checkpoint);
    }

    /** {@return how many times the player was hit} */
    public int hits() {
        return hits;
    }

    /** Counts one more hit. */
    public void hit() {
        hits++;
    }

    /** {@return the index of the checkpoint the player restarts from} */
    public int checkpoint() {
        return checkpoint;
    }

    /**
     * Records that the player touched a checkpoint: it becomes the restart point.
     * @param index index of the checkpoint
     */
    public void touchCheckpoint(int index) {
        boolean added = reachedCheckpoints.add(index);
        if (added || checkpoint != index) {
            changed = true;
        }
        checkpoint = index;
    }

    /**
     * Tells whether a checkpoint was ever reached.
     * @param checkpoint index of the checkpoint
     * @return {@code true} if it was reached
     */
    public boolean hasReached(int checkpoint) {
        return reachedCheckpoints.contains(checkpoint);
    }

    /** {@return the indices of every reached checkpoint} */
    public Set<Integer> reachedCheckpoints() {
        return Collections.unmodifiableSet(reachedCheckpoints);
    }

    /**
     * Tells whether a light was obtained.
     * @param name name of the light
     * @return {@code true} if it was obtained
     */
    public boolean hasLight(String name) {
        return lights.contains(name);
    }

    /**
     * Records an obtained light.
     * @param name name of the light
     */
    public void obtainLight(String name) {
        if (lights.add(name)) {
            changed = true;
        }
    }

    /** {@return the names of the obtained lights, in the order they were obtained} */
    public Set<String> lights() {
        return Collections.unmodifiableSet(lights);
    }

    /**
     * Tells whether the player has learnt an ability, that is, has brought back the light that teaches it.
     * @param ability the ability
     * @return {@code true} if it was learnt
     */
    public boolean hasAbility(Ability ability) {
        return lights.contains(ability.light());
    }

    /**
     * Tells whether a door was locked open.
     * @param door index of the door
     * @return {@code true} if it stays open
     */
    public boolean isLocked(int door) {
        return lockedDoors.contains(door);
    }

    /**
     * Locks a door open for the rest of the game.
     * @param door index of the door
     */
    public void lock(int door) {
        if (lockedDoors.add(door)) {
            changed = true;
        }
    }

    /** {@return the indices of the doors locked open} */
    public Set<Integer> lockedDoors() {
        return Collections.unmodifiableSet(lockedDoors);
    }

    /**
     * Tells whether something worth saving changed since the last call, and clears that flag.
     * @return {@code true} if the progress changed
     */
    public boolean takeChanged() {
        boolean wasChanged = changed;
        changed = false;
        return wasChanged;
    }
}
