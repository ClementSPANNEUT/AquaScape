package enemy.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import world.Progress;

class AbilityTest {
    @Test
    void theLightOfSplitTeachesTheDash() {
        assertEquals(Ability.DASH, Ability.unlockedBy("Split"));
        assertEquals("Dash", Ability.DASH.label());
        assertEquals("Split", Ability.DASH.light());
    }

    @Test
    void theLightOfShootTeachesTheShoot() {
        assertEquals(Ability.SHOOT, Ability.unlockedBy("Shoot"));
        assertEquals("Shoot", Ability.SHOOT.label());
        assertEquals("Shoot", Ability.SHOOT.light());
    }

    @Test
    void theLightOfDashTeachesTheSlide() {
        assertEquals(Ability.SLIDE, Ability.unlockedBy("Dash"));
        assertEquals("Slide", Ability.SLIDE.label());
        assertEquals("Dash", Ability.SLIDE.light());
    }

    @Test
    void otherLightsTeachNothingYet() {
        assertNull(Ability.unlockedBy("Lumière"));
        assertNull(Ability.unlockedBy("Pary"), "the light of Pary teaches nothing yet");
        assertNull(Ability.unlockedBy("Sortie"));
    }

    @Test
    void everyAbilityIsExplainedToThePlayer() {
        for (Ability ability : Ability.values()) {
            assertFalse(ability.description().isEmpty(), ability.name());
            for (String line : ability.description()) {
                assertFalse(line.isBlank());
            }
        }
    }

    @Test
    void anAbilityIsLearntWithItsLight() {
        Progress progress = new Progress();
        assertFalse(progress.hasAbility(Ability.DASH));
        progress.obtainLight("Lumière");
        assertFalse(progress.hasAbility(Ability.DASH));
        progress.obtainLight("Split");
        assertTrue(progress.hasAbility(Ability.DASH));
        assertFalse(progress.hasAbility(Ability.SHOOT), "each ability has its own light");
        assertFalse(progress.hasAbility(Ability.SLIDE));
        progress.obtainLight("Dash");
        assertTrue(progress.hasAbility(Ability.SLIDE), "finishing the Dash zone teaches the slide");
        assertFalse(progress.hasAbility(Ability.SHOOT));
        progress.obtainLight("Shoot");
        assertTrue(progress.hasAbility(Ability.SHOOT));
    }
}
