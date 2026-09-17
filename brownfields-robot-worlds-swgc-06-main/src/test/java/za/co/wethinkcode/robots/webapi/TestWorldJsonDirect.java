package za.co.wethinkcode.robots.webapi;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.server.Robot;
import za.co.wethinkcode.robots.server.RobotTypes;
import za.co.wethinkcode.robots.server.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit tests for the Web API's JSON serializer.
 */
public class TestWorldJsonDirect {

    private World world;

    @BeforeEach
    void setUp() {
        world = new World(20, 20);
    }

    @Test
    void anEmptyWorldSerializesWithEmptyArrays() {
        JSONObject json = WorldJson.of(world);

        assertEquals(20, json.getJSONObject("dimensions").getInt("width"));
        assertEquals(20, json.getJSONObject("dimensions").getInt("height"));
        assertEquals(0, json.getJSONArray("obstacles").length());
        assertEquals(0, json.getJSONArray("pits").length());
        assertEquals(0, json.getJSONArray("mines").length());
        assertEquals(0, json.getJSONArray("robots").length());
    }

    @Test
    void obstaclesSerializeWithTypeAndCoordinates() {
        world.addObstacle(new za.co.wethinkcode.robots.server.obstacles.MountainObstacle(3, 4));

        JSONObject json = WorldJson.of(world);
        JSONArray obstacles = json.getJSONArray("obstacles");

        assertEquals(1, obstacles.length());
        assertEquals("mountain", obstacles.getJSONObject(0).getString("type"));
        assertEquals(3, obstacles.getJSONObject(0).getInt("x"));
        assertEquals(4, obstacles.getJSONObject(0).getInt("y"));
    }

    @Test
    void pitsSerializeWithCoordinates() {
        world.addPit(new za.co.wethinkcode.robots.server.obstacles.PitObstacle(7, 8));

        JSONArray pits = WorldJson.of(world).getJSONArray("pits");
        assertEquals(1, pits.length());
        assertEquals(7, pits.getJSONObject(0).getInt("x"));
        assertEquals(8, pits.getJSONObject(0).getInt("y"));
    }

    @Test
    void minesSerializeWithCoordinates() {
        world.placeMine(9, 1);

        JSONArray mines = WorldJson.of(world).getJSONArray("mines");
        assertEquals(1, mines.length());
        assertEquals(9, mines.getJSONObject(0).getInt("x"));
        assertEquals(1, mines.getJSONObject(0).getInt("y"));
    }

    @Test
    void robotsSerializeWithTheirFullState() {
        Robot robot = new Robot("hal", world, 4, 6, RobotTypes.SOLDIER);
        world.addRobot(robot);

        JSONArray robots = WorldJson.of(world).getJSONArray("robots");
        assertEquals(1, robots.length());

        JSONObject hal = robots.getJSONObject(0);
        assertEquals("hal", hal.getString("name"));
        assertEquals(4, hal.getJSONArray("position").getInt(0));
        assertEquals(6, hal.getJSONArray("position").getInt(1));
        assertEquals("NORTH", hal.getString("direction"));
        assertEquals(3, hal.getInt("shields"));
        assertEquals(3, hal.getInt("shots"));
        assertEquals("NORMAL", hal.getString("status"));
    }

    @Test
    void multipleRobotsAllAppear() {
        world.addRobot(new Robot("a", world, 0, 0, RobotTypes.SOLDIER));
        world.addRobot(new Robot("b", world, 1, 1, RobotTypes.SNIPER));

        assertEquals(2, WorldJson.of(world).getJSONArray("robots").length());
    }
}
