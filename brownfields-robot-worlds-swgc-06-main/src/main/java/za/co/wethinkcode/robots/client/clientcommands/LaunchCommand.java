package za.co.wethinkcode.robots.client.clientcommands;

import org.json.JSONArray;
import za.co.wethinkcode.robots.server.Robot;
import za.co.wethinkcode.robots.server.RobotTypes;
import za.co.wethinkcode.robots.server.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Handles the "launch" command to introduce a new robot into the world.
 * A robot is placed at a random open position, if available, and is assigned a specific type.
 */
public class LaunchCommand extends Command{

    /**
     * Constructs a LaunchCommand for the given robot name and arguments.
     *
     * @param name The name of the robot to be launched.
     * @param args A JSONArray containing one element: the robot type as a string.
     */
    public LaunchCommand(String name, JSONArray args){
        super("launch", name, args);
    }

    /**
     * Executes the launch command by attempting to place a new robot in the world:
     * - Checks for available space.
     * - Randomly finds a valid position.
     * - Validates the robot name and type.
     * - Creates and adds the robot to the world.
     *
     * @param world The game world into which the robot should be launched.
     * @return A string indicating the outcome:
     *         - "OK" if the robot was successfully launched.
     *         - "ERROR#No space left in the world" if the world is full.
     *         - "ERROR#Too many of you in this world" if the robot name is already taken.
     *         - "ERROR#Not a valid robot type" if the robot type is unrecognized.
     */
    @Override
    public String execute(World world){
//        Random random = new Random();

        String type = getArguments().getString(0);
        RobotTypes robotType;

        List<int[]> freeCells = new ArrayList<>();

//      Here I Calculate the radius (e.g. for width 3, radius is 1)
        int radiusX = world.getWidth() / 2;
        int radiusY = world.getHeight() / 2;

//      refracted this code to loop from -1 to 1 instead of 0 to 2
        for (int i = -radiusX; i <= radiusX; i++){
            for (int j = -radiusY; j <= radiusY; j++){
                if (world.isValidPosition(i, j)){
                    freeCells.add(new int[]{i, j});
                }
            }
        }


        if (!world.isValidRobotName(this.getRobotName()) ){
            return "ERROR#Too many of you in this world";
        }

        if (freeCells.isEmpty()){
            return "ERROR#No more space in this world";
        }

        freeCells.sort((cell1, cell2) -> {
            int dist1 = Math.abs(cell1[0]) + Math.abs(cell1[1]);
            int dist2 = Math.abs(cell2[0]) + Math.abs(cell2[1]);
            return Integer.compare(dist1, dist2);
        });

        // Ensure you select index 0 instead of random.nextInt()
        int[] spot = freeCells.get(0);

        switch (type){
            case "juggernaut":
                robotType = RobotTypes.JUGGERNAUT;
                break;
            case "tank":
                robotType = RobotTypes.TANK;
                break;
            case "soldier":
                robotType = RobotTypes.SOLDIER;
                break;
            case "marksman":
                robotType = RobotTypes.MARKSMAN;
                break;
            case "sniper":
                robotType = RobotTypes.SNIPER;
                break;
            case "intruder":
                robotType = RobotTypes.INTRUDER;
                break;
            default:
                return "ERROR#Not a valid robot type";
        }

//        Random random = new Random();
//        int[] spot = freeCells.get(random.nextInt(freeCells.size()));

        Robot robot = new Robot(this.getRobotName(), world, spot[0], spot[1], robotType);
        world.addRobot(robot);
        return "OK";
    }
}
