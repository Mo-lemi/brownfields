package za.co.wethinkcode.robots.server;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.client.clientcommands.Command;
import za.co.wethinkcode.robots.client.clientcommands.FireCommand;
import za.co.wethinkcode.robots.client.clientcommands.ForwardCommand;
import za.co.wethinkcode.robots.client.clientcommands.LaunchCommand;
import za.co.wethinkcode.robots.client.clientcommands.LookCommand;
import za.co.wethinkcode.robots.client.clientcommands.MineCommand;
import za.co.wethinkcode.robots.client.clientcommands.StateCommand;
import za.co.wethinkcode.robots.server.protocol.Response;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit tests for the Response layer: the JSON envelope it builds
 * for each command, the status-reset rules, and the guards.
 */
public class TestResponseDirect {

    private World world;
    private Response response;

    @BeforeEach
    void setUp() {
        world = new World(20, 20);
        response = new Response(world);
    }

    private String handle(Command command) {
        return response.handleResponse(command);
    }

    @Test
    void stateCommandReturnsEmptyDataAndTheRobotState() {
        Robot robot = new Robot("hal", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);

        JSONObject json = new JSONObject(handle(new StateCommand("hal", new JSONArray())));

        assertEquals("OK", json.getString("result"));
        assertEquals(0, json.getJSONObject("data").length(), "state has no extra data");
        assertEquals(3, json.getJSONObject("state").getInt("shields"));
        assertEquals("NORTH", json.getJSONObject("state").getString("direction"));
    }

    @Test
    void missingRobotReturnsTheStandardError() {
        JSONObject json = new JSONObject(handle(new StateCommand("ghost", new JSONArray())));

        assertEquals("ERROR", json.getString("result"));
        assertEquals("Robot does not exist in the world",
                json.getJSONObject("data").getString("message"));
    }

    @Test
    void launchDataContainsTheWorldConfiguration() {
        handle(new LaunchCommand("hal", new JSONArray("[\"soldier\"]")));

        JSONObject json = new JSONObject(handle(new StateCommand("hal", new JSONArray())));
        // the launch data shape is verified on the launch response itself:
        JSONObject launch = new JSONObject(handle(new LaunchCommand("tmp", new JSONArray("[\"soldier\"]"))));
        JSONObject data = launch.getJSONObject("data");
        assertEquals(5, data.getInt("visibility"));
        assertEquals(2, data.getInt("reload"));
        assertEquals(3, data.getInt("repair"));
        assertEquals(3, data.getInt("setmine"));
        assertEquals(5, data.getInt("shields"));
        assertEquals(2, data.getJSONArray("position").length());
        assertNotNull(json.getJSONObject("state"));
    }




    @Test
    void defaultCommandsPutTheMessageInTheDataBlock() {
        Robot intruder = new Robot("digger", world, 0, 0, RobotTypes.INTRUDER);
        world.addRobot(intruder);
        System.setProperty("setMineTime", "0");

        JSONObject json = new JSONObject(handle(new MineCommand("digger", new JSONArray())));

        assertEquals("Done", json.getJSONObject("data").getString("message"));
        System.clearProperty("setMineTime");
    }

    @Test
    void successfulCommandsResetTheStatusToNormal() {
        Robot robot = new Robot("hal", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);
        robot.setStatus("REPAIR");

        handle(new ForwardCommand("hal", new JSONArray("[1]")));

        assertEquals("NORMAL", robot.getStatus(), "the status is reset after the response");
    }

    @Test
    void setMineStatusSurvivesTheResponse() {
        Robot robot = new Robot("digger", world, 0, 0, RobotTypes.INTRUDER);
        world.addRobot(robot);
        robot.startMining();

        JSONObject json = new JSONObject(handle(new StateCommand("digger", new JSONArray())));

        assertEquals("SETMINE", json.getJSONObject("state").getString("status"),
                "the mining status is what the client sees");
        assertEquals("SETMINE", robot.getStatus(),
                "and it stays until a later command resets it");
    }



    @Test
    void errorResponsesHaveResultDataButNoState() {
        JSONObject json = new JSONObject(handle(new StateCommand("ghost", new JSONArray())));

        assertEquals("ERROR", json.getString("result"));
        assertTrue(json.getJSONObject("data").has("message"));
        assertFalse(json.has("state"), "errors carry no robot state");
    }
}
