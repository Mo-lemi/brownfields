package za.co.wethinkcode.robots.server.servercommands;

import za.co.wethinkcode.robots.server.Robot;
import za.co.wethinkcode.robots.server.World;

import java.util.List;

/**
 * Command that displays the status of all robots in the world.
 */
public class RobotsCommand extends ServerCommand{

    /**
     * Executes the robots command, which prints the status of all robots currently
     * in the world. If no robots exist, it outputs a message indicating so.
     *
     * @return true after successfully displaying robot statuses.
     */
    @Override
    public boolean execute() {
        List<Robot> Robots = this.getWorld().getRobots();
        if (Robots.isEmpty()) {
            System.out.println("There are no Robots in the world!");
            return true;
        }

        System.out.println("Robots:");
        for (Robot robot : Robots) {
            System.out.println("- " + robot.getName() + " " + robot.getStatus());
        }
        return true;
    }

    /**
     * Constructor for creating a RobotsCommand with a specified world.
     *
     * @param world The world containing the robots.
     */
    public RobotsCommand(World world){
        super("robots", world);
    }
}