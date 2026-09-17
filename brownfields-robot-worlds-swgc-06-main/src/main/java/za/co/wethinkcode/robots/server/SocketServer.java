package za.co.wethinkcode.robots.server;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;

public class SocketServer {
    private final ServerSocket serverSocket;
    private final World world;

    public SocketServer(ServerSocket serverSocket, World world) {
        this.serverSocket = serverSocket;
        this.world = world;
    }

    public void startServer() {
        try {
            while (!serverSocket.isClosed()) {
                System.out.println("Waiting on client to connect");
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress().getHostAddress());
                ClientHandler clientHandler = new ClientHandler(socket, world);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.out.println("Server Shutdown.");
        }
    }
}