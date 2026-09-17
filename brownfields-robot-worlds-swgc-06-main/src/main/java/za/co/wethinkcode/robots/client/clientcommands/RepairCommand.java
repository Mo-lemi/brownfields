package za.co.wethinkcode.robots.client.clientcommands;

import org.json.JSONArray;
import za.co.wethinkcode.robots.server.Robot;
import za.co.wethinkcode.robots.server.World;

/**
 * Handles the "repair" command which allows a robot to repair its shield.
 * This action simulates a delay based on the world's configured repair time.
 */
public class RepairCommand extends Command {

    /**
     * Constructs a RepairCommand for the specified robot.
     *
     * @param robotName The name of the robot issuing the repair command.
     * @param args      The command arguments.
     */
    public RepairCommand(String robotName, JSONArray args) {
        super("repair", robotName, args);
    }

    /**
     * Executes the repair operation.
     * Sets the robot's status to "REPAIR", waits for the repair time duration,
     * and restores the robot's shield.
     *
     * @param world The world in which the robot operates.
     * @return A string indicating success or an error if the operation is interrupted.
     */
    @Override
    public String execute(World world) {
        Robot robot = world.getARobot(getRobotName());
        int time = world.getRepairTime() * 1000;
        try {
            robot.setStatus("REPAIR");
            Thread.sleep(time);
            robot.repairShield();
        }catch (InterruptedException e){
            return "ERROR#Interrupted";
        }
        return "OK#Done";
    }

}