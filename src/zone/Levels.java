package zone;

import java.util.List;
import world.Level;
import world.Tunnel;
import world.World;

/**
 * The levels of the game: the whole world of Aquascape, a test room and the training room of the dash. The
 * tunnels are built once and shared by every {@link World} made from them.
 */
public final class Levels {
    /** Index of the Aquascape world. */
    public static final int WORLD = 0;
    /** Index of the test room. */
    public static final int TEST = 1;
    /** Index of the training room of the dash. */
    public static final int TRAINING = 2;
    private static final int ROOM_SIZE = 3_000;

    private static final List<Level> ALL = List.of(
        new Level("Aquascape",
                "Rapporte les lumières de Split, Dash, Shoot, Pary et de la Lumière : la porte dorée de Fin s'ouvrira.",
                HubZone.WORLD_WIDTH, HubZone.WORLD_HEIGHT, Levels::layOutWorld),

        new Level("Test", "ceci est un test", ROOM_SIZE, ROOM_SIZE, Levels::layOutRoom),

        new Level("Entraînement : Dash",
                "Entraîne-toi au dash : tends la bulle pour passer les spinners et ouvrir les deux portes, jusqu'à la sortie.",
                DashTraining.WIDTH, DashTraining.HEIGHT, Levels::layOutTraining)
    );

    private static Tunnel worldTunnel;
    private static Tunnel trainingTunnel;

    private Levels() {
    }

    /** {@return how many levels there are} */
    public static int count() {
        return ALL.size();
    }

    /**
     * Gives a level.
     * @param index index of the level
     * @return the level
     */
    public static Level get(int index) {
        return ALL.get(index);
    }

    private static Tunnel worldTunnel() {
        if (worldTunnel == null) {
            worldTunnel = new Tunnel();
            HubZone.carve(worldTunnel);
            LightZone.carve(worldTunnel);
            SplitZone.carve(worldTunnel);
            DashZone.carve(worldTunnel);
            ShootZone.carve(worldTunnel);
            ParyZone.carve(worldTunnel);
            FinZone.carve(worldTunnel);
        }
        return worldTunnel;
    }

    private static void layOutWorld(World world) {
        world.setTunnel(worldTunnel());
        HubZone.layOut(world);
        world.setArea("Lumière");
        LightZone.layOut(world);
        world.setArea("Split");
        SplitZone.layOut(world);
        world.setArea("Dash");
        DashZone.layOut(world);
        world.setArea("Shoot");
        ShootZone.layOut(world);
        world.setArea("Pary");
        ParyZone.layOut(world);
        world.setArea("Fin");
        FinZone.layOut(world);
    }

    private static void layOutTraining(World world) {
        if (trainingTunnel == null) {
            trainingTunnel = new Tunnel();
            DashTraining.carve(trainingTunnel);
        }
        world.setTunnel(trainingTunnel);
        world.setArea("Entraînement");
        DashTraining.layOut(world);
    }

    private static void layOutRoom(World world) {
        world.addDroplet(ROOM_SIZE / 2.0, ROOM_SIZE / 2.0, 1);
    }
}
