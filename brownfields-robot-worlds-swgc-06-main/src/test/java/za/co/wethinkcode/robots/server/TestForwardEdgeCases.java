package za.co.wethinkcode.robots.server;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.client.clientcommands.ForwardCommand;
import za.co.wethinkcode.robots.server.obstacles.MountainObstacle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ForwardCommand's edges, obstructions, mines and pits
 * along the path.
 */
public class TestForwardEdgeCases {

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

    private String forward(String name, int steps) {
        return new ForwardCommand(name, new JSONArray("[" + steps + "]")).execute(world);
    }

    @Test
    void aClearMoveReportsDoneAndUpdatesPosition() {
        launch("hal", 0, 0, Direction.NORTH);
        assertEquals("OK#Done", forward("hal", 3));
        assertEquals(0, world.getARobot("hal").getX());
        assertEquals(3, world.getARobot("hal").getY());
    }

    @Test
    void theNorthEdgeIsReportedWhenBlockedByIt() {
        launch("hal", 0, 0, Direction.NORTH);
        Robot robot = world.getARobot("hal");
        robot.setPosition(0, 19); // at the top of a 20-high world

        String result = forward("hal", 1);

        assertEquals("OK#At the NORTH edge", result);
        assertEquals(19, world.getARobot("hal").getY());
    }

    @Test
    void theSouthEdgeIsReportedWhenBlockedByIt() {
        launch("hal", 0, 0, Direction.SOUTH);
        String result = forward("hal", 1);
        assertEquals("OK#At the SOUTH edge", result);
    }

    @Test
    void theEastEdgeIsReportedWhenBlockedByIt() {
        launch("hal", 19, 0, Direction.EAST);
        String result = forward("hal", 1);
        assertEquals("OK#At the EAST edge", result);
    }

    @Test
    void theWestEdgeIsReportedWhenBlockedByIt() {
        launch("hal", 0, 0, Direction.WEST);
        String result = forward("hal", 1);
        assertEquals("OK#At the WEST edge", result);
    }

    @Test
    void anObstacleAtStepOneBlocksEverything() {
        world.addObstacle(new MountainObstacle(0, 1));
        launch("hal", 0, 0, Direction.NORTH);

        String result = forward("hal", 5);

        assertEquals("OK#Obstructed#Moved 0 steps blocked by:OBSTACLE", result);
        assertEquals(0, world.getARobot("hal").getY());
    }

    @Test
    void anObstacleMidPathStopsTheMoveWithTheDistanceTravelled() {
        world.addObstacle(new MountainObstacle(0, 3));
        launch("hal", 0, 0, Direction.NORTH);

        String result = forward("hal", 5);

        assertEquals("OK#Obstructed#Moved 2 steps blocked by:OBSTACLE", result);
        assertEquals(2, world.getARobot("hal").getY());
    }

    @Test
    void aMineMidPathStopsTheMoveAndDetonates() {
        world.placeMine(0, 2);
        launch("hal", 0, 0, Direction.NORTH);

        String result = forward("hal", 5);

        assertTrue(result.contains("mine"), result);
        assertEquals(2, world.getARobot("hal").getY(), "the move ends on the mine");
        assertEquals(0, world.getARobot("hal").getShield());
        assertFalse(world.hasMineAt(0, 2), "the mine is consumed");
    }

    @Test
    void forwardWithoutArgumentsIsAProtocolError() {
        launch("hal", 0, 0, Direction.NORTH);
        assertThrows(Exception.class,
                () -> new ForwardCommand("hal", new JSONArray()).execute(world));
    }
}
