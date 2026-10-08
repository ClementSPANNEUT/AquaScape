import com.sun.jdi.Bootstrap;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.ToolProvider;

/**
 * Development loop of Aquascape, started by {@code dev.ps1} or {@code dev.sh} from the root of the project: it
 * builds and starts the game, then watches the sources and brings every saved change into the game.
 *
 * <p>A change that only touches the body of methods is swapped into the running game, which keeps its state: the
 * new code runs from the next call on. What was already built with the old code stays as it was, such as a world
 * already laid out or the tunnels, which are carved once. A deeper change, such as a new field or method, cannot
 * be swapped: the game is then closed and started again. With {@code --restart} the game is started again on every
 * change, and pressing Enter starts it again at any time.
 *
 * <p>When the code does not compile, the errors are printed and the running game keeps the code it had.
 */
public final class DevLoop {
    private static final Path SOURCES = Path.of("src");
    private static final Path CLASSES = Path.of("out");
    private static final Path LIBRARIES = Path.of("lib");
    private static final String MAIN_CLASS = "Main";
    private static final String RESTART_OPTION = "--restart";
    private static final String JAVA = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    private static final String LOCALHOST = "127.0.0.1";
    private static final String DEBUG_AGENT = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,quiet=y,address=";
    private static final int POLL_MS = 250;
    private static final int SETTLE_MS = 150;
    private static final int ATTACH_TRIES = 50;
    private static final int ATTACH_WAIT_MS = 100;

    private final boolean alwaysRestart;
    private final AtomicBoolean restartAsked = new AtomicBoolean();
    private volatile Process game;
    private VirtualMachine debugger;
    private Map<String, byte[]> running = Map.of();

    private DevLoop(boolean alwaysRestart) {
        this.alwaysRestart = alwaysRestart;
    }

