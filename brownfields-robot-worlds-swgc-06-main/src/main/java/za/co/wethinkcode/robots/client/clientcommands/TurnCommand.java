package za.co.wethinkcode.robots.client.clientcommands;

import org.json.JSONArray;
import za.co.wethinkcode.robots.server.Robot;
import za.co.wethinkcode.robots.server.Direction;
import za.co.wethinkcode.robots.server.World;

/**
 * Command to turn the robot left or right.
 */
public class TurnCommand extends Command {

    private final String turnDirection; // "left" or "right"

    /**
     * Constructs a TurnCommand for the given robot with the desired turn direction.
     *
     * @param robotName     The name of the robot using the turn command.
     * @param turnDirection A JSONArray containing a single string: "left" or "right".
     */
    public TurnCommand(String robotName, JSONArray turnDirection) {
        super("TURN", robotName, turnDirection);
        this.turnDirection = turnDirection.getString(0);
    }

    /**
     * Executes the turn operation for the robot.
     * Changes the robot's facing direction based on the command argument.
     *
     * @param world The world containing the robot.
     * @return "OK#Done" if turn was successful, otherwise "ERROR".
     */
    @Override
    public String execute(World world) {
        Robot target = world.getARobot(getRobotName());

        // a robot that is setting a mine cannot move at all
        if (target.isMining()) {
            return "ERROR#Robot is setting a mine";
        }

        Direction currentDirection = target.getDirection();
        Direction newDirection;

        switch (turnDirection.toLowerCase()) {
            case "left":
                newDirection = turnLeft(currentDirection);
                break;
            case "right":
                newDirection = turnRight(currentDirection);
                break;
            default:
                return "ERROR";
        }

        target.setDirection(newDirection);
        return "OK#Done";
    }

    /**
     * Determines the new direction when turning left from the current direction.
     *
     * @param facing The current direction the robot is facing.
     * @return The new direction after turning left.
     */
    public Direction turnLeft(Direction facing){
        switch (facing){
            case Direction.NORTH:
                return Direction.WEST;
            case Direction.WEST:
                return Direction.SOUTH;
            case Direction.SOUTH:
                return Direction.EAST;
        }
        return Direction.NORTH;
    }

    /**
     * Determines the new direction when turning right from the current direction.
     *
     * @param facing The current direction the robot is facing.
     * @return The new direction after turning right.
     */
    public Direction turnRight(Direction facing){
        switch (facing){
            case Direction.NORTH:
                return Direction.EAST;
            case Direction.EAST:
                return Direction.SOUTH;
            case Direction.SOUTH:
                return Direction.WEST;
        }
        return Direction.NORTH;
    }
}