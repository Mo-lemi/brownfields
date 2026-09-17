package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.json.JSONArray;
import za.co.wethinkcode.robots.client.clientcommands.ForwardCommand;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for forward movement in all four directions.
 */
public class TestForwardDirectionMatrix {

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

    private void forward(String name, int steps) {
        new ForwardCommand(name, new JSONArray("[" + steps + "]")).execute(world);
    }

    @Test
    void northIncreasesY() {
        launch("n", 10, 10, Direction.NORTH);
        forward("n", 3);
        assertEquals(10, world.getARobot("n").getX());
        assertEquals(13, world.getARobot("n").getY());
    }

    @Test
    void southDecreasesY() {
        launch("s", 10, 10, Direction.SOUTH);
        forward("s", 3);
        assertEquals(10, world.getARobot("s").getX());
        assertEquals(7, world.getARobot("s").getY());
    }

    @Test
    void eastIncreasesX() {
        launch("e", 10, 10, Direction.EAST);
        forward("e", 3);
        assertEquals(13, world.getARobot("e").getX());
        assertEquals(10, world.getARobot("e").getY());
    }

    @Test
    void westDecreasesX() {
        launch("w", 10, 10, Direction.WEST);
        forward("w", 3);
        assertEquals(7, world.getARobot("w").getX());
        assertEquals(10, world.getARobot("w").getY());
    }
}
