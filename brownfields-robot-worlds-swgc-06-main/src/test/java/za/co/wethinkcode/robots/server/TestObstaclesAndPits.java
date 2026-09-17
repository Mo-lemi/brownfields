package za.co.wethinkcode.robots.server;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.client.clientcommands.Command;
import za.co.wethinkcode.robots.client.clientcommands.ForwardCommand;
import za.co.wethinkcode.robots.client.clientcommands.LookCommand;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for obstacles and bottomless pits:
 * coordinate configuration, the no-overlap rule for obstacles, pits that
 * may overlap, fatal pit entry, and pit visibility in the look results.
 */
public class TestObstaclesAndPits {

    private World world;

    @BeforeEach
    void setUp() {
        System.setProperty("width", "10");
        System.setProperty("height", "10");
        System.setProperty("lookDistance", "4");
        System.setProperty("setMineTime", "0");
        System.setProperty("obstacles", "none");
        System.setProperty("pits", "none");
        world = new World();
    }

    @AfterEach
    void tearDown() {
        // these properties are JVM global - clear them so other test
        // classes keep their expected defaults
        System.clearProperty("width");
        System.clearProperty("height");
        System.clearProperty("lookDistance");
        System.clearProperty("setMineTime");
        System.clearProperty("obstacles");
        System.clearProperty("pits");
    }

    private Robot launch(String name, int x, int y) {
        Robot robot = new Robot(name, world, x, y, RobotTypes.SOLDIER);
        world.addRobot(robot);
        return robot;
    }

    @Test
    void severalObstaclesCanBeConfigured() {
        System.setProperty("obstacles", "0,1;2,2");
        world = new World();

        assertNotNull(world.getAObstacle(0, 1));
        assertNotNull(world.getAObstacle(2, 2));
        assertEquals(2, world.getObstacles().size());
    }

    @Test
    void obstaclesCannotSitOnTheSameSpot() {
        System.setProperty("obstacles", "0,1;0,1");
        world = new World();

        assertEquals(1, world.getObstacles().size(),
                "two obstacles cannot share the same coordinate");
    }

    @Test
    void obstaclesCanBePlacedNextToEachOther() {
        System.setProperty("obstacles", "0,1;1,1");
        world = new World();

        assertEquals(2, world.getObstacles().size());
        assertNotNull(world.getAObstacle(0, 1));
        assertNotNull(world.getAObstacle(1, 1));
    }

    @Test
    void obstaclesBlockThePathOfRobots() {
        System.setProperty("obstacles", "0,1;0,2");
        world = new World();
        Robot robot = launch("walker", 0, 0);

        String result = new ForwardCommand("walker", new JSONArray("[2]")).execute(world);

        assertTrue(result.contains("Obstructed"), result);
        assertEquals(0, robot.getX());
        assertEquals(0, robot.getY());
    }

    @Test
    void pitsCanOverlapEachOther() {
        System.setProperty("pits", "0,2;0,2");
        world = new World();

        assertEquals(2, world.getPits().size(), "pits may overlap - both are kept");
        assertTrue(world.hasPitAt(0, 2));
    }

    @Test
    void robotDiesWhenItEntersAPit() {
        System.setProperty("pits", "0,2");
        world = new World();
        Robot robot = launch("walker", 0, 0);

        // two steps north: the robot ends up inside the pit
        String result = new ForwardCommand("walker", new JSONArray("[2]")).execute(world);

        assertTrue(result.startsWith("OK#"), result);
        assertTrue(result.contains("Fell"), result);
        assertTrue(robot.isDead(), "entering a bottomless pit must kill the robot");
        assertNull(world.getARobot("walker"), "a dead robot is removed from the world");
    }

    @Test
    void pitDoesNotBlockThePathOfARobotThatSurvives() {
        System.setProperty("pits", "5,5");
        world = new World();
        Robot robot = launch("walker", 0, 0);

        // the pit is far away - normal movement still works
        String result = new ForwardCommand("walker", new JSONArray("[2]")).execute(world);

        assertEquals("OK#Done", result);
        assertFalse(robot.isDead());
    }

    @Test
    void lookReportsPitWithDirectionAndDistance() {
        System.setProperty("pits", "0,2");
        world = new World();
        launch("watcher", 0, 0);

        String result = new LookCommand("watcher", new JSONArray()).execute(world);
        JSONArray objects = new JSONArray(result.substring("OK#".length()));

        boolean found = false;
        for (int i = 0; i < objects.length(); i++) {
            JSONObject object = objects.getJSONObject(i);
            if ("PIT".equals(object.getString("type"))) {
                found = true;
                assertEquals("NORTH", object.getString("direction"));
                assertEquals(2, object.getInt("distance"));
            }
        }
        assertTrue(found, "the pit must show up in the look results");
    }

    @Test
    void lookSeesPastAPit() {
        // a pit at distance 1, an obstacle behind it at distance 4
        System.setProperty("pits", "0,1");
        System.setProperty("obstacles", "0,4");
        world = new World();
        launch("watcher", 0, 0);

        String result = new LookCommand("watcher", new JSONArray()).execute(world);
        JSONArray objects = new JSONArray(result.substring("OK#".length()));

        boolean sawPit = false;
        boolean sawObstacle = false;
        for (int i = 0; i < objects.length(); i++) {
            JSONObject object = objects.getJSONObject(i);
            if ("PIT".equals(object.getString("type")) && object.getInt("distance") == 1) {
                sawPit = true;
            }
            if ("OBSTACLE".equals(object.getString("type")) && object.getInt("distance") == 4) {
                sawObstacle = true;
            }
        }
        assertTrue(sawPit, "the pit must be visible");
        assertTrue(sawObstacle, "pits are see-through - the obstacle behind is visible too");
    }

    @Test
    void mineAutoForwardIntoPitKillsTheRobot() {
        System.setProperty("pits", "0,1");
        world = new World();
        Robot robot = new Robot("digger", world, 0, 0, RobotTypes.INTRUDER);
        world.addRobot(robot);

        String result = Command.create("mine", "digger", new JSONArray()).execute(world);

        assertTrue(result.startsWith("OK#"), result);
        assertTrue(result.contains("Fell"), result);
        assertTrue(robot.isDead(), "the automatic step walked the robot into the pit");
        // the mine was placed before the fatal step and stays behind
        assertTrue(world.hasMineAt(0, 0));
    }

    @Test
    void bareWorldHasAtLeastOneObstacleAndOnePit() {
        // nothing configured at all - the world guarantees the minimum
        System.clearProperty("obstacles");
        System.clearProperty("pits");
        world = new World();

        assertEquals(1, world.getObstacles().size(), "the world must have at least one obstacle");
        assertEquals(1, world.getPits().size(), "the world must have at least one pit");
        // neither sits on the launch coordinate
        assertFalse(world.hasPitAt(0, 0));
        assertNull(world.getAObstacle(0, 0));
    }

    @Test
    void configuredWorldIsTakenAsItIs() {
        // an explicitly configured world is not "helped" with extra objects
        System.setProperty("obstacles", "0,1");
        world = new World();

        assertEquals(1, world.getObstacles().size());
        assertEquals(0, world.getPits().size(), "no pit is invented for a configured world");
    }
}
