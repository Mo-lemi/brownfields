package za.co.wethinkcode.robots.server.protocol;

import org.json.JSONArray;
import org.json.JSONObject;
import za.co.wethinkcode.robots.server.World;
import za.co.wethinkcode.robots.client.clientcommands.Command;

/**
 * Handles incoming requests by processing command data.
 */
public class Request {

    /** Constructor. */
    public Request(){}

    /**
     * Parses a JSON string representing a client request, creates the corresponding Command,
     * executes it in the context of the given World, and returns a JSON-formatted response string.
     *
     * @param jsonString The JSON string received from the client representing the command request.
     * @param world      The World instance in which the command will be executed.
     * @return           A JSON string representing the result of executing the command.
     */
    public String handleRequest(String jsonString, World world) {

        Response responseHandler = new Response(world);

        try {
            JSONObject json = new JSONObject(jsonString);
            String robotName = json.getString("robot");
            String commandName = json.getString("command");
            JSONArray args = json.getJSONArray("arguments");
            Command command = Command.create(commandName, robotName, args);
            return responseHandler.handleResponse(command);
        }
        catch (IllegalArgumentException e) {
            // Unknown command, e.g. "luanch" — message is "Unsupported command ..."
            JSONObject error = new JSONObject();
            error.put("result", "ERROR");
            error.put("data", new JSONObject().put("message", e.getMessage()));
            return error.toString();
        } catch (org.json.JSONException e) {
            // Malformed request JSON — also reply instead of killing the thread
            JSONObject error = new JSONObject();
            error.put("result", "ERROR");
            error.put("data", new JSONObject().put("message", "Could not parse arguments"));
            return error.toString();
        }
    }
}