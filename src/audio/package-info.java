/**
 * The background music: a {@link audio.Track} for the menus and one for each zone of the world, played in a loop by
 * a {@link audio.Music}. The game asks on every frame for the track of the zone the player is in; the music only
 * changes when the track does. The audio files are read through the MP3SPI library.
 */
package audio;
