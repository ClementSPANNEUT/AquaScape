package audio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class MusicTest {
    private final FakeMusic music = new FakeMusic();

    @Test
    void nothingPlaysAtFirst() {
        assertNull(music.playing());
        assertEquals(List.of(), music.started());
    }

    @Test
    void aTrackStartsWhenItIsAskedFor() {
        music.play(Track.SPLIT);
        assertEquals(Track.SPLIT, music.playing());
        assertEquals(List.of(Track.SPLIT), music.started());
    }

    @Test
    void askingAgainForTheTrackThatPlaysDoesNotStartItAgain() {
        for (int frame = 0; frame < 60; frame++) {
            music.play(Track.SPLIT);
        }
        assertEquals(List.of(Track.SPLIT), music.started());
    }

    @Test
    void anotherTrackTakesThePlaceOfTheOneThatPlays() {
        music.play(Track.HUB);
        music.play(Track.DASH);
        music.play(Track.HUB);
        assertEquals(Track.HUB, music.playing());
        assertEquals(List.of(Track.HUB, Track.DASH, Track.HUB), music.started());
        assertEquals(0, music.silences(), "a track is replaced, not stopped first");
    }

    @Test
    void aTrackWithoutItsFilePlaysTheHubInstead() {
        music.lose(Track.SHOOT);
        music.play(Track.SHOOT);
        assertEquals(Track.HUB, music.playing());
        assertEquals(List.of(Track.HUB), music.started());
    }

    @Test
    void theHubGoesOnThroughAZoneWithoutItsFile() {
        music.lose(Track.SHOOT);
        music.play(Track.HUB);
        music.play(Track.SHOOT);
        music.play(Track.HUB);
        assertEquals(List.of(Track.HUB), music.started(), "never started again");
    }

    @Test
    void withoutAnyFileTheMusicIsSilent() {
        music.lose(Track.values());
        music.play(Track.SPLIT);
        music.play(Track.HUB);
        assertNull(music.playing());
        assertEquals(List.of(), music.started());
        assertEquals(0, music.silences());
    }

    @Test
    void aTrackThatCanNoLongerBeReplacedIsSilenced() {
        music.play(Track.SPLIT);
        music.lose(Track.DASH, Track.HUB);
        music.play(Track.DASH);
        assertNull(music.playing());
        assertEquals(1, music.silences());
    }

    @Test
    void stoppingSilencesTheTrackAndItCanStartAgain() {
        music.play(Track.MENU);
        music.stop();
        assertNull(music.playing());
        assertEquals(1, music.silences());
        music.play(Track.MENU);
        assertEquals(List.of(Track.MENU, Track.MENU), music.started());
    }

    @Test
    void stoppingASilentMusicDoesNothing() {
        music.stop();
        assertEquals(0, music.silences());
    }

    @Test
    void theSilentMusicNeverPlays() {
        Music none = Music.none();
        assertDoesNotThrow(() -> {
            none.play(Track.HUB);
            none.play(Track.SPLIT);
            none.setVolume(0.5);
            none.stop();
        });
        assertNull(none.playing());
        assertEquals(0.5, none.volume(), "it still remembers the volume it is given");
    }

    @Test
    void theVolumeStartsAtItsLoudest() {
        assertEquals(1, music.volume());
        assertEquals(List.of(), music.volumes());
    }

    @Test
    void aNewVolumeIsAppliedAtOnce() {
        music.play(Track.HUB);
        music.setVolume(0.4);
        music.setVolume(0.7);
        assertEquals(0.7, music.volume());
        assertEquals(List.of(0.4, 0.7), music.volumes());
        assertEquals(List.of(Track.HUB), music.started(), "the track goes on");
    }

    @Test
    void theVolumeStaysBetweenSilenceAndItsLoudest() {
        music.setVolume(3);
        assertEquals(1, music.volume());
        music.setVolume(-0.5);
        assertEquals(0, music.volume());
        assertEquals(List.of(1.0, 0.0), music.volumes());
    }
}
