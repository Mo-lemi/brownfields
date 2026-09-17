package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public abstract class AbstractAcceptanceTest {
    protected final static int DEFAULT_PORT = Integer.getInteger("test.port", 5000);
    // 127.0.0.1, NOT "localhost": on Windows, localhost resolves to ::1
    // first, and Docker Desktop's port forward can serve ::1 while the
    // IPv4 path works (or vice versa). Pinning IPv4 makes the harness
    // independent of the machine's IPv6/Docker forwarding quirks.
    protected final static String DEFAULT_IP = "127.0.0.1";

    protected RobotClient serverClient;
    private ServerSocket testServerSocket;
    private Process serverProcess;

    @BeforeEach
    void setUpFreshServerAndClient() throws IOException, InterruptedException {
        // Check if the developer passed the specific Maven command flag
        boolean useReference = Boolean.getBoolean("useRefServer");
        boolean useDocker = Boolean.getBoolean("dockerServer");

        // Port 5000 is shared by every stage of the pipeline. A previous
        // run that was interrupted before its teardown completed (Ctrl+C,
        // a killed surefire fork, the flow recorder's System.exit, or a
        // teardown exception) leaves an orphaned server holding the port;
        // clear it before booting anything. In docker mode nothing is
        // booted here - the port is legitimately held by Docker's port
        // forwarder, so the ownership check would always misfire.
        if (useDocker) {
            startDockerServer();
        } else {
            PortGuard.ensureFree(DEFAULT_PORT, 5_000);
            if (useReference) {
                // EXTERNAL MODE: Spin up the reference server JAR in a separate process
                startReferenceServer();
            } else {
                // CI/CD MODE: Spin up YOUR server in a background thread
                startOwnServer();
            }
        }

        // Connect the test client
        serverClient = new RobotClient();
        serverClient.connect(DEFAULT_IP, DEFAULT_PORT);
    }

    private void startDockerServer() {
        // The container serves every test in the class, so restart it
        // to drop robots/world state left by the previous scenario -
        // Server.main builds a fresh World() at boot. The name matches
        // the container the Makefile starts (make docker-run) and can
        // be overridden with -Ddocker.container=<name>.
        restartDockerContainer();
        if (!waitUntilServerUp(15_000, true)) {
            throw new IllegalStateException("Docker server did not accept connections on port " + DEFAULT_PORT);
        }
    }

    private void startReferenceServer() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                "java", "-jar", "reference-server-0.2.3.jar", "-p", "5000", "-s", "2", "-o", "0,1"
        );
        // The child's stdout/stderr go to pipes nobody drains - once the
        // pipe buffer fills the server blocks mid-write. Log to a file
        // instead so it can never stall and boot problems are inspectable.
        Path log = Paths.get("target", "reference-server.log");
        if (log.getParent() != null) {
            Files.createDirectories(log.getParent());
        }
        pb.redirectErrorStream(true).redirectOutput(log.toFile());
        serverProcess = pb.start();

        // Wait until the server actually accepts connections - a blind
        // sleep(2000) failed on slower machines and passed by luck on fast ones.
        if (!waitUntilServerUp(10_000, false)) {
            throw new IllegalStateException(
                    "Reference server did not accept connections on port " + DEFAULT_PORT
                            + " within 10s - see target/reference-server.log");
        }
    }

    private void startOwnServer() throws IOException {
        System.setProperty("width", "3");
        System.setProperty("height", "3");
        System.setProperty("obstacles", "0,1");

        testServerSocket = new ServerSocket();
        testServerSocket.bind(new InetSocketAddress(DEFAULT_IP, DEFAULT_PORT));

        // Change these two lines:
        SocketServer server = new SocketServer(testServerSocket, new World());
        Thread serverThread = new Thread(server::startServer);

        serverThread.setDaemon(true);
        serverThread.start();

        if (!waitUntilServerUp(5_000, false)) {
            throw new IllegalStateException("Own server did not accept connections on port " + DEFAULT_PORT);
        }
    }

    /**
     * Restarts the container under test. Failures are tolerated with a
     * warning: the wait probe below reports the real problem if the
     * server is not reachable afterwards.
     */
    private void restartDockerContainer() {
        String container = System.getProperty("docker.container", "robot-worlds-test");
        Path log = Paths.get("target", "docker-restart.log");

        try {
            if (log.getParent() != null) {
                Files.createDirectories(log.getParent());
            }
            Process process = new ProcessBuilder("docker", "restart", container)
                    .redirectErrorStream(true)
                    .redirectOutput(log.toFile())
                    .start();

            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                System.err.println("WARNING: docker restart " + container + " timed out");
            }
        } catch (IOException e) {
            System.err.println("WARNING: could not run docker (is the container running?): " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Repeatedly probes the port until the server is up.
     *
     * With requireResponse=false a bare TCP connect is accepted as proof -
     * correct for the local modes (own in-process server, reference server
     * process) where the socket is backed by a real listener immediately.
     *
     * With requireResponse=true (docker mode) a connect is NOT enough:
     * Docker's host-side proxy accepts connections before the container's
     * server is listening, so early requests get reset. The probe then
     * sends a request line and requires a response line back - the docker
     * server answers any input with a JSON response line.
     */
    private boolean waitUntilServerUp(long timeoutMs, boolean requireResponse) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try (Socket probe = new Socket()) {
                probe.connect(new InetSocketAddress(DEFAULT_IP, DEFAULT_PORT), 250);
                if (!requireResponse) return true;

                probe.setSoTimeout(1_000);
                probe.getOutputStream().write("{}\n".getBytes(StandardCharsets.UTF_8));
                probe.getOutputStream().flush();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(probe.getInputStream(), StandardCharsets.UTF_8));
                if (reader.readLine() != null) return true;
            } catch (IOException notUpYet) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    @AfterEach
    void tearDownServerAndClient() {
        // Every cleanup step below is
        // independent on purpose: this method used to run them in
        // sequence, so a disconnect that threw (broken connection at
        // the end of a test) skipped the kill of the reference server
        // and left it running as an orphan holding port 5000 - the
        // next run then failed to bind.
        disconnectClient();
        closeOwnServer();
        destroyReferenceServer();
    }

    private void disconnectClient() {
        // 1. Disconnect the test client.
        try {
            if (serverClient != null && serverClient.isConnected()) {
                serverClient.disconnect();
            }
        } catch (RuntimeException e) {
            System.err.println("tearDown: could not disconnect test client: " + e.getMessage());
        }
    }

    private void closeOwnServer() {
        // 2. Shut down our internal server if it was used
        try {
            if (testServerSocket != null && !testServerSocket.isClosed()) {
                testServerSocket.close();
            }
        } catch (IOException e) {
            System.err.println("tearDown: could not close server socket: " + e.getMessage());
        }
    }

    private void destroyReferenceServer() {
        // 3. Kill the reference server process if it was used - and wait
        //    for it to actually die so the port is released before the
        //    next test (destroyForcibly alone is asynchronous).
        if (serverProcess == null) return;

        Process process = serverProcess;
        serverProcess = null;

        try {
            process.destroy(); // polite SIGTERM first
            if (process.waitFor(2, TimeUnit.SECONDS)) return;

            process.destroyForcibly();
            if (process.waitFor(3, TimeUnit.SECONDS)) return;

            // Last resort: kill the whole tree (on Windows a child
            // may have spawned grandchildren that outlive the JVM kill).
            PortGuard.killTree(process);
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }

        if (process.isAlive()) {
            System.err.println("tearDown: WARNING - reference server pid "
                    + process.pid() + " is still alive and may hold port " + DEFAULT_PORT);
        }
    }
}