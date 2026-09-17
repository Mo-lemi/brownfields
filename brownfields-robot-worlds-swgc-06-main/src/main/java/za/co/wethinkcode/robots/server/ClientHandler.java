package za.co.wethinkcode.robots.server;

import za.co.wethinkcode.robots.server.protocol.Request;

import java.io.*;
import java.net.Socket;

/**
 * Handles communication with a client by reading requests and sending responses.
 * Implements Runnable to support multithreaded client handling.
 */
public class ClientHandler implements Runnable{

    /** The socket used to communicate with the client. */
    private Socket socket;

    /** Reader to receive messages from the client. */
    private BufferedReader bufferedReader;

    /** Writer to send messages to the client. */
    private BufferedWriter bufferedWriter;

    /** Reference to the shared world instance. */
    private World world;

    /**
     * Constructor to initialize the client handler with the given socket and world.
     *
     * @param socket the client socket
     * @param world the world shared across all client sessions
     */
    public ClientHandler(Socket socket, World world){
        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.world = world;

        } catch (IOException e) {
            closeEverything(socket, bufferedReader,bufferedWriter);
        }
    }

    /**
     * Continuously reads messages from the client, processes them, and sends back a response.
     */
    @Override
    public void run() {
        String messageFromClient;
        Request requestHandler = new Request();

        // Keeps reading from client until the connection is closed
        while(socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine();
                // Handle the request and send a response
                if (messageFromClient == null){
                    closeEverything(socket,bufferedReader,bufferedWriter);
                    break;
                }
                String response = requestHandler.handleRequest(messageFromClient, world);
                sendResponse(response);

            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    /**
     * Sends a response to the client through the output stream.
     *
     * @param response the message to send back to the client
     */
    public void sendResponse(String response){
        try {
            bufferedWriter.write(response);
            bufferedWriter.newLine();
            bufferedWriter.flush();

        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }

    }

    /**
     * Closes all input/output resources and the client socket.
     * Ensures clean disconnecting.
     *
     * @param socket the client socket
     * @param bufferedReader the reader connected to the client
     * @param bufferedWriter the writer connected to the client
     */
    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        try {
            if (bufferedReader != null){
                bufferedReader.close();
            }
            if (bufferedWriter != null){
                bufferedWriter.close();
            }
            if (socket != null){
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
