package world;

import java.util.function.Consumer;

/**
 * A level: its name, what the player has to do, the size of its world and how it is laid out.
 * @param name name of the level
 * @param objective what the player has to do, shown in the HUD
 * @param width width of the world, in px
 * @param height height of the world, in px
 * @param layout fills a new world with the level's content
 */
public record Level(String name, String objective, int width, int height, Consumer<World> layout) {
}
