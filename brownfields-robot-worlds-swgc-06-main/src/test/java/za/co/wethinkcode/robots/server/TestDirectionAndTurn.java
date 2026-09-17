package za.co.wethinkcode.robots.server;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.client.clientcommands.TurnCommand;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for directions and turning: the full rotation cycles and
 * the turn command's guards.
 */
public class TestDirectionAndTurn {

    private World world;
    private Robot robot;

    @BeforeEach
    void setUp() {
        world = new World(20, 20);
        robot = new Robot("hal", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);
    }

    private TurnCommand turn(String direction) {
        return new TurnCommand("hal", new JSONArray("[\"" + direction + "\"]"));
    }

    @Test
    void thereAreExactlyFourDirections() {
        assertEquals(4, Direction.values().length);
        assertNotNull(Direction.valueOf("NORTH"));
        assertNotNull(Direction.valueOf("EAST"));
        assertNotNull(Direction.valueOf("SOUTH"));
        assertNotNull(Direction.valueOf("WEST"));
    }

    @Test
    void turningLeftCyclesThroughAllDirections() {
        TurnCommand turn = turn("left");
        assertEquals(Direction.WEST, turn.turnLeft(Direction.NORTH));
        assertEquals(Direction.SOUTH, turn.turnLeft(Direction.WEST));
        assertEquals(Direction.EAST, turn.turnLeft(Direction.SOUTH));
        assertEquals(Direction.NORTH, turn.turnLeft(Direction.EAST));
    }

    @Test
    void turningRightCyclesThroughAllDirections() {
        TurnCommand turn = turn("right");
        assertEquals(Direction.EAST, turn.turnRight(Direction.NORTH));
        assertEquals(Direction.SOUTH, turn.turnRight(Direction.EAST));
        assertEquals(Direction.WEST, turn.turnRight(Direction.SOUTH));
        assertEquals(Direction.NORTH, turn.turnRight(Direction.WEST));
    }

    @Test
    void turnExecutesAndChangesDirection() {
        assertEquals(Direction.NORTH, robot.getDirection());

        String result = turn("left").execute(world);

        assertEquals("OK#Done", result);
        assertEquals(Direction.WEST, robot.getDirection());
    }

    @Test
    void turnRejectsAnUnknownDirection() {
        assertEquals("ERROR", turn("up").execute(world));
        assertEquals(Direction.NORTH, robot.getDirection(), "no change on error");
    }

    @Test
    void turnIsBlockedWhileSettingAMine() {
        robot.startMining();

        String result = turn("left").execute(world);

        assertTrue(result.startsWith("ERROR#"), result);
        assertEquals(Direction.NORTH, robot.getDirection());
    }
}
