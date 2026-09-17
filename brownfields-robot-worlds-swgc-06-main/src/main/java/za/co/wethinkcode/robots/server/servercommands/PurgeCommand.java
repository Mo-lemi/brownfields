package za.co.wethinkcode.robots.server.servercommands;

import za.co.wethinkcode.robots.server.Robot;
import za.co.wethinkcode.robots.server.World;

public class PurgeCommand extends ServerCommand{
    
    private final String robotName;

    /**
     * Executes the purge command that requires a robot name, which kill the robot and remove it from the game.
     * in the world. If the robot does not exist, it outputs a message indicating so.
     *
     * @return true after the command is executed.
     */
    @Override
    public boolean execute() {
        if (robotName.isEmpty()) {
            System.out.println("Use purge command\ne.g. purge <robot name>\n");
            return false;
        }

        Robot robot = getWorld().getARobot(robotName);
        if (robot == null) {
            System.out.println(robotName + " does not exist in this world");
            return false;
        }

        robot.setDead();
        return true;
    }

    /**
     * Constructor for creating a PurgeCommand with a specified name and world.
     * @param robotName The name of the robot that will be targeted
     * @param world The world in which the command will be executed.
     */
    public PurgeCommand(World world, String robotName) {
        super("purge", world);
        this.robotName = robotName;
    }

    


}
