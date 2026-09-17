package za.co.wethinkcode.robots.server;

import za.co.wethinkcode.flow.Recorder;
import za.co.wethinkcode.robots.server.database.DatabaseManager;
import za.co.wethinkcode.robots.server.database.SqliteWorldRepository;
import za.co.wethinkcode.robots.webapi.RobotWorldApi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class Server {
    static {
        new Recorder().logRun();
    }

    public static void main(String[] args) throws IOException {
        DatabaseManager.initializeDatabase();

        int port = Integer.parseInt(System.getenv().getOrDefault("ROBOT_WORLD_PORT", "5000"));
        for (int i = 0; i < args.length - 1; i++) {
            if ("-p".equals(args[i])) {
                port = Integer.parseInt(args[i + 1]);
                break;
            }
        }

        World sharedWorld = new World();

        // 1. Start Console Listener
        ConsoleManager console = new ConsoleManager(sharedWorld);
        Thread consoleThread = new Thread(console);
        consoleThread.setDaemon(true);
        consoleThread.start();

        // 2. Start Socket Server
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("0.0.0.0", port));
        SocketServer socketServer = new SocketServer(serverSocket, sharedWorld);

        // 3. Start Javalin Web API (on a distinct port to avoid collisions)
        int apiPort = Integer.parseInt(System.getenv().getOrDefault("ROBOT_WORLD_API_PORT", "7000"));
        if (apiPort == port) {
            apiPort++; // Bump the API port by 1 if it accidentally collides with the socket port
        }
        RobotWorldApi webApi = new RobotWorldApi(sharedWorld, new SqliteWorldRepository());
        webApi.start(apiPort);

        socketServer.startServer();
    }
    // The following initialisation is REQUIRED for `flow` monitoring.
    // DO NOT REMOVE OR MODIFY THIS CODE.
    static {
        new Recorder().logRun();
    }
}