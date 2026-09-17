package za.co.wethinkcode.robots.client.clientcommands;

import org.json.JSONArray;
import za.co.wethinkcode.robots.server.Robot;
import za.co.wethinkcode.robots.server.World;

/**
 * Handles the "reload" command which allows a robot to reload its ammo.
 * This operation simulates a delay based on the world's configured reload time.
 */
public class ReloadCommand extends Command {

    /**
     * Constructs a ReloadCommand for the given robot.
     *
     * @param robotName The name of the robot that will execute the command.
     * @param args      The command arguments.
     */
    public ReloadCommand(String robotName, JSONArray args) {
        super("reload", robotName, args);
    }

    /**
     * Executes the reload operation.
     * Puts the robot in "RELOAD" status, simulates waiting,
     * and restores the robot's shots.
     *
     * @param world The world in which the robot operates.
     * @return A string indicating success or if the reload was interrupted.
     */
    @Override
    public String execute(World world) {
        Robot robot = world.getARobot(getRobotName());
        int time = world.getReloadTime() * 1000;
        try {
            robot.setStatus("RELOAD");
            Thread.sleep(time);
            robot.reloadShots();
        }catch (InterruptedException e){
            return "ERROR#Interrupted";
        }
        return "OK#Done";
    }
}
