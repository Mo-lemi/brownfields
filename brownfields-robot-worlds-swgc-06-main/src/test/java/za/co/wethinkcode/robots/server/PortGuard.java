package za.co.wethinkcode.robots.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * Test-side guard for the shared acceptance-test port (5000).
 *
 * Every stage of the pipeline (acceptance tests against the reference
 * server and against our own server) binds the same port, and a run that
 * was interrupted before its teardown could complete leaves an orphaned
 * server process still holding the port. The next run then fails with
 * "Address already in use" no matter how correct the server code is.
 *
 * PortGuard makes the harness self-healing: before a server is booted it
 * waits for the port to become free and, as a last resort, kills the
 * java process that still holds it. Only java processes are ever killed;
 * if the port is held by anything else the guard fails loudly instead of
 * killing an unrelated application.
 */
final class PortGuard {

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("win");

    private PortGuard() {
    }

    /**
     * Blocks until the given port can be bound again. If the port is
     * still held after the grace period, the java process(es) listening
     * on it are killed and we wait once more.
     *
     * @param port          the port that must be free
     * @param graceTimeoutMs how long to wait for a port holder to exit on
     *                      its own before killing it (covers the window
     *                      between Process.destroyForcibly() and the OS
     *                      actually releasing the socket)
     * @throws IllegalStateException if the port cannot be freed
     */
    static void ensureFree(int port, long graceTimeoutMs) {
        if (isFree(port)) {
            return;
        }

        // A listener that is already dying (destroyForcibly() is
        // asynchronous) releases the port within moments - give it the
        // grace period before resorting to a kill.
        if (waitUntilFree(port, graceTimeoutMs)) {
            return;
        }

        List<Long> killed = killJavaListenersOnPort(port);
        if (killed.isEmpty()) {
            throw new IllegalStateException(
                    "Port " + port + " is held by a non-java process - free it manually. "
                    + "Run 'netstat -ano | findstr :" + port + "' (Windows) or "
                    + "'lsof -i :" + port + "' (Linux/macOS) to identify it.");
        }
        System.out.println("PortGuard: killed orphaned server process(es) " + killed
                + " holding port " + port);

        if (!waitUntilFree(port, graceTimeoutMs)) {
            throw new IllegalStateException(
                    "Port " + port + " is still not free after killing " + killed);
        }
    }

    /**
     * @return true if a ServerSocket can bind the port right now. The probe
     * deliberately does NOT set SO_REUSEADDR - on Windows that option lets
     * the bind succeed even while another process holds the port, which
     * would report a false "free".
     */
    static boolean isFree(int port) {
        try (ServerSocket probe = new ServerSocket()) {
            probe.bind(new InetSocketAddress(port));
            return true;
        } catch (Exception busy) {
            return false;
        }
    }

    /** Polls isFree until the port is free or the timeout expires. */
    static boolean waitUntilFree(int port, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (isFree(port)) {
                return true;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return isFree(port);
    }

    /**
     * Kills every process listening on the given port, but ONLY if it is a
     * java process (the reference/robot-world servers are the only things
     * in this pipeline that should ever hold the port).
     *
     * @return the PIDs that were killed; empty when the holder is not a
     *         java process (in which case nothing is killed)
     */
    static List<Long> killJavaListenersOnPort(int port) {
        List<Long> killed = new ArrayList<>();
        for (long pid : listeningPids(port)) {
            String image = imageOf(pid);
            if (image != null && image.toLowerCase().startsWith("java")) {
                killTree(pid);
                killed.add(pid);
            }
        }
        return killed;
    }

    /** Kills a process and, on Windows, its whole child tree. */
    static void killTree(long pid) {
        if (IS_WINDOWS) {
            run(new String[]{"taskkill", "/F", "/T", "/PID", Long.toString(pid)});
        } else {
            run(new String[]{"kill", "-9", Long.toString(pid)});
        }
    }

    /** Kills a process and its child tree. */
    static void killTree(Process process) {
        killTree(process.pid());
    }

    /**
     * @return the PIDs of every process currently LISTENING on the port.
     */
    private static List<Long> listeningPids(int port) {
        return IS_WINDOWS ? getWindowsPids(port) : getUnixPids(port);
    }

    private static List<Long> getWindowsPids(int port) {
        List<Long> pids = new ArrayList<>();
        // netstat -ano lines:  TCP  0.0.0.0:5000  0.0.0.0:0  LISTENING  18528
        for (String line : runAndRead(new String[]{"netstat", "-ano", "-p", "tcp"})) {
            String[] tokens = line.trim().split("\\s+");

            // Guard clauses: skip lines that don't match exactly what we want
            if (tokens.length != 5) continue;
            if (!"TCP".equalsIgnoreCase(tokens[0])) continue;
            if (!"LISTENING".equalsIgnoreCase(tokens[3])) continue;
            if (portOf(tokens[1]) != port) continue;

            try {
                pids.add(Long.parseLong(tokens[4]));
            } catch (NumberFormatException ignore) {
                // header or malformed line
            }
        }
        return pids;
    }

    private static List<Long> getUnixPids(int port) {
        List<Long> pids = new ArrayList<>();
        for (String line : runAndRead(new String[]{"lsof", "-t", "-iTCP:" + port, "-sTCP:LISTEN"})) {
            try {
                pids.add(Long.parseLong(line.trim()));
            } catch (NumberFormatException ignore) {
                // lsof not installed or no listener
            }
        }
        return pids;
    }

    /** Extracts the port number from a netstat local/remote address. */
    private static int portOf(String address) {
        int colon = address.lastIndexOf(':');
        if (colon < 0) {
            return -1;
        }
        try {
            return Integer.parseInt(address.substring(colon + 1).replace("]", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * @return the image (executable) name of the process, or null when the
     *         process no longer exists or its name cannot be determined.
     */
    private static String imageOf(long pid) {
        List<String> lines;
        if (IS_WINDOWS) {
            lines = runAndRead(new String[]{
                    "tasklist", "/FO", "CSV", "/NH", "/FI", "PID eq " + pid});
        } else {
            lines = runAndRead(new String[]{"ps", "-p", Long.toString(pid), "-o", "comm="});
        }
        for (String line : lines) {
            String name = line.trim();
            if (name.startsWith("\"")) {
                String[] fields = name.split("\"");
                name = fields.length > 1 ? fields[1] : "";
            }
            return name.isEmpty() ? null : name;
        }
        return null;
    }

    private static void run(String[] command) {
        try {
            new ProcessBuilder(command).start().waitFor();
        } catch (Exception e) {
            System.err.println("PortGuard: '" + String.join(" ", command) + "' failed: " + e);
        }
    }

    private static List<String> runAndRead(String[] command) {
        List<String> lines = new ArrayList<>();
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("PortGuard: '" + String.join(" ", command) + "' failed: " + e);
        }
        return lines;
    }
}