    /**
     * Runs the development loop until it is interrupted.
     * @param args {@code --restart} to start the game again on every change instead of swapping the code
     * @throws IOException if the sources or the compiled classes cannot be read, or the game cannot be started
     * @throws InterruptedException if the loop is interrupted while waiting
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        new DevLoop(List.of(args).contains(RESTART_OPTION)).run();
    }

    private void run() throws IOException, InterruptedException {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        listenForEnter();
        say("Mode développement : chaque fichier enregistré dans src/ met le jeu à jour.");
        say("Entrée : relancer le jeu. Ctrl+C : quitter.");
        Map<Path, Long> sources = stamps();
        build();
        while (true) {
            Thread.sleep(POLL_MS);
            if (game != null && !game.isAlive()) {
                game = null;
                say("Jeu fermé : il sera relancé à la prochaine modification, ou avec Entrée.");
            }
            Map<Path, Long> now = stamps();
            if (!now.equals(sources)) {
                sources = settled(now);
                restartAsked.set(false);
                build();
            } else if (restartAsked.getAndSet(false)) {
                restart();
            }
        }
    }

    private void build() throws IOException, InterruptedException {
        if (!compile()) {
            say("Erreur de compilation : "
                    + (isRunning() ? "le jeu garde l'ancien code." : "le jeu démarrera une fois le code corrigé."));
            return;
        }
        Map<String, byte[]> compiled = classes();
        Map<String, byte[]> changed = changes(compiled);
        running = compiled;
        if (!isRunning()) {
            launch();
        } else if (changed.isEmpty()) {
            say("Rien à mettre à jour.");
        } else if (!alwaysRestart && hotSwap(changed)) {
            say("Mis à jour sans relancer : " + String.join(", ", new TreeSet<>(changed.keySet())));
        } else {
            restart();
        }
    }

    private boolean compile() throws IOException {
        return ToolProvider.getSystemJavaCompiler().run(null, null, null, "-encoding", "UTF-8",
                "-d", CLASSES.toString(), "-cp", libraries(), "-sourcepath", SOURCES.toString(),
                SOURCES.resolve(MAIN_CLASS + ".java").toString()) == 0;
    }

    private Map<String, byte[]> changes(Map<String, byte[]> compiled) {
        Map<String, byte[]> changed = new HashMap<>();
        compiled.forEach((name, bytes) -> {
            byte[] before = running.get(name);
            if (before != null && !Arrays.equals(before, bytes)) {
                changed.put(name, bytes);
            }
        });
        return changed;
    }

    private boolean hotSwap(Map<String, byte[]> changed) {
        if (debugger == null) {
            return false;
        }
        try {
            Map<ReferenceType, byte[]> loaded = new HashMap<>();
            changed.forEach((name, bytes) -> debugger.classesByName(name).forEach(type -> loaded.put(type, bytes)));
            if (!loaded.isEmpty()) {
                debugger.redefineClasses(loaded);
            }
            return true;
        } catch (RuntimeException | LinkageError e) {
            say("Changement trop profond pour le jeu en cours (" + e.getMessage() + ").");
            return false;
        }
    }

    private void restart() throws IOException, InterruptedException {
        Process process = game;
        if (process != null) {
            process.destroy();
            process.waitFor();
        }
        launch();
    }

    private void launch() throws IOException, InterruptedException {
        say("Lancement du jeu.");
        int port = freePort();
        List<String> command = new ArrayList<>(List.of(JAVA, "--enable-native-access=ALL-UNNAMED"));
        if (!alwaysRestart) {
            command.add(DEBUG_AGENT + LOCALHOST + ":" + port);
        }
        command.addAll(List.of("-cp", CLASSES + File.pathSeparator + libraries(), MAIN_CLASS));
        game = new ProcessBuilder(command).redirectOutput(Redirect.INHERIT).redirectError(Redirect.INHERIT).start();
        debugger = alwaysRestart ? null : attach(port);
        if (!alwaysRestart && debugger == null && isRunning()) {
            say("Mise à jour à chaud indisponible : le jeu sera relancé à chaque modification.");
        }
    }

    private VirtualMachine attach(int port) throws InterruptedException {
        AttachingConnector connector = Bootstrap.virtualMachineManager().attachingConnectors().stream()
                .filter(candidate -> candidate.transport().name().equals("dt_socket")).findFirst().orElse(null);
        if (connector == null) {
            return null;
        }
        Map<String, Connector.Argument> arguments = connector.defaultArguments();
        arguments.get("hostname").setValue(LOCALHOST);
        arguments.get("port").setValue(String.valueOf(port));
        for (int attempt = 0; attempt < ATTACH_TRIES && isRunning(); attempt++) {
            try {
                return connector.attach(arguments);
            } catch (IOException e) {
                Thread.sleep(ATTACH_WAIT_MS);
            } catch (IllegalConnectorArgumentsException e) {
                return null;
            }
        }
        return null;
    }

    private void stop() {
        Process process = game;
        if (process != null) {
            process.destroy();
        }
    }

    private boolean isRunning() {
        Process process = game;
        return process != null && process.isAlive();
    }

    private void listenForEnter() {
        Thread listener = new Thread(() -> new BufferedReader(new InputStreamReader(System.in)).lines()
                .forEach(line -> restartAsked.set(true)));
        listener.setDaemon(true);
        listener.start();
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName(LOCALHOST))) {
            return socket.getLocalPort();
        }
    }

    private static String libraries() throws IOException {
        try (Stream<Path> files = Files.list(LIBRARIES)) {
            return files.map(Path::toString).filter(file -> file.endsWith(".jar")).sorted()
                    .collect(Collectors.joining(File.pathSeparator));
        }
    }

    private static Map<Path, Long> stamps() {
        try (Stream<Path> files = Files.walk(SOURCES)) {
            return files.filter(file -> file.toString().endsWith(".java"))
                    .collect(Collectors.toMap(file -> file, file -> file.toFile().lastModified()));
        } catch (IOException | UncheckedIOException e) {
            return Map.of();
        }
    }

    private static Map<Path, Long> settled(Map<Path, Long> seen) throws InterruptedException {
        Map<Path, Long> last = seen;
        while (true) {
            Thread.sleep(SETTLE_MS);
            Map<Path, Long> now = stamps();
            if (now.equals(last)) {
                return now;
            }
            last = now;
        }
    }

    private static Map<String, byte[]> classes() throws IOException {
        Map<String, byte[]> classes = new HashMap<>();
        try (Stream<Path> files = Files.walk(CLASSES)) {
            for (Path file : files.filter(candidate -> candidate.toString().endsWith(".class")).toList()) {
                String path = CLASSES.relativize(file).toString();
                String name = path.substring(0, path.length() - ".class".length()).replace(File.separatorChar, '.');
                classes.put(name, Files.readAllBytes(file));
            }
        }
        return classes;
    }

    private static void say(String message) {
        System.out.println("[dev] " + message);
    }
}
