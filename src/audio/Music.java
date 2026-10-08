package audio;

/**
 * The background music: one {@link Track} at a time, played in a loop until another one is asked for.
 * <p>This class decides what plays; subclasses only say which tracks they have and start or silence one (Template
 * Method). Asking again for the track that plays changes nothing, so the game can ask on every frame and the music
 * goes on through a death or a pause. A track whose file is missing is replaced by the hub's. The volume can be
 * changed while a track plays. When no sound can be played, {@link #none()} stands in for the music, so the rest of
 * the game never has to check.
 */
public abstract class Music {
    private Track asked;
    private Track playing;
    private double volume = 1;

    /** Creates a music that plays nothing yet. */
    protected Music() {
    }

    /** {@return the music read from the audio files of the game} */
    public static Music open() {
        return new Mp3Music(Mp3Music.folder());
    }

    /** {@return a music that has no track and stays silent} */
    public static Music none() {
        return new Music() {
            @Override
            protected boolean has(Track track) {
                return false;
            }

            @Override
            protected void start(Track track) {
            }

            @Override
            protected void silence() {
            }

            @Override
            protected void volumeChanged(double volume) {
            }
        };
    }

    /**
     * Asks for a track: it starts from its beginning unless it is already the one playing.
     * @param track the track wanted
     */
    public final void play(Track track) {
        if (track == asked) {
            return;
        }
        asked = track;
        Track next = has(track) ? track : has(Track.HUB) ? Track.HUB : null;
        if (next == playing) {
            return;
        }
        playing = next;
        if (next == null) {
            silence();
        } else {
            start(next);
        }
    }

    /** Stops the music. */
    public final void stop() {
        asked = null;
        if (playing != null) {
            playing = null;
            silence();
        }
    }

    /** {@return the track that plays, or {@code null} when the music is silent} */
    public final Track playing() {
        return playing;
    }

    /**
     * Changes the volume, at once for the track that plays.
     * @param volume from 0 (silent) to 1 (as loud as the file); a value outside is brought back in
     */
    public final void setVolume(double volume) {
        this.volume = Math.max(0, Math.min(1, volume));
        volumeChanged(this.volume);
    }

    /** {@return the volume, from 0 (silent) to 1 (as loud as the file)} */
    public final double volume() {
        return volume;
    }

    /**
     * Tells whether a track can be played.
     * @param track the track
     * @return {@code true} if its audio file is there
     */
    protected abstract boolean has(Track track);

    /**
     * Starts a track in a loop, in place of the one that plays.
     * @param track a track this music has
     */
    protected abstract void start(Track track);

    /** Stops the track that plays. */
    protected abstract void silence();

    /**
     * Applies a new volume to the track that plays and to the next ones.
     * @param volume from 0 (silent) to 1 (as loud as the file)
     */
    protected abstract void volumeChanged(double volume);
}
