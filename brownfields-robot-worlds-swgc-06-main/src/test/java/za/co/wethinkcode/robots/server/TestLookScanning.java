package za.co.wethinkcode.robots.server;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.client.clientcommands.LookCommand;
import za.co.wethinkcode.robots.server.obstacles.LakeObstacle;
import za.co.wethinkcode.robots.server.obstacles.MountainObstacle;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import za.co.wethinkcode.robots.server.Robot;
import org.json.JSONObject;

/**
 * Unit tests for the look scanner: what is reported per direction, how
 * transparency works, and how mines are detected.
 */
public class TestLookScanning {

    private JSONArray look(World world, Robot robot) {
        String result = new LookCommand(robot.getName(), new JSONArray()).execute(world);
        return new JSONArray(result.substring("OK#".length()));
    }

    private boolean hasObject(JSONArray objects, String type, String direction, int distance) {
        for (int i = 0; i < objects.length(); i++) {
            JSONObject o = objects.getJSONObject(i);
            if (type.equals(o.getString("type"))
                    && direction.equals(o.getString("direction"))
                    && distance == o.getInt("distance")) {
                return true;
            }
        }
        return false;
    }



    @Test
    void anOpaqueMountainHidesEverythingBehindIt() {
        World world = new World(20, 20);
        world.addObstacle(new MountainObstacle(0, 1));   // opaque, 1 step north
        Robot robot = new Robot("watcher", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);
        Robot hidden = new Robot("hidden", world, 0, 3, RobotTypes.SOLDIER);
        world.addRobot(hidden);

        JSONArray objects = look(world, robot);

        assertTrue(hasObject(objects, "OBSTACLE", "NORTH", 1), "the mountain is seen");
        assertFalse(hasObject(objects, "ROBOT", "NORTH", 3),
                "nothing behind an opaque mountain is reported");
        assertTrue(world.getARobot("hidden") != null, "the hidden robot still exists");
    }

    @Test
    void aLakeIsSeeThrough() {
        World world = new World(20, 20);
        world.addObstacle(new LakeObstacle(0, 1));
        world.addObstacle(new MountainObstacle(0, 3));
        Robot robot = new Robot("watcher", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);

        JSONArray objects = look(world, robot);

        assertTrue(hasObject(objects, "OBSTACLE", "NORTH", 1), "the lake is seen");
        assertTrue(hasObject(objects, "OBSTACLE", "NORTH", 3), "the mountain behind the lake is seen too");
    }

    @Test
    void robotsAreVisibleAtTheirDistance() {
        World world = new World(20, 20);
        Robot watcher = new Robot("watcher", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(watcher);
        world.addRobot(new Robot("target", world, 3, 0, RobotTypes.SOLDIER));

        JSONArray objects = look(world, watcher);

        assertTrue(hasObject(objects, "ROBOT", "EAST", 3), "the robot to the east is seen");
    }

    @Test
    void robotsBeyondTheVisibilityAreNotReported() {
        World world = new World(20, 20);   // lookDistance 5
        Robot watcher = new Robot("watcher", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(watcher);
        world.addRobot(new Robot("far", world, 8, 0, RobotTypes.SOLDIER));

        JSONArray objects = look(world, watcher);

        assertFalse(hasObject(objects, "ROBOT", "EAST", 8),
                "a robot 8 steps away is beyond the visibility of 5");
    }

    @Test
    void pitsAreReportedByTheScanner() {
        World world = new World(20, 20);
        world.addPit(new za.co.wethinkcode.robots.server.obstacles.PitObstacle(0, 1));
        Robot watcher = new Robot("watcher", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(watcher);

        JSONArray objects = look(world, watcher);

        assertTrue(hasObject(objects, "PIT", "NORTH", 1), "pits show as type PIT");
    }

    @Test
    void aPitDoesNotBlockTheRay() {
        World world = new World(20, 20);
        world.addPit(new za.co.wethinkcode.robots.server.obstacles.PitObstacle(0, 1));
        world.addObstacle(new MountainObstacle(0, 3));
        Robot watcher = new Robot("watcher", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(watcher);

        JSONArray objects = look(world, watcher);

        assertTrue(hasObject(objects, "PIT", "NORTH", 1));
        assertTrue(hasObject(objects, "OBSTACLE", "NORTH", 3), "the mountain behind the pit is visible");
    }
}
