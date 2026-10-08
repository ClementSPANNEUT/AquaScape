/**
 * The abilities of the player's drop: {@link enemy.ability.Ability} lists them and the light that teaches each,
 * {@link enemy.ability.Dash}, {@link enemy.ability.Slide} and {@link enemy.ability.Shoot} say what they do, and
 * {@link enemy.ability.Shot} is the ball the last one fires.
 * <p>Like the enemies, they only see the world through {@link enemy.Arena}, and hit its objects through
 * {@link enemy.ability.Target}, so they don't depend on the rest of the game.
 */
package enemy.ability;
