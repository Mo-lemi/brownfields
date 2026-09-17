package za.co.wethinkcode.robots.client;

import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * The Client class connects to a robot server and allows a user to send commands
 * to control a robot in the game world.
 */
public class Client {

    /** The socket used to connect to the server. */
    private Socket socket;

    /** Writer used to send data to the server. */
    private BufferedWriter bufferedWriter;

    /** Reader used to receive data from the server. */
    private BufferedReader bufferedReader;

    /** Validates and formats commands into JSON format. */
    private ValidateCommand commandValidator;

    /** Handles parsing and displaying server responses. */
    private ResponseHandler responseHandler;

    /**
     * Creates a client and connects it to the server,
     * and utility classes for command validation and response handling.
     *
     * @param socket the socket used for communication
     * @param robotName the name of the robot this client controls
     */
    public Client (Socket socket, String robotName){
        try {
            this.socket = socket;
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            commandValidator = new ValidateCommand(robotName);
            responseHandler = new ResponseHandler();

        } catch (IOException e){
            closeClient(socket, bufferedReader, bufferedWriter);
        }

    }

    /**
     * Checks if the robot has died (launched but no longer exists on the server).
     * If so, closes the client.
     */
    private void checkIfRobotDied(JSONObject responseJSON) {
        if ("ERROR".equals(responseJSON.optString("result"))) {
            String message = responseJSON.getJSONObject("data").getString("message");
            if (message.contains("does not exist")) {
                if (!commandValidator.isEmptyRobotName()) {
                    System.out.println("\nYour robot has died");
                    closeClient(socket, bufferedReader, bufferedWriter);
                }
            }
        }
    }

    /**
     * Starts an input loop where the user can type commands, which are
     * validated, formatted, and sent to the server.
     * Server responses are printed via the ResponseHandler.
     */
    public void sendCommands() {
        try {
            Scanner scanner = new Scanner(System.in);
            while (socket.isConnected()) {
                processSingleCommand(scanner); // Offload the heavy lifting
            }
        } catch (IOException e) {
            closeClient(socket, bufferedReader, bufferedWriter);
        }
    }

    private void processSingleCommand(Scanner scanner) throws IOException {
        System.out.print("\n> ");
        String messageToSentToServer = scanner.nextLine();
        System.out.println();

        String jsonCommand = commandValidator.handleCommand(messageToSentToServer);

        if (jsonCommand.isEmpty()) {
            return;
        }
        if (jsonCommand.equals("off")) {
            closeClient(socket, bufferedReader, bufferedWriter);
            return;
        }

        String response = sendACommand(jsonCommand);

        if (response == null) {
            closeClient(socket, bufferedReader, bufferedWriter);
            return;
        }

        if (response.equals("DEAD")) {
            System.out.println("\nYour robot has died!");
            closeClient(socket, bufferedReader, bufferedWriter);
            return;
        }

        String commandName = messageToSentToServer.toLowerCase().split(" ")[0];
        JSONObject responseJSON = new JSONObject(response);

        checkIfRobotDied(responseJSON);

        if (commandName.equals("launch")) {
            if (responseJSON.get("result").equals("ERROR")) {
                commandValidator.setRobotName("");
            }
        }

        responseHandler.decodeResponse(commandName, responseJSON);
    }

    /**
     * Sends a preformatted JSON command to the server and waits for a response.
     *
     * @param commandToSend the JSON formatted command string
     * @return the server's response as a string
     * @throws IOException if IO operations fail
     */
    public String sendACommand(String commandToSend) throws IOException{

        bufferedWriter.write(commandToSend);
        bufferedWriter.newLine();
        bufferedWriter.flush();

        // Waiting on response from the server
        return bufferedReader.readLine();
    }

    /**
     * Closes all streams and the socket, and terminates the client application.
     *
     * @param socket the client socket
     * @param bufferedReader the input stream
     * @param bufferedWriter the output stream
     */
    public void closeClient(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        try {
            if (socket != null) {
                socket.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            System.out.println("Disconnected from Server!");
            System.exit(0);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Entry point for the client application.
     * Prompts the user to launch a robot before sending any other commands.
     * Valid robot types are listed via the "types" command.
     *
     * @param args command line arguments
     * @throws IOException if the socket fails to connect
     */
    public static void main(String[] args) throws IOException {
        // 127.0.0.1 on purpose: "localhost" resolves to ::1 first on
        // Windows, and Docker Desktop's IPv6 port forwarding can be broken
        // while the IPv4 path works - pinning IPv4 keeps play against a
        // container reliable. Optional arguments override the defaults:
        //   java -cp robot-world-...-jar-with-dependencies.jar \
        //       za.co.wethinkcode.robots.client.Client [host] [port]
        String host = args.length > 0 ? args[0]
                : System.getenv().getOrDefault("ROBOT_WORLD_HOST", "127.0.0.1");
        int port = args.length > 1 ? Integer.parseInt(args[1])
                : Integer.parseInt(System.getenv().getOrDefault("ROBOT_WORLD_PORT", "5000"));
        Socket socket = new Socket(host, port);
        System.out.println("Connected to server at " + host + ":" + port + " ... ");
        System.out.println("Use launch command first e.g. launch <type> <name>\"");
        System.out.println("""
                        Types:
                        ---------------
                        - Juggernaut
                        - Tank
                        - Soldier
                        - Marksman
                        - Sniper
                        - Intruder (places mines, no gun)
                        ---------------""");

        Client client = new Client(socket, "");
        client.sendCommands();
    }



}
