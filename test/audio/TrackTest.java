package audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import zone.DashTraining;
import zone.DashZone;
import zone.FinZone;
import zone.HubZone;
import zone.LightZone;
import zone.ParyZone;
import zone.ShootZone;
import zone.SplitZone;

class TrackTest {
    @Test
    void everyTrackHasItsOwnFile() {
        Set<String> files = new HashSet<>();
        for (Track track : Track.values()) {
            assertTrue(track.file().endsWith(".mp3"), track.file());
            assertTrue(files.add(track.file()), track.file() + " is used twice");
        }
    }

    @Test
    void theLevelsAreNumberedClockwiseFromSplit() {
        assertEquals("Level1.mp3", Track.ofZone(SplitZone.PLAN.name()).file());
        assertEquals("Level2.mp3", Track.ofZone(DashZone.PLAN.name()).file());
        assertEquals("Level3.mp3", Track.ofZone(ShootZone.PLAN.name()).file());
        assertEquals("Level4.mp3", Track.ofZone(ParyZone.PLAN.name()).file());
        assertEquals("Level5.mp3", Track.ofZone(LightZone.PLAN.name()).file());
    }

    @Test
    void theOtherPlacesHaveTheirTrackToo() {
        assertEquals(Track.HUB, Track.ofZone(HubZone.PLAN.name()));
        assertEquals(Track.END, Track.ofZone(FinZone.PLAN.name()));
        assertEquals(Track.TRAINING, Track.ofZone(DashTraining.PLAN.name()));
        assertEquals("Hub.mp3", Track.HUB.file());
        assertEquals("Final.mp3", Track.END.file());
        assertEquals("Trainning.mp3", Track.TRAINING.file());
    }

    @Test
    void aPlaceWithoutATrackGetsTheHub() {
        assertEquals(Track.HUB, Track.ofZone("Salle de test"));
        assertEquals(Track.HUB, Track.ofZone(null));
    }

    @Test
    void theMenusBelongToNoZone() {
        assertNull(Track.MENU.zone());
        assertEquals("Debut.mp3", Track.MENU.file());
    }
}
