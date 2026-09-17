package za.co.wethinkcode.robots.server.protocol;

import org.json.JSONArray;
import org.json.JSONObject;
import za.co.wethinkcode.robots.server.Robot;
import za.co.wethinkcode.robots.server.World;
import za.co.wethinkcode.robots.client.clientcommands.Command;

public class Response {
    private final World world;

    public Response(World world) {
        this.world = world;
    }

    public String handleResponse(Command command) {
        JSONObject json = new JSONObject();
        String commandName = command.getCommandName();
        Robot robot = world.getARobot(command.getRobotName());

        if (robot == null && !"launch".equals(commandName)) {
            json.put("result", "ERROR");
            json.put("data", createErrorJson("Robot does not exist in the world"));
            return json.toString();
        }

        String executionResult = command.execute(world);
        ResponsePayload payload = parseExecutionResult(executionResult);

        if ("launch".equals(commandName)) {
            robot = world.getARobot(command.getRobotName());
        }

        if (!"OK".equals(payload.status())) {
            json.put("result", payload.status());
            json.put("data", createErrorJson(payload.message()));
            return json.toString();
        }

        json.put("result", "OK");
        json.put("data", buildCommandData(commandName, robot, payload.message()));

        if (robot != null) {
            json.put("state", createStateJson(robot));
            if (!robot.isDead() && !robot.isMining()) {
                robot.setStatus("NORMAL");
            }
        }

        return json.toString();
    }

    private ResponsePayload parseExecutionResult(String rawResult) {
        int hashIndex = rawResult.indexOf('#');
        if (hashIndex == -1) {
            return new ResponsePayload(rawResult, "");
        }
        return new ResponsePayload(
                rawResult.substring(0, hashIndex),
                rawResult.substring(hashIndex + 1)
        );
    }

    private JSONObject buildCommandData(String commandName, Robot robot, String message) {
        return switch (commandName) {
            case "state" -> new JSONObject();
            case "launch" -> createLaunchDataJson(robot);
            case "fire" -> createFireDataJson(message);
            case "look" -> createLookDataJson(message);
            default -> createDataJson(message);
        };
    }

    public JSONObject createErrorJson(String errorMsg) {
        return new JSONObject().put("message", errorMsg);
    }

    public JSONObject createLaunchDataJson(Robot robot) {
        return new JSONObject()
                .put("position", createPosition(robot))
                .put("visibility", world.getLookDistance())
                .put("reload", world.getReloadTime())
                .put("repair", world.getRepairTime())
                .put("setmine", world.getSetMineTime())
                .put("shields", world.getShields());
    }

    public JSONObject createLookDataJson(String message) {
        return new JSONObject().put("objects", new JSONArray(message));
    }

    public JSONObject createFireDataJson(String message) {
        JSONObject fire = new JSONObject(message);
        if ("Hit".equals(fire.optString("message"))) {
            Robot hitRobot = world.getARobot(fire.optString("robot"));
            if (hitRobot != null) {
                hitRobot.takeHit();
                fire.put("state", createStateJson(hitRobot));
            }
        }
        return fire;
    }

    public JSONObject createDataJson(String message) {
        return new JSONObject().put("message", message);
    }

    public JSONObject createStateJson(Robot robot) {
        return new JSONObject()
                .put("position", createPosition(robot))
                .put("direction", robot.getDirection())
                .put("shields", robot.getShield())
                .put("shots", robot.getShots())
                .put("status", robot.getStatus());
    }

    private JSONArray createPosition(Robot robot) {
        return new JSONArray().put(robot.getX()).put(robot.getY());
    }

    private record ResponsePayload(String status, String message) {}
}