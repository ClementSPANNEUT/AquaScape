package audio;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** A music driven by the tests: it makes no sound and records the tracks it is told to start. */
public class FakeMusic extends Music {
    private final Set<Track> files = EnumSet.allOf(Track.class);
    private final List<Track> started = new ArrayList<>();
    private final List<Double> volumes = new ArrayList<>();
    private int silences;

    public void lose(Track... tracks) {
        files.removeAll(Set.of(tracks));
    }

    public List<Track> started() {
        return started;
    }

    public int silences() {
        return silences;
    }

    @Override
    protected boolean has(Track track) {
        return files.contains(track);
    }

    @Override
    protected void start(Track track) {
        started.add(track);
    }

    @Override
    protected void silence() {
        silences++;
    }

    public List<Double> volumes() {
        return volumes;
    }

    @Override
    protected void volumeChanged(double volume) {
        volumes.add(volume);
    }
}
