package save;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import world.Progress;

/**
 * Progress and settings, kept in a small file in the user's home folder: finished levels, window size, volume,
 * invincible mode, validated checkpoints, lights, doors locked open, last checkpoint and controls. The
 * {@code aquascape.save} system property can point it to another file.
 */
public class SaveData {
    /** The loudest volume, in percent. */
    public static final int MAX_VOLUME = 100;
    private static final Path FILE = Path.of(System.getProperty("aquascape.save",
            Path.of(System.getProperty("user.home"), ".aquascape", "save.properties").toString()));

    private int completedLevels;
    private WindowSize windowSize = WindowSize.DEFAULT;
    private boolean invincible;
    private int volume = MAX_VOLUME;
    private final Set<Integer> checkpoints = new TreeSet<>();
    private final Set<String> lights = new LinkedHashSet<>();
    private final Set<Integer> doors = new TreeSet<>();
    private int lastCheckpoint;
    private final Controls controls = new Controls();
    private final Path file;

    private SaveData(Path file) {
        this.file = file;
    }

    /**
     * Reads the save from the user's home folder, or starts a fresh one.
     * @return the save
     */
    public static SaveData load() {
        return load(FILE);
    }

    /**
     * Reads a save file, or starts a fresh save if the file doesn't exist or can't be read. The save is written
     * back to the same file.
     * @param file the save file
     * @return the save
     */
    public static SaveData load(Path file) {
        SaveData data = new SaveData(file);
        if (!Files.exists(file)) {
            return data;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
            data.completedLevels = Math.max(0, Integer.parseInt(properties.getProperty("completedLevels", "0")));
            data.windowSize = WindowSize.valueOf(properties.getProperty("windowSize", WindowSize.DEFAULT.name()));
            data.invincible = Boolean.parseBoolean(properties.getProperty("invincible", "false"));
            data.checkpoints.addAll(numbers(properties.getProperty("checkpoints", "")));
            data.lights.addAll(names(properties.getProperty("lights", "")));
            data.doors.addAll(numbers(properties.getProperty("doors", "")));
            data.lastCheckpoint = Math.max(0, Integer.parseInt(properties.getProperty("lastCheckpoint", "0")));
            data.controls.load(properties);
            data.setVolume(Integer.parseInt(properties.getProperty("volume", String.valueOf(MAX_VOLUME))));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Sauvegarde illisible, elle est ignorée : " + e.getMessage());
        }
        return data;
    }

    /** Writes the save to its file. */
    public void save() {
        Properties properties = new Properties();
        properties.setProperty("completedLevels", String.valueOf(completedLevels));
        properties.setProperty("windowSize", windowSize.name());
        properties.setProperty("invincible", String.valueOf(invincible));
        properties.setProperty("volume", String.valueOf(volume));
        properties.setProperty("checkpoints", join(checkpoints));
        properties.setProperty("lights", join(lights));
        properties.setProperty("doors", join(doors));
        properties.setProperty("lastCheckpoint", String.valueOf(lastCheckpoint));
        controls.store(properties);
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                properties.store(out, "Aquascape");
            }
        } catch (IOException e) {
            System.err.println("Impossible d'enregistrer la progression : " + e.getMessage());
        }
    }

    /** {@return how many levels are finished} */
    public int completedLevels() {
        return completedLevels;
    }

    /**
     * Records a finished level: it unlocks the next one, and replaying an older level changes nothing.
     * @param level index of the level
     */
    public void complete(int level) {
        completedLevels = Math.max(completedLevels, level + 1);
    }

    /** {@return the chosen window size} */
    public WindowSize windowSize() {
        return windowSize;
    }

    /**
     * Chooses the window size.
     * @param windowSize the size
     */
    public void setWindowSize(WindowSize windowSize) {
        this.windowSize = windowSize;
    }

    /** {@return whether the invincible mode is on: the player's drop can't die} */
    public boolean isInvincible() {
        return invincible;
    }

    /**
     * Turns the invincible mode on or off.
     * @param invincible whether the player's drop can't die
     */
    public void setInvincible(boolean invincible) {
        this.invincible = invincible;
    }

    /** {@return the volume of the music, in percent, from 0 (silent) to {@link #MAX_VOLUME}} */
    public int volume() {
        return volume;
    }

    /**
     * Chooses the volume of the music.
     * @param volume the volume, in percent; a value outside 0 to {@link #MAX_VOLUME} is brought back in
     */
    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(MAX_VOLUME, volume));
    }

    /** {@return the indices of the validated checkpoints} */
    public Set<Integer> checkpoints() {
        return Collections.unmodifiableSet(checkpoints);
    }

    /** {@return the names of the obtained lights} */
    public Set<String> lights() {
        return Collections.unmodifiableSet(lights);
    }

    /** {@return the indices of the doors locked open} */
    public Set<Integer> doors() {
        return Collections.unmodifiableSet(doors);
    }

    /** {@return the key bindings} */
    public Controls controls() {
        return controls;
    }

    /** {@return the last checkpoint touched} */
    public int lastCheckpoint() {
        return lastCheckpoint;
    }

    /**
     * Adds a game's progress to the save: what was reached stays reached.
     * @param progress the progress of the game
     */
    public void remember(Progress progress) {
        checkpoints.addAll(progress.reachedCheckpoints());
        lights.addAll(progress.lights());
        doors.addAll(progress.lockedDoors());
        lastCheckpoint = progress.checkpoint();
    }

    private static Set<Integer> numbers(String text) {
        return names(text).stream().map(Integer::parseInt).collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> names(String text) {
        Set<String> names = new LinkedHashSet<>();
        for (String name : text.split(",")) {
            if (!name.isBlank()) {
                names.add(name.trim());
            }
        }
        return names;
    }

    private static String join(Collection<?> values) {
        return values.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
