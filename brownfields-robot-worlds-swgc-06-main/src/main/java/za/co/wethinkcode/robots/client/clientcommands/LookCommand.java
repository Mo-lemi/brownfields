package za.co.wethinkcode.robots.client.clientcommands;

import org.json.JSONArray;
import org.json.JSONObject;
import za.co.wethinkcode.robots.server.Direction;
import za.co.wethinkcode.robots.server.Robot;
import za.co.wethinkcode.robots.server.World;
import za.co.wethinkcode.robots.server.obstacles.Obstacle;

import java.util.*;

/**
 * Handles the "look" command, allowing a robot to scan in all four cardinal directions
 * (NORTH, SOUTH, EAST, WEST) up to the configured look distance.
 * Reports obstacles, robots, and world edges within sight range.
 */
public class LookCommand extends Command{

    /**
     * Executes the look command.
     * The robot scans in all four directions (NORTH, SOUTH, EAST, WEST) for visible objects.
     *
     * @param world The game world in which the robot is operating.
     * @return A string starting with "OK#" followed by a JSONArray of detected objects
     *         including their type, direction, and distance.
     */
    @Override
    public String execute(World world) {
        JSONArray result = new JSONArray();
        Robot robot = world.getARobot(getRobotName());

        JSONArray northLook = look(robot.getX(), robot.getY(),Direction.NORTH,world.getLookDistance(), world);
        JSONArray southLook = look(robot.getX(), robot.getY(),Direction.SOUTH,world.getLookDistance(), world);
        JSONArray westLook = look(robot.getX(), robot.getY(),Direction.WEST,world.getLookDistance(), world);
        JSONArray eastLook = look(robot.getX(), robot.getY(),Direction.EAST,world.getLookDistance(), world);
        List<JSONArray> arrs = new ArrayList<>(Arrays.asList(northLook, southLook, westLook,eastLook));
        for (JSONArray arr : arrs) {
            for (int i = 0; i < arr.length(); i++) {
                result.put(arr.get(i));
            }
        }

        // Mines are only detectable within a quarter of the visibility
        // range, rounded down, in any direction the robot can scan.
        JSONArray mines = lookForMines(robot.getX(), robot.getY(), world);
        for (int i = 0; i < mines.length(); i++) {
            result.put(mines.get(i));
        }

        return "OK#" + result;
    }

    /**
     * Scans the four directions for mines within a quarter of the world's
     * visibility range, rounded down. Mines are reported like other
     * objects, with type MINE.
     *
     * @param x     Starting x-coordinate.
     * @param y     Starting y-coordinate.
     * @param world The world to scan for mines.
     * @return A JSONArray of detected mines with their direction and distance.
     */
    public JSONArray lookForMines(int x, int y, World world) {
        JSONArray mines = new JSONArray();
        int mineRange = world.getLookDistance() / 4;

        if (mineRange <= 0) {
            return mines;
        }

        List<Direction> directions = List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
        for (Direction facing : directions) {
            for (int i = 1; i <= mineRange; i++) {
                List<Integer> coords = move(facing, x, y, i);
                int newX = coords.get(0);
                int newY = coords.get(1);

                if (!world.isInsideBounds(newX, newY)) {
                    break;
                }

                if (world.hasMineAt(newX, newY)) {
                    JSONObject mine = new JSONObject();
                    mine.put("direction", facing.name());
                    mine.put("distance", i);
                    mine.put("type", "MINE");
                    mines.put(mine);
                }
            }
        }

        return mines;
    }

    /**
     * Looks in a specific direction from a starting position for a certain distance.
     * Detects and returns information about robots, obstacles, and edges.
     *
     * @param x        Starting x-coordinate.
     * @param y        Starting y-coordinate.
     * @param facing   Direction in which to look.
     * @param distance Maximum number of steps to scan.
     * @param world    The world to check for objects and boundaries.
     * @return A JSONArray of visible objects in the specified direction.
     */
    public JSONArray look(int x, int y, Direction facing, int distance, World world){
        JSONArray objects = new JSONArray();

        for (int i = 1; i <= distance; i++) {
            List<Integer> coords = move(facing, x, y, i);
            int newX = coords.get(0);
            int newY = coords.get(1);

            String type = getObjectType(world.checkType(newX, newY));
            if (type.isEmpty()) {
                continue;
            }

            JSONObject object = createObject(world, facing, newX, newY, i);
            objects.put(object);

            if ("EDGE".equals(type)) {
                break;
            }

            if ("OBSTACLE".equals(type)
                    && !world.getAObstacle(newX, newY).isTransparent()) {
                break;
            }
        }

        return objects;
    }


    private JSONObject createObject(World world, Direction facing,
                                    int x, int y, int distance) {
        JSONObject object = new JSONObject();
        // ensure it outputs North
        object.put("direction", facing.name());
        object.put("distance", distance);

        String type = getObjectType(world.checkType(x, y));

        if ("OBSTACLE".equals(type)) {
//            Obstacle obstacle = world.getAObstacle(x, y);
//            object.put("type", "OBSTACLE: " + obstacle.getType());
//          this is so that the acceptance test can read it correctly
            object.put("type", "OBSTACLE");
        } else {
            object.put("type", type);
        }

        return object;
    }



    /**
     * Calculates a new coordinate after moving a certain number of steps in a given direction.
     *
     * @param facing The direction to move in.
     * @param x      Starting x-coordinate.
     * @param y      Starting y-coordinate.
     * @param step   Number of steps to move.
     * @return A list containing the new x and y coordinates.
     */
    public List<Integer> move(Direction facing, int x, int y, int step){
        List<Integer> coords = new ArrayList<>();

        switch (facing) {
            case NORTH:
                y = y + step;
                break;
            case EAST:
                x = x + step;
                break;
            case SOUTH:
                y = y - step;
                break;
            case WEST:
                x = x - step;
                break;
        }

        coords.add(x);
        coords.add(y);

        return coords;
    }

    /**
     * Constructs a new LookCommand for the given robot name and arguments.
     *
     * @param robotName The name of the robot using the look command.
     * @param args      The arguments passed to the command.
     */
    public LookCommand(String robotName, JSONArray args) {
        super("look", robotName, args);
    }
}
