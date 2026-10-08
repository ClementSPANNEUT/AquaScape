package audio;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * The music read from the audio files of a folder: {@code assets/audio}, looked for in the current folder then
 * beside the compiled code. A track is decoded and sent to the sound card little by little on a thread of its own,
 * so a long file takes no memory and the game loop never waits for it. The volume is applied to the samples just
 * before they are sent, and the sound card is given a fifth of a second at most, so a change is heard at once.
 * <p>Java reads MP3 files through the MP3SPI library (with JLayer and Tritonus) of the {@code lib} folder. Without
 * it, or without a sound card, the game goes on in silence.
 */
final class Mp3Music extends Music {
    private static final Path FOLDER = Path.of("assets", "audio");
    private static final int BUFFER_SIZE = 4096;
    private static final int SAMPLE_BITS = 16;
    private static final double LINE_SECONDS = 0.2;

    private final Path folder;
    private final AtomicInteger plays = new AtomicInteger();
    private volatile Thread player;
    private volatile double gain = 1;

    Mp3Music(Path folder) {
        this.folder = folder;
    }

    static Path folder() {
        if (Files.isDirectory(FOLDER)) {
            return FOLDER;
        }
        try {
            Path code = Path.of(Mp3Music.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return code.resolveSibling(FOLDER);
        } catch (URISyntaxException | RuntimeException e) {
            return FOLDER;
        }
    }

    /** {@return how many times a file was played to its end, for the tests} */
    int plays() {
        return plays.get();
    }

    @Override
    protected boolean has(Track track) {
        return Files.isRegularFile(folder.resolve(track.file()));
    }

    @Override
    protected void start(Track track) {
        File file = folder.resolve(track.file()).toFile();
        Thread thread = new Thread(() -> loop(file), "music");
        thread.setDaemon(true);
        player = thread;
        thread.start();
    }

    @Override
    protected void silence() {
        player = null;
    }

    @Override
    protected void volumeChanged(double volume) {
        gain = gain(volume);
    }

    /**
     * Gives the factor the samples are multiplied by for a volume. The ear hears loudness on a curve, so the
     * factor is the square of the volume: half the volume then sounds about half as loud.
     * @param volume from 0 (silent) to 1 (as loud as the file)
     * @return the factor, from 0 to 1
     */
    static double gain(double volume) {
        return volume * volume;
    }

    /**
     * Makes 16-bit little-endian samples quieter, in place.
     * @param samples the samples, two bytes each
     * @param length how many bytes to change
     * @param gain the factor to multiply them by, from 0 to 1
     */
    static void scale(byte[] samples, int length, double gain) {
        for (int i = 0; i + 1 < length; i += 2) {
            int sample = (short) ((samples[i] & 0xFF) | (samples[i + 1] << 8));
            int quieter = (int) Math.round(sample * gain);
            samples[i] = (byte) quieter;
            samples[i + 1] = (byte) (quieter >> 8);
        }
    }

    private boolean isWanted() {
        return player == Thread.currentThread();
    }

    private void loop(File file) {
        try {
            while (isWanted() && playThrough(file)) {
                plays.incrementAndGet();
            }
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException | RuntimeException e) {
            System.err.println("Musique indisponible, le jeu continue sans : " + e);
        }
    }

    private boolean playThrough(File file)
            throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        try (AudioInputStream encoded = AudioSystem.getAudioInputStream(file)) {
            AudioFormat source = encoded.getFormat();
            AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, source.getSampleRate(), SAMPLE_BITS,
                    source.getChannels(), source.getChannels() * SAMPLE_BITS / 8, source.getSampleRate(), false);
            try (AudioInputStream sound = AudioSystem.getAudioInputStream(pcm, encoded);
                    SourceDataLine line = AudioSystem.getSourceDataLine(pcm)) {
                line.open(pcm, (int) (pcm.getSampleRate() * LINE_SECONDS) * pcm.getFrameSize());
                line.start();
                byte[] buffer = new byte[BUFFER_SIZE];
                boolean heard = false;
                int read;
                while (isWanted() && (read = sound.read(buffer)) > 0) {
                    scale(buffer, read, gain);
                    line.write(buffer, 0, read);
                    heard = true;
                }
                if (isWanted()) {
                    line.drain();
                }
                return heard && isWanted();
            }
        }
    }
}
