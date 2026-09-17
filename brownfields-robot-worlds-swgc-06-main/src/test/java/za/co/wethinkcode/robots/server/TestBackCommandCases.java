package za.co.wethinkcode.robots.server;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.client.clientcommands.BackCommand;
import za.co.wethinkcode.robots.server.obstacles.MountainObstacle;
import za.co.wethinkcode.robots.server.obstacles.PitObstacle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BackCommand: free moves, the immediate-block error
 * format, multi-step obstruction, pits and the mining guard.
 */
public class TestBackCommandCases {

    private World world;

    @BeforeEach
    void setUp() {
        world = new World(20, 20);
    }

    private Robot launch(String name, int x, int y, Direction direction) {
        Robot robot = new Robot(name, world, x, y, RobotTypes.SOLDIER);
        robot.setDirection(direction);
        world.addRobot(robot);
        return robot;
    }

    private String back(String name, int steps) {
        return new BackCommand(name, new JSONArray("[" + steps + "]")).execute(world);
    }

    @Test
    void aClearBackwardMoveReportsDone() {
        // facing NORTH, backing moves SOUTH (y decreases)
        launch("hal", 0, 5, Direction.NORTH);
        String result = back("hal", 2);
        assertEquals("OK#Done", result);
        assertEquals(3, world.getARobot("hal").getY());
    }

    @Test
    void anImmediateObstacleIsReportedAsABlock() {
        world.addObstacle(new MountainObstacle(0, 4));
        launch("hal", 0, 5, Direction.NORTH);

        String result = back("hal", 1);

        assertEquals("ERROR#Blocked by OBSTACLE of world at [0,4]", result);
        assertEquals(5, world.getARobot("hal").getY(), "the robot did not move");
    }

    @Test
    void aLaterObstructionReportsTheDistanceMoved() {
        world.addObstacle(new MountainObstacle(0, 3));
        launch("hal", 0, 5, Direction.NORTH);

        String result = back("hal", 3);

        assertTrue(result.contains("Obstructed"), result);
        assertEquals(4, world.getARobot("hal").getY(), "one step was possible");
    }

    @Test
    void backingIntoAPitIsFatal() {
        world.addPit(new PitObstacle(0, 4));
        launch("hal", 0, 5, Direction.NORTH);

        String result = back("hal", 1);

        assertTrue(result.startsWith("OK#"), result);
        assertTrue(result.contains("Fell"), result);
        assertNull(world.getARobot("hal"), "dead robots leave the world");
    }


    @Test
    void backIsBlockedWhileSettingAMine() {
        Robot robot = launch("hal", 0, 5, Direction.NORTH);
        robot.startMining();

        String result = back("hal", 1);

        assertTrue(result.startsWith("ERROR#"), result);
        assertEquals(5, world.getARobot("hal").getY());
    }
}
