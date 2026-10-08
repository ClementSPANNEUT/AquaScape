package screen;

import input.Gamepad;
import java.util.ArrayList;
import java.util.List;
import save.SaveData;
import world.Progress;

/** A game without a window that records the screens the panels ask for, instead of opening them. */
class RecordingGame extends Game {
    private List<String> calls;

    RecordingGame(SaveData save) {
        super(save);
    }

    RecordingGame(SaveData save, Gamepad gamepad) {
        super(save, gamepad);
    }

    List<String> calls() {
        if (calls == null) {
            calls = new ArrayList<>();
        }
        return calls;
    }

    GamePanel showTheGameScreen() {
        super.playTest();
        return gamePanel();
    }

    @Override
    public void showMenu() {
        calls().add("menu");
        super.showMenu();
    }

    @Override
    public void showSettings() {
        calls().add("settings");
    }

    @Override
    public void showMap() {
        calls().add("map");
    }

    @Override
    public void showControls() {
        calls().add("controls");
    }

    @Override
    public void play(int checkpoint) {
        calls().add("play " + checkpoint);
    }

    @Override
    public void continueGame() {
        calls().add("continue");
    }

    @Override
    public void playTest() {
        calls().add("test");
    }

    @Override
    public void playTraining() {
        calls().add("training");
    }

    @Override
    public void quit() {
        calls().add("quit");
    }

    @Override
    public void saveControls() {
        calls().add("save controls");
        super.saveControls();
    }

    @Override
    public Progress restartProgress(int level, int checkpoint) {
        calls().add("restart " + level + " " + checkpoint);
        return super.restartProgress(level, checkpoint);
    }
}
