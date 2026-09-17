package za.co.wethinkcode.robots.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class RobotClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PrintStream out;
    private BufferedReader in;
    private Socket socket;

    public void connect(String ipAddress, int port) {
        try {
            socket = new Socket(ipAddress, port);
            out = new PrintStream(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
        } catch (IOException e) {
            //error connecting should just throw Runtime error and fail test
            throw new RuntimeException("Error connecting to Robot Worlds server.", e);
        }
    }

    public boolean isConnected() {
        //here we have to check if socket is null before checking if it is connected
        //return socket.isConnected();
        return socket != null && socket.isConnected();
        }

    public void disconnect() {
        try {
            // here we prevent NUllPointerExceptions if the connection fails in the beforeach
            if (out != null){
            out.close();}
            if (in != null) {
            in.close();}
            if (socket != null && !socket.isClosed()){
                socket.close();}
        } catch (IOException e) {
            //error connecting should just throw Runtime error and fail test
            throw new RuntimeException("Error disconnecting from Robot Worlds server.", e);
        }
    }

    public JsonNode sendRequest(String requestJsonString) {
        try {
            out.println(requestJsonString);
            out.flush();
            return OBJECT_MAPPER.readTree(in.readLine());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing server response as JSON.", e);
        } catch (IOException e) {
            throw new RuntimeException("Error reading server response.", e);
        }
    }

    public String sendRequestAsString(String requestString) {
        try {
            out.println(requestString);
            out.flush();
            return in.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Error reading server response.", e);
        }
    }
}
