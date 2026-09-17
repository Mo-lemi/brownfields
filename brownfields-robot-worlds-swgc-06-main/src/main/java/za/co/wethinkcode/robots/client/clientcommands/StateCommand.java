package za.co.wethinkcode.robots.client.clientcommands;

import org.json.JSONArray;
import za.co.wethinkcode.robots.server.*;

/**
 * Command to check whether a robot exists in the world.
 * Returns a simple status message based on the robot's presence.
 */
public class StateCommand extends Command {

    /**
     * Constructs a StateCommand for the specified robot.
     *
     * @param name The name of the robot using the command.
     * @param args The arguments passed to the command.
     */
    public StateCommand(String name, JSONArray args){
        super("state", name, args);
    }

    /**
     * Executes the state check operation.
     * Verifies whether the robot exists in the world.
     *
     * @param world The world in which the robot is supposed to exist.
     * @return "OK" if the robot exists, otherwise an error message.
     */
    public String execute(World world) {
        Robot robot = world.getARobot(this.getRobotName());

        if (robot == null) {
            return "ERROR#robot not in the world!";
        } else {
            return "OK";
        }
    }
}
