/**
 * The game controllers: an abstract {@link input.Gamepad} read once a frame, which tells its
 * {@link input.GamepadListener listeners} the buttons pressed, and the real controllers through the Jamepad library
 * (SDL). Each screen of the game is a listener and handles the buttons itself.
 */
package input;
