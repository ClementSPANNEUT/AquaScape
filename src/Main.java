import javax.swing.SwingUtilities;
import screen.Game;

/** Entry point of Aquascape: opens the game window on the Swing event thread. */
public class Main {
    private Main() {
    }

    /**
     * Starts the game.
     * @param args unused
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Game::new);
    }
}
