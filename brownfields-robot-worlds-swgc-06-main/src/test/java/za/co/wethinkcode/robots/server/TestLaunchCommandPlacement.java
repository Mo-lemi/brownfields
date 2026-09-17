package za.co.wethinkcode.robots.server;

import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.client.clientcommands.LaunchCommand;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LaunchCommand's placement rules: the deterministic
 * closest-to-centre order, duplicate names, invalid kinds, and full
 * worlds.
 */
public class TestLaunchCommandPlacement {

    @BeforeEach
    void setUp() {
        System.setProperty("width", "6");
        System.setProperty("height", "6");
        System.setProperty("obstacles", "none");
        System.setProperty("pits", "none");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("width");
        System.clearProperty("height");
        System.clearProperty("obstacles");
        System.clearProperty("pits");
    }

    private World world = new World();

    private String launch(World world, String name, String type) {        return new LaunchCommand(name, new JSONArray("[\"" + type + "\"]")).execute(world);
    }

    @Test
    void theFirstRobotLaunchesAtTheCentre() {
        World world = new World();
        String result = launch(world, "hal", "sniper");
        assertEquals("OK", result);
        assertEquals(0, world.getARobot("hal").getX());
        assertEquals(0, world.getARobot("hal").getY());
    }

    @Test
    void theSecondRobotTakesTheNearestFreeCell() {
        World world = new World();
        launch(world, "hal", "sniper");

        launch(world, "bob", "sniper");

        // the free cells are sorted by Manhattan distance, ties in
        // insertion order: the first distance-1 cell in row-major order
        assertEquals(-1, world.getARobot("bob").getX());
        assertEquals(0, world.getARobot("bob").getY());
    }

    @Test
    void duplicateNamesAreRejected() {
        World world = new World();
        launch(world, "hal", "sniper");

        String result = launch(world, "hal", "tank");

        assertEquals("ERROR#Too many of you in this world", result);
    }

    @Test
    void unknownKindsAreRejected() {
        World world = new World();
        String result = launch(world, "hal", "dragon");
        assertEquals("ERROR#Not a valid robot type", result);
        assertNull(world.getARobot("hal"));
    }

    @Test
    void obstacleCellsAreNeverUsedForLaunches() {
        System.setProperty("obstacles", "0,1;-1,0");
        World world = new World();
        launch(world, "hal", "sniper");   // (0,0)
        launch(world, "bob", "sniper");   // (-1,0) is an obstacle now

        assertNotNull(world.getARobot("bob"));
        int x = world.getARobot("bob").getX();
        int y = world.getARobot("bob").getY();
        boolean onObstacle = (x == 0 && y == 1) || (x == -1 && y == 0);
        assertFalse(onObstacle, "robots never launch onto an obstacle");
    }


    @Test
    void intrudersLaunchWithZeroShots() {
        World world = new World();
        launch(world, "digger", "intruder");
        assertEquals(0, world.getARobot("digger").getShots());
    }
}
