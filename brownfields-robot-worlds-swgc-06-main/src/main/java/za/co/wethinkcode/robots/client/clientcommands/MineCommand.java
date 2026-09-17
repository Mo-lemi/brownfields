package za.co.wethinkcode.robots.client.clientcommands;

import org.json.JSONArray;
import za.co.wethinkcode.robots.server.Direction;
import za.co.wethinkcode.robots.server.Robot;
import za.co.wethinkcode.robots.server.World;

/**
 * Handles the "mine" command for a robot configured to place mines
 * (a kind with no gun, e.g. the Intruder).
 *
 * The mine is set at the robot's current coordinate. While it is being
 * set the robot is in SetMine mode: its shields are disabled and it
 * cannot move at all - a sitting duck. When the mine is ready the robot
 * automatically moves one step forward; if that step is blocked it stays
 * standing on its own mine and takes the damage.
 *
 * Stepping on a mine - own or someone else's - reduces the shields by
 * 3 hits and detonates the mine.
 */
public class MineCommand extends Command {

    /**
     * Constructs a MineCommand for the given robot.
     *
     * @param robotName The name of the robot that will place the mine.
     * @param args      The command arguments (none expected).
     */
    public MineCommand(String robotName, JSONArray args) {
        super("mine", robotName, args);
    }

    /**
     * Executes the mine command.
     *
     * @param world The world in which the robot operates.
     * @return A response string indicating the outcome:
     *         - "ERROR#..." when the robot may not or cannot place a mine.
     *         - "OK#Done" when the mine was placed and the robot moved clear.
     *         - "OK#..." variants when the robot ended up on a mine and took damage.
     */
    @Override
    public String execute(World world) {
        Robot robot = world.getARobot(getRobotName());
        if (robot == null) {
            return "Error#robot not found";
        }

        // only kinds without an active weapon can place mines
        if (!robot.getType().canPlaceMine()) {
            return "ERROR#This robot is not configured to place mines";
        }

        // one mine at a time
        if (robot.isMining()) {
            return "ERROR#Robot is already setting a mine";
        }

        // the mine needs an unoccupied coordinate
        if (!world.canPlaceMineAt(robot.getX(), robot.getY())) {
            return "ERROR#Cannot place a mine here";
        }

        // SetMine mode: shields disabled, no movement allowed - sitting duck
        robot.startMining();
        try {
            Thread.sleep(world.getSetMineTime() * 1000L);
        } catch (InterruptedException e) {
            robot.stopMining();
            robot.setStatus("NORMAL");
            return "ERROR#Interrupted";
        }

        // the mine is in the ground - the shield-disabling window is over
        robot.stopMining();

        world.placeMine(robot.getX(), robot.getY());

        // the robot automatically moves one step forward once the mine is set
        int[] next = nextPosition(robot);
        if (world.isValidPosition(next[0], next[1])) {
            robot.setPosition(next[0], next[1]);
        }
        // else: the step is blocked, so the robot stays standing on its own mine

        // stepping into a bottomless pit instead is fatal too
        if (world.hasPitAt(robot.getX(), robot.getY())) {
            robot.setDead();
            return "OK#Fell";
        }

        // standing on a mine (own or anyone's) hurts - the robot that was
        // hit is told so through this response message
        if (world.hasMineAt(robot.getX(), robot.getY())) {
            world.explodeMineAt(robot.getX(), robot.getY());
            robot.takeMineHit();
            if (robot.isDead()) {
                return "OK#Stepped on a mine and was destroyed";
            }
            return "OK#Stepped on your own mine and lost 3 shields";
        }

        return "OK#Done";
    }

    /**
     * Works out the coordinate one step ahead of the robot in the direction it faces.
     *
     * @param robot The robot to move.
     * @return The [x,y] coordinate one step forward.
     */
    private int[] nextPosition(Robot robot) {
        int x = robot.getX();
        int y = robot.getY();
        Direction facing = robot.getDirection();

        switch (facing) {
            case NORTH -> y = y + 1;
            case EAST  -> x = x + 1;
            case SOUTH -> y = y - 1;
            case WEST  -> x = x - 1;
        }

        return new int[] { x, y };
    }
}
