package za.co.wethinkcode.robots.client;

import org.json.JSONObject;

/**
 * The ResponseHandler class processes JSON responses received from the server.
 * It extracts and displays meaningful information to the user based on the command issued.
 */
public class ResponseHandler {

    /**
     * Constructs a new ResponseHandler.
     */
    public ResponseHandler(){}

    /**
     * Decodes the server's JSON response and delegates output based on the result.
     * If the result is "ERROR", an error message is displayed.
     * If the result is "OK" (or the command is "state"), it displays the relevant data and the robot's state.
     *
     * @param command  The command that was sent to the server.
     * @param json     The JSON object representing the server's response.
     */
    public void decodeResponse(String command, JSONObject json){

        if (!command.equals("state")){
            String result = json.getString("result");
            JSONObject data = json.getJSONObject("data");
            if (result.equals("ERROR")){
                showError(data);
                return;
            }
            showOK(command, data);
        }
        JSONObject state = json.getJSONObject("state");
        showState(state);
    }

    /**
     * Prints an error message extracted from the JSON data object.
     *
     * @param data The JSON object containing the error message.
     */
    public void showError(JSONObject data){
        String msg = data.getString("message");
        System.out.println("ERROR: " + msg);
    }

    /**
     * Handles a successful command response and prints the result using the appropriate handler.
     * If the command was "fire" and the result was a hit, the state is also shown.
     *
     * @param commandName The command that was executed.
     * @param data        The data part of the server response.
     */
    public void showOK(String commandName, JSONObject data){
        DataHandler dataHandler = new DataHandler();
        System.out.println("DATA:");
        dataHandler.handleData(commandName, data);
        if (commandName.equals("fire") && data.getString("message").equals("Hit")){
            showState(data.getJSONObject("state"));
        }
        System.out.println();
    }

    /**
     * Displays the current state of the robot, including position, direction, shields, shots, and status.
     *
     * @param state The JSON object representing the robot's current state.
     */
    public void showState(JSONObject state){
        String position = state.getJSONArray("position").toString();
        String direction = state.getString("direction");
        String shields = Integer.toString(state.getInt("shields"));
        String shots = Integer.toString(state.getInt("shots"));
        String status = state.getString("status");

        System.out.println("""
                State:
                 position:\s""" + position + """
                \n direction:\s""" + direction + """
                \n shields:\s""" + shields + """
                \n shots\s""" + shots + """
                \n status\s""" + status);
    }
}
