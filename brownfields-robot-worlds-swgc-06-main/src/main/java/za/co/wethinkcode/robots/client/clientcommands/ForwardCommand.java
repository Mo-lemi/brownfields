package za.co.wethinkcode.robots.client.clientcommands;

import org.json.JSONArray;
import za.co.wethinkcode.robots.server.Robot;
import za.co.wethinkcode.robots.server.World;

public class ForwardCommand extends Command {

    public ForwardCommand(String robotName, JSONArray args) {
        super("forward", robotName, args);
    }

    @Override
    public String execute(World world) {
        Robot robot = world.getARobot(getRobotName());
        if (robot == null) return "Error#robot not found";
        if (robot.isMining()) return "ERROR#Robot is setting a mine";

        int lastX = robot.getX();
        int lastY = robot.getY();
        int steps = getArguments().getInt(0);

        for (int step = 1; step <= steps; step++) {
            int nextX = lastX;
            int nextY = lastY;

            switch (robot.getDirection()) {
                case NORTH -> nextY += 1;
                case EAST  -> nextX += 1;
                case SOUTH -> nextY -= 1;
                case WEST  -> nextX -= 1;
            }

            String obstruction = checkObstruction(world, nextX, nextY, step);
            if (obstruction != null) {
                robot.setPosition(lastX, lastY);
                return obstruction;
            }

            if (world.hasPitAt(nextX, nextY)) {
                robot.setPosition(nextX, nextY);
                robot.setDead();
                return "OK#Fell";
            }

            lastX = nextX;
            lastY = nextY;

            if (world.hasMineAt(nextX, nextY)) {
                world.explodeMineAt(nextX, nextY);
                robot.setPosition(nextX, nextY);
                robot.takeMineHit();
                return robot.isDead() ? "OK#Stepped on a mine and was destroyed" : "OK#Stepped on a mine";
            }
        }

        robot.setPosition(lastX, lastY);
        return "OK#Done";
    }

    private String checkObstruction(World world, int x, int y, int step) {
        if (!world.isValidPosition(x, y)) {
            String type = getObjectType(world.checkType(x, y));
            if (step == 1 && "EDGE".equalsIgnoreCase(type)) {
                return "OK#At the " + getEdgeDirection(world, x, y) + " edge";
            }
            return "OK#Obstructed#Moved " + (step - 1) + " steps blocked by:" + type;
        }
        return null;
    }

    private String getEdgeDirection(World world, int x, int y) {
        if (y >= world.getHeight()) return "NORTH";
        if (y < 0) return "SOUTH";
        if (x >= world.getWidth()) return "EAST";
        if (x < 0) return "WEST";
        return "";
    }
}