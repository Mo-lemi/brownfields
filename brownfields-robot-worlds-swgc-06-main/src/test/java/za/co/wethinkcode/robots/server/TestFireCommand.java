package za.co.wethinkcode.robots.server;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.client.clientcommands.FireCommand;
import za.co.wethinkcode.robots.server.obstacles.MountainObstacle;
import za.co.wethinkcode.robots.server.protocol.Request;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the fire command: ammo handling, hits, misses, gun range
 * and obstacles blocking the line of fire.
 */
public class TestFireCommand {

    private World world;

    @BeforeEach
    void setUp() {
        System.setProperty("obstacles", "none");
        System.setProperty("pits", "none");
        world = new World(20, 20);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("obstacles");
        System.clearProperty("pits");
    }

    private Robot launch(String name, int x, int y, RobotTypes type) {
        Robot robot = new Robot(name, world, x, y, type);
        world.addRobot(robot);
        return robot;
    }

    private String fire(String name) {
        return new FireCommand(name, new JSONArray()).execute(world);
    }

    @Test
    void fireWithNoShotsFails() {
        launch("shooter", 0, 0, RobotTypes.SOLDIER);
        world.getARobot("shooter").setShots(0);

        String result = fire("shooter");

        assertEquals("ERROR#No more shots left", result);
    }

    @Test
    void fireSpendsOneShotPerTriggerPull() {
        launch("shooter", 0, 0, RobotTypes.SOLDIER);
        fire("shooter");
        assertEquals(2, world.getARobot("shooter").getShots());
    }

    @Test
    void fireMissesIntoEmptySpace() {
        launch("shooter", 0, 0, RobotTypes.SOLDIER);

        String result = fire("shooter");

        assertTrue(result.startsWith("OK#"), result);
        assertTrue(result.contains("Miss"), result);
    }

    @Test
    void fireHitsARobotStraightAhead() {
        launch("shooter", 0, 0, RobotTypes.SOLDIER);
        Robot victim = launch("target", 0, 2, RobotTypes.TANK);

        String result = fire("shooter");

        assertTrue(result.startsWith("OK#"), result);
        assertTrue(result.contains("Hit"), result);
        assertEquals(4, victim.getShield(), "the TANK's shield drops by one hit");
    }

    @Test
    void fireCannotReachBeyondTheGunRange() {
        // a SOLDIER's range is 3 - a robot 5 steps away is safe
        launch("shooter", 0, 0, RobotTypes.SOLDIER);
        launch("faraway", 0, 5, RobotTypes.TANK);

        String result = fire("shooter");

        assertTrue(result.contains("Miss"), result);
        assertEquals(4, world.getARobot("faraway").getShield(), "out of range: no damage");
    }

    @Test
    void fireDoesNotSeeThroughAMountain() {
        // an opaque mountain blocks the line of fire
        world.addObstacle(new MountainObstacle(0, 1));
        launch("shooter", 0, 0, RobotTypes.SOLDIER);
        launch("hidden", 0, 2, RobotTypes.TANK);

        String result = fire("shooter");

        assertTrue(result.contains("Miss"), result);
        assertEquals(4, world.getARobot("hidden").getShield());
    }

    @Test
    void fireThroughTheFullProtocolDamagesTheVictim() {
        // the hit damage is applied by the response layer - the full
        // Request path must deliver it
        launch("shooter", 0, 0, RobotTypes.SOLDIER);
        Robot victim = launch("target", 0, 2, RobotTypes.TANK);

        String response = new Request().handleRequest(
                "{\"robot\": \"shooter\", \"command\": \"fire\", \"arguments\": []}", world);
        JSONObject json = new JSONObject(response);

        assertEquals("OK", json.getString("result"));
        assertEquals("Hit", json.getJSONObject("data").getString("message"));
        assertEquals(3, victim.getShield(), "the victim loses one shield");
        assertEquals(2, world.getARobot("shooter").getShots());
    }
}
