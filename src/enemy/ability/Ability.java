package enemy.ability;

import java.util.List;

/**
 * An ability the player learns by bringing back the light of a zone. Owning the light means owning the ability,
 * so the save needs nothing more. What each ability does is in its own class: {@link Dash}, {@link Slide} and
 * {@link Shoot}.
 */
public enum Ability {
    /**
     * The elastic dash, learnt by bringing back the light of Split: the back of the drop sticks to the ground, its
     * head stretches away and the drop is thrown when it is let go, or tears in two when it is pulled too far.
     */
    DASH("Dash", "Split", List.of(
            "Maintiens la touche : l'arrière de la bulle se fige au sol.",
            "Pousse la tête dans une direction : la bulle se tend comme un élastique.",
            "Relâche : elle part d'un coup dans cette direction, plus fort si elle est bien tendue.",
            "Si tu tires trop, elle casse en deux : tu choisis alors la moitié que tu diriges.",
            "L'arrière figé appuie aussi sur les boutons des portes.")),
    /**
     * The slide, learnt by bringing back the light of Dash: a press makes the drop keep the speed and the
     * direction it has, with no key held, and another press gives the control back.
     */
    SLIDE("Slide", "Dash", List.of(
            "Appuie une fois : la bulle garde sa vitesse et sa direction, sans rien maintenir.",
            "Tant qu'elle file ainsi, elle ne ralentit plus et les touches ne la dévient pas.",
            "Appuie de nouveau pour reprendre la main : elle se remet à ralentir.",
            "Lance-toi d'abord : une bulle à l'arrêt n'a pas de direction à garder.",
            "Après un dash, elle garde toute la vitesse du lancer.")),
    /**
     * The charged shot, learnt by bringing back the light of Shoot: the longer the key is held, the stronger the
     * ball fired when it is let go, and the drop blows up if the charge goes past its maximum.
     */
    SHOOT("Shoot", "Shoot", List.of(
            "Maintiens la touche de tir : la puissance monte tant que tu restes appuyé.",
            "Tourne la flèche de visée, un cran à la fois, puis relâche : la balle part dans ce sens.",
            "Plus la puissance est forte, plus la balle va vite et loin.",
            "Attention : si la jauge dépasse le maximum, la bulle explose.",
            "La balle s'arrête sur les murs et les ennemis, et appuie sur les boutons qu'elle touche."));

    private final String label;
    private final String light;
    private final List<String> description;

    Ability(String label, String light, List<String> description) {
        this.label = label;
        this.light = light;
        this.description = description;
    }

    /** {@return the name of the ability shown to the player} */
    public String label() {
        return label;
    }

    /** {@return the name of the light that teaches it} */
    public String light() {
        return light;
    }

    /** {@return what the ability does, one sentence per line, shown when it is learnt} */
    public List<String> description() {
        return description;
    }

    /**
     * Finds the ability a light teaches.
     * @param light name of the light
     * @return the ability, or {@code null} if the light teaches none
     */
    public static Ability unlockedBy(String light) {
        for (Ability ability : values()) {
            if (ability.light.equals(light)) {
                return ability;
            }
        }
        return null;
    }
}
