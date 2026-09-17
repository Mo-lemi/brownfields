package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SocketServer: real sockets, real threads - the
 * full network round trip from a raw TCP client to the JSON answer.
 */
public class TestSocketServerIntegration {

    private ServerSocket serverSocket;
    private Thread serverThread;
    private World world;

    @BeforeEach
    void setUp() throws IOException {
        world = new World(20, 20);
        serverSocket = new ServerSocket(0); // a free port for this test
        SocketServer socketServer = new SocketServer(serverSocket, world);
        serverThread = new Thread(socketServer::startServer);
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        serverSocket.close(); // the accept loop exits, the thread ends
    }

    /** Opens a client connection and returns a line-reader/writer pair. */
    private SocketConnection connect() throws IOException {
        Socket socket = new Socket("127.0.0.1", serverSocket.getLocalPort());
        socket.setSoTimeout(5000);
        return new SocketConnection(socket);
    }

    private static class SocketConnection {
        private final Socket socket;
        private final BufferedReader reader;
        private final PrintWriter writer;

        SocketConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.socket.setSoTimeout(5000);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        }

        String send(String json) throws IOException {
            writer.println(json);
            return reader.readLine();
        }

        void close() throws IOException {
            socket.close();
        }
    }

    @Test
    void malformedJsonGetsAProtocolErrorAnswer() throws IOException {
        SocketConnection connection = connect();
        String response = connection.send("this is not json");
        assertNotNull(response);
        assertTrue(response.contains("ERROR"), response);
        assertTrue(response.contains("Could not parse arguments"), response);
        connection.close();
    }

    @Test
    void launchOverSocketsReturnsOk() throws IOException {
        SocketConnection connection = connect();
        String response = connection.send(
                "{\"robot\": \"hal\", \"command\": \"launch\", \"arguments\": [\"soldier\"]}");
        assertNotNull(response);
        assertTrue(response.contains("\"result\": \"OK\"")
                || response.contains("\"result\":\"OK\""), response);
        assertNotNull(world.getARobot("hal"), "the robot exists in the shared world");
        connection.close();
    }

    @Test
    void stateForAMissingRobotReturnsAnError() throws IOException {
        SocketConnection connection = connect();
        String response = connection.send(
                "{\"robot\": \"ghost\", \"command\": \"state\", \"arguments\": []}");
        assertTrue(response.contains("ERROR"), response);
        assertTrue(response.contains("Robot does not exist"), response);
        connection.close();
    }

    @Test
    void oneConnectionServesManyCommandsInSequence() throws IOException {
        SocketConnection connection = connect();
        connection.send("{\"robot\": \"hal\", \"command\": \"launch\", \"arguments\": [\"soldier\"]}");
        String state = connection.send(
                "{\"robot\": \"hal\", \"command\": \"state\", \"arguments\": []}");
        assertTrue(state.contains("\"OK\"") || state.contains("OK"), state);
        String forward = connection.send(
                "{\"robot\": \"hal\", \"command\": \"forward\", \"arguments\": [1]}");
        assertTrue(forward.contains("OK"), forward);
        assertEquals(0, world.getARobot("hal").getX());
        assertEquals(1, world.getARobot("hal").getY(), "the robot moved north");
        connection.close();
    }

    @Test
    void closingTheServerSocketEndsTheAcceptLoop() throws IOException, InterruptedException {
        assertTrue(serverThread.isAlive());
        serverSocket.close();
        serverThread.join(2000);
        assertFalse(serverThread.isAlive(), "the accept loop ends when the socket closes");
    }
}
