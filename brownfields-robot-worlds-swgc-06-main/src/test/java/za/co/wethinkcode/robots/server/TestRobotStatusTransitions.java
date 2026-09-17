package za.co.wethinkcode.robots.server;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.client.clientcommands.ForwardCommand;
import za.co.wethinkcode.robots.client.clientcommands.LaunchCommand;
import za.co.wethinkcode.robots.client.clientcommands.RepairCommand;
import za.co.wethinkcode.robots.server.protocol.Response;

import static org.junit.jupiter.api.Assertions.*;
import org.json.JSONArray;

/**
 * Unit tests for the robot status lifecycle as it travels through the
 * Response layer: NORMAL, REPAIR, SETMINE and DEAD.
 */
public class TestRobotStatusTransitions {

    private World world;
    private Response response;

    @BeforeEach
    void setUp() {
        System.setProperty("repairTime", "0");
        System.setProperty("setMineTime", "0");
        world = new World(20, 20);
        response = new Response(world);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("repairTime");
        System.clearProperty("setMineTime");
    }

    private Robot launchIntruder(String name) {
        Robot robot = new Robot(name, world, 0, 0, RobotTypes.INTRUDER);
        world.addRobot(robot);
        response.handleResponse(new LaunchCommand(name, new JSONArray("[\"intruder\"]")));
        return robot;
    }

    @Test
    void aFreshLaunchLeavesTheStatusNormal() {
        Robot robot = launchIntruder("hal");
        assertEquals("NORMAL", robot.getStatus());
    }

    @Test
    void repairPutsTheStatusIntoRepairDuringTheCommand() {
        Robot robot = launchIntruder("hal");
        new RepairCommand("hal", new JSONArray()).execute(world);
        assertEquals("REPAIR", robot.getStatus(),
                "direct execute leaves the command's status (Response would reset it)");
    }

    @Test
    void aSuccessfulForwardResetsRepairToNormal() {
        Robot robot = launchIntruder("hal");
        robot.setStatus("REPAIR");
        response.handleResponse(new ForwardCommand("hal", new JSONArray("[1]")));
        assertEquals("NORMAL", robot.getStatus());
    }

    @Test
    void deathSetsTheStatusToDead() {
        Robot robot = launchIntruder("hal");
        robot.setDead();
        assertEquals("DEAD", robot.getStatus());
    }
}
