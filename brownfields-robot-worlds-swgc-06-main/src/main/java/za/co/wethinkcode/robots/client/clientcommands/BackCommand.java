package za.co.wethinkcode.robots.client.clientcommands;

import org.json.JSONArray;
import za.co.wethinkcode.robots.server.Robot;
import za.co.wethinkcode.robots.server.World;

public class BackCommand extends Command {

    public BackCommand(String robotName, JSONArray args) {
        super("back", robotName, args);
    }

    @Override
    public String execute(World world) {
        Robot robot = world.getARobot(getRobotName());
        if (robot == null) {
            return "Error#robot not found";
        }
        if (robot.isMining()) {
            return "ERROR#Robot is setting a mine";
        }

        int startX = robot.getX();
        int startY = robot.getY();
        int steps = -getArguments().getInt(0);
        int lastValidX = startX;
        int lastValidY = startY;

        for (int step = -1; step >= steps; step--) {
            int[] next = getNextPosition(robot, startX, startY, step);
            int nextX = next[0];
            int nextY = next[1];

            String obstruction = handleObstruction(world, robot, nextX, nextY, lastValidX, lastValidY, step);
            if (obstruction != null) return obstruction;

            String pit = handlePit(world, robot, nextX, nextY);
            if (pit != null) return pit;

            lastValidX = nextX;
            lastValidY = nextY;

            String mine = handleMine(world, robot, nextX, nextY);
            if (mine != null) return mine;
        }

        robot.setPosition(lastValidX, lastValidY);
        return "OK#Done";
    }

    private String handleObstruction(World world, Robot robot, int nextX, int nextY, int lastValidX, int lastValidY, int step) {
        if (!world.isValidPosition(nextX, nextY)) {
            robot.setPosition(lastValidX, lastValidY);
            String objectType = getObjectType(world.checkType(nextX, nextY));
            if (step == -1) {
                return "ERROR#Blocked by " + objectType + " of world at [" + nextX + "," + nextY + "]";
            }
            return "OK#Obstructed#Moved " + (step - 1) + " steps blocked by:" + objectType;
        }
        return null;
    }

    private String handlePit(World world, Robot robot, int nextX, int nextY) {
        if (world.hasPitAt(nextX, nextY)) {
            robot.setPosition(nextX, nextY);
            robot.setDead();
            return "OK#Fell";
        }
        return null;
    }

    private String handleMine(World world, Robot robot, int nextX, int nextY) {
        if (world.hasMineAt(nextX, nextY)) {
            world.explodeMineAt(nextX, nextY);
            robot.setPosition(nextX, nextY);
            robot.takeMineHit();
            return robot.isDead() ? "OK#Stepped on a mine and was destroyed" : "OK#Stepped on a mine";
        }
        return null;
    }

    public int[] getNextPosition(Robot robot, int x, int y, int step) {
        int nextX = x;
        int nextY = y;
        switch (robot.getDirection()) {
            case NORTH -> nextY = y + step;
            case EAST  -> nextX = x + step;
            case SOUTH -> nextY = y - step;
            case WEST  -> nextX = x - step;
        }
        return new int[] { nextX, nextY };
    }
}