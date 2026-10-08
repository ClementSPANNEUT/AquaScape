package audio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Mp3MusicTest {
    private static final AudioFormat FORMAT = new AudioFormat(44100, 16, 2, true, false);
    private static final long PATIENCE_MS = 10_000;

    @TempDir
    Path folder;
    private Mp3Music music;

    @BeforeEach
    void open() {
        music = new Mp3Music(folder);
    }

    @AfterEach
    void close() throws InterruptedException {
        music.stop();
        assertTrue(waitForNoPlayer(), "the player lets go of its file");
    }

    private static boolean isPlayerAlive() {
        return Thread.getAllStackTraces().keySet().stream()
                .anyMatch(thread -> thread.getName().equals("music") && thread.isAlive());
    }

    private static boolean waitForNoPlayer() throws InterruptedException {
        long end = System.currentTimeMillis() + PATIENCE_MS;
        while (isPlayerAlive() && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
        return !isPlayerAlive();
    }

    private void silentFile(Track track, double seconds) throws IOException {
        byte[] samples = new byte[(int) (FORMAT.getSampleRate() * seconds) * FORMAT.getFrameSize()];
        try (AudioInputStream sound = new AudioInputStream(new ByteArrayInputStream(samples), FORMAT,
                samples.length / FORMAT.getFrameSize())) {
            AudioSystem.write(sound, AudioFileFormat.Type.WAVE, folder.resolve(track.file()).toFile());
        }
    }

    private static void assumeSoundCanBePlayed() {
        assumeTrue(AudioSystem.isLineSupported(new DataLine.Info(SourceDataLine.class, FORMAT)), "no sound card");
    }

    private boolean waitFor(int plays) throws InterruptedException {
        long end = System.currentTimeMillis() + PATIENCE_MS;
        while (music.plays() < plays && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
        return music.plays() >= plays;
    }

    private static byte[] samples(int... values) {
        byte[] bytes = new byte[values.length * 2];
        for (int i = 0; i < values.length; i++) {
            bytes[2 * i] = (byte) values[i];
            bytes[2 * i + 1] = (byte) (values[i] >> 8);
        }
        return bytes;
    }

    @Test
    void theVolumeMultipliesTheSamples() {
        byte[] sound = samples(1000, -1000, 32767, -32768, 1, 0);
        Mp3Music.scale(sound, sound.length, 0.5);
        assertArrayEquals(samples(500, -500, 16384, -16384, 1, 0), sound);
    }

    @Test
    void fullVolumeLeavesTheSamplesAndNoVolumeSilencesThem() {
        byte[] sound = samples(1000, -1000, 32767, -32768);
        Mp3Music.scale(sound, sound.length, 1);
        assertArrayEquals(samples(1000, -1000, 32767, -32768), sound);
        Mp3Music.scale(sound, sound.length, 0);
        assertArrayEquals(samples(0, 0, 0, 0), sound);
    }

    @Test
    void onlyTheBytesReadAreChanged() {
        byte[] sound = samples(1000, 1000, 1000);
        Mp3Music.scale(sound, 4, 0.5);
        assertArrayEquals(samples(500, 500, 1000), sound, "the end of the buffer holds older sound");
        Mp3Music.scale(sound, 3, 0.5);
        assertArrayEquals(samples(250, 500, 1000), sound, "half a sample is left alone");
    }

    @Test
    void halfTheVolumeSoundsAboutHalfAsLoud() {
        assertEquals(0, Mp3Music.gain(0));
        assertEquals(0.25, Mp3Music.gain(0.5));
        assertEquals(1, Mp3Music.gain(1));
    }

    @Test
    void theVolumeCanChangeWhileATrackPlays() throws Exception {
        assumeSoundCanBePlayed();
        silentFile(Track.HUB, 0.1);
        music.setVolume(0.3);
        music.play(Track.HUB);
        assertTrue(waitFor(1));
        music.setVolume(0);
        music.setVolume(1);
        assertTrue(waitFor(music.plays() + 2), "the track goes on");
    }

    @Test
    void onlyTheFilesOfTheFolderCanBePlayed() throws IOException {
        Files.writeString(folder.resolve(Track.HUB.file()), "here");
        Files.createDirectory(folder.resolve(Track.DASH.file()));
        assertTrue(music.has(Track.HUB));
        assertFalse(music.has(Track.SPLIT), "no file");
        assertFalse(music.has(Track.DASH), "a folder is not a file");
    }

    @Test
    void theMusicFolderIsTheOneOfTheGame() {
        assertTrue(Mp3Music.folder().endsWith(Path.of("assets", "audio")), Mp3Music.folder().toString());
    }

    @Test
    void aTrackIsPlayedAgainEachTimeItEnds() throws Exception {
        assumeSoundCanBePlayed();
        silentFile(Track.HUB, 0.1);
        music.play(Track.HUB);
        assertTrue(waitFor(3), "played " + music.plays() + " times");
    }

    @Test
    void stoppingEndsTheLoop() throws Exception {
        assumeSoundCanBePlayed();
        silentFile(Track.HUB, 0.1);
        music.play(Track.HUB);
        assertTrue(waitFor(1));
        assertTrue(isPlayerAlive());
        music.stop();
        assertTrue(waitForNoPlayer());
        int plays = music.plays();
        Thread.sleep(400);
        assertEquals(plays, music.plays(), "nothing plays after the stop");
    }

    @Test
    void anotherTrackReplacesTheOneThatPlays() throws Exception {
        assumeSoundCanBePlayed();
        silentFile(Track.HUB, 30);
        silentFile(Track.SPLIT, 0.1);
        music.play(Track.HUB);
        Thread.sleep(300);
        music.play(Track.SPLIT);
        assertTrue(waitFor(2), "the long track would still be playing");
    }

    @Test
    void aFileThatIsNotSoundLeavesTheGameRunning() throws Exception {
        Files.writeString(folder.resolve(Track.HUB.file()), "this is not music");
        PrintStream console = System.err;
        ByteArrayOutputStream said = new ByteArrayOutputStream();
        System.setErr(new PrintStream(said, true, StandardCharsets.UTF_8));
        try {
            music.play(Track.HUB);
            long end = System.currentTimeMillis() + PATIENCE_MS;
            while (said.size() == 0 && System.currentTimeMillis() < end) {
                Thread.sleep(20);
            }
        } finally {
            System.setErr(console);
        }
        assertTrue(said.toString(StandardCharsets.UTF_8).startsWith("Musique indisponible"), said.toString());
        assertEquals(0, music.plays());
    }
}
