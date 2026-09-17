package za.co.wethinkcode.robots.client.clientcommands;

import org.json.JSONArray;
import org.json.JSONObject;
import za.co.wethinkcode.robots.server.Robot;
import za.co.wethinkcode.robots.server.World;

import java.util.List;

/**
 * Handles the "fire" command, allowing a robot to shoot in the direction it is facing.
 * If another robot is in the line of fire, it is considered a hit. If not, the shot is a miss.
 * The command decreases the robot's available shots by one.
 */
public class FireCommand extends Command{

    /**
     * Constructs a new FireCommand for the specified robot.
     *
     * @param name The name of the robot using the fire command.
     * @param args Arguments for the command .
     */
    public FireCommand(String name, JSONArray args){
        super("fire", name, args);
    }

    /**
     * Executes the fire command:
     * - Checks if the robot has shots left.
     * - Uses the LookCommand to detect robots in the line of fire.
     * - If a robot is hit, it records the hit; if not, it's a miss.
     *
     * @param world The world in which the command is executed.
     * @return A result string formatted as:
     *         - "ERROR#No more shots left" if robot is out of ammo.
     *         - "OK# if no robot is hit.
     */
    @Override
    public String execute(World world) {
        int distanceShot = 0;
        Robot robot = world.getARobot(this.getRobotName());
        Robot hitRobot = null;
        LookCommand look = new LookCommand(getRobotName(), new JSONArray());
        JSONArray obstacles = look.look(robot.getX(), robot.getY(), robot.getDirection(), robot.getDistance(), world);

        if (robot.getShots() == 0){
            return "ERROR#No more shots left";
        }

        for (int i = 0; i<obstacles.length(); i++){
            JSONObject ob = obstacles.getJSONObject(i);
            if (ob.getString("type").equals("ROBOT")){
                distanceShot = ob.getInt("distance");
                List<Integer> coords = look.move(robot.getDirection(), robot.getX(), robot.getY(), distanceShot);
                hitRobot = world.getARobot(coords.get(0), coords.get(1));
                break;
            }
        }


        robot.setShots(robot.getShots() - 1);
        if (hitRobot == null){
            return "OK#" + createFireJson("Miss", hitRobot, distanceShot);
        }

        return "OK#" + createFireJson("Hit", hitRobot, distanceShot);
    }

    /**
     * Creates a JSON response for the result of a fire command.
     *
     * @param result The result of the shot "Hit" or "Miss".
     * @param robot The robot that was hit.
     * @param distance The distance to the robot that was hit.
     * @return A JSONObject with information about the fire result.
     */
    public JSONObject createFireJson(String result, Robot robot, int distance){
        JSONObject fire = new JSONObject();
        fire.put("message", result);
        if (result.equals("Miss")){return fire;}
        fire.put("distance", distance);
        fire.put("robot", robot.getName());
        return fire;
    }
}
