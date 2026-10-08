package audio;

/**
 * A music track of the game: one for the menus and one for each zone of the world. A track is the audio file of
 * that name in the music folder, and it knows the zone it belongs to by the name the plan of the zone gives, so
 * the game only has to ask for the track of the zone the player is in.
 */
public enum Track {
    /** The menus, the checkpoint map and the controls screen. */
    MENU("Debut", null),
    /** The hub, and the corridors that lead to the zones. */
    HUB("Hub", "Hub"),
    /** The Split zone, the first level. */
    SPLIT("Level1", "Split"),
    /** The Dash zone, the second level. */
    DASH("Level2", "Dash"),
    /** The Shoot zone, the third level. */
    SHOOT("Level3", "Shoot"),
    /** The Pary zone, the fourth level. */
    PARY("Level4", "Pary"),
    /** The zone of the light, the fifth level. */
    LIGHT("Level5", "Lumière"),
    /** The last zone. */
    END("Final", "Fin"),
    /** The training room. */
    TRAINING("Trainning", "Entraînement");

    private final String file;
    private final String zone;

    Track(String file, String zone) {
        this.file = file;
        this.zone = zone;
    }

    /** {@return the name of its audio file, such as "Level1.mp3"} */
    public String file() {
        return file + ".mp3";
    }

    /** {@return the name of the zone it plays in, or {@code null} for the menus} */
    public String zone() {
        return zone;
    }

    /**
     * Finds the track of a zone.
     * @param zone name of the zone, as its plan gives it
     * @return its track, or the hub's when the zone has none of its own
     */
    public static Track ofZone(String zone) {
        for (Track track : values()) {
            if (zone != null && zone.equals(track.zone)) {
                return track;
            }
        }
        return HUB;
    }
}
