package za.co.wethinkcode.robots.client;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The DataHandler class processes different types of data received from the server
 * based on the command issued, and prints appropriate details to the console.
 */
public class DataHandler {

    /**
     * Constructs a new DataHandler.
     */
    public DataHandler(){}

    /**
     * Handles the JSON data based on the command name, delegating to specialized display methods.
     *
     * @param commandName The name of the command that generated the data.
     * @param data        The JSON object containing the data to handle.
     */
    public void handleData(String commandName , JSONObject data){
        switch (commandName){
            case "launch":
                showLaunchData(data);
                break;
            case "fire":
                showFireData(data);
                break;
            case "look":
                showLookData(data);
                break;
            default:
                showData(data);
        }
    }

    /**
     * Prints details about obstacles or objects seen by the robot when the "look" command is issued.
     *
     * @param data The JSON object containing an array of obstacles.
     */
    public void showLookData(JSONObject data){
        System.out.println("objects:");
        JSONArray obstacles = data.getJSONArray("objects");
        if (obstacles.isEmpty()){
            System.out.println("NONE");
            return;
        }

        for (int i = 0; i < obstacles.length(); i++){
            System.out.println();
            JSONObject obstacle = obstacles.getJSONObject(i);
            String direction = obstacle.getString("direction");
            String type = obstacle.getString("type");
            String distance = Integer.toString(obstacle.getInt("distance"));

            System.out.println("""
                    \sdirection:\s""" + direction + """
                    \n type\s""" + type + """
                    \n distance\s""" + distance);
        }
    }

    /**
     * Displays information related to the result of a "fire" command,
     * including hit or miss message, distance to target, and target robot name.
     *
     * @param data The JSON object containing firing result details.
     */
    public void showFireData(JSONObject data){
        String message = data.getString("message");
        System.out.println(" message: " + message);
        if (message.equals("Miss")){return;}
        String distance = Integer.toString(data.getInt("distance"));
        String robotName = data.getString("robot");

        System.out.println("""
                \sdistance:\s""" + distance + """
                \n robot:\s""" + robotName);
    }

    /**
     * Displays the initial status information of the robot when launched.
     *
     * @param data The JSON object containing launch details like position, visibility, reload time, repair capacity, and shields.
     */
    public void showLaunchData(JSONObject data){
        String position = data.getJSONArray("position").toString();
        String visibility = Integer.toString(data.getInt("visibility"));
        String reload = Integer.toString(data.getInt("reload"));
        String repair = Integer.toString(data.getInt("repair"));
        String shields = Integer.toString(data.getInt("shields"));

        System.out.println("""
                \sposition:\s""" + position + """
                \n visibility:\s""" + visibility + """
                \n reload:\s""" + reload + """
                \n repair\s""" + repair + """
                \n shields\s""" + shields);

    }

    /**
     * Prints a message found in the data JSON object.
     *
     * @param data The JSON object containing a message.
     */
    public void showData(JSONObject data){
        String message = data.getString("message");
        System.out.println(" message: " + message);
    }
}
