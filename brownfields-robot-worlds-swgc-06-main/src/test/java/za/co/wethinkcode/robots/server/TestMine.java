package za.co.wethinkcode.robots.server;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.client.clientcommands.BackCommand;
import za.co.wethinkcode.robots.client.clientcommands.Command;
import za.co.wethinkcode.robots.client.clientcommands.ForwardCommand;
import za.co.wethinkcode.robots.client.clientcommands.LookCommand;
import za.co.wethinkcode.robots.client.clientcommands.TurnCommand;
import za.co.wethinkcode.robots.server.protocol.Request;

import static org.junit.jupiter.api.Assertions.*;

class TestMine {
    private static final String ROBOT_NAME = "digger";
    private World world;

    @BeforeEach
    void setUp() {
        System.setProperty("width", "10");
        System.setProperty("height", "10");
        System.setProperty("obstacles", "none");
        System.setProperty("lookDistance", "4");
        System.setProperty("setMineTime", "0");
        world = new World();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("width");
        System.clearProperty("height");
        System.clearProperty("obstacles");
        System.clearProperty("lookDistance");
        System.clearProperty("setMineTime");
    }

    private Robot launchIntruder(String name, int x, int y) {
        Robot robot = new Robot(name, world, x, y, RobotTypes.INTRUDER);
        world.addRobot(robot);
        return robot;
    }

    private String mineCommand(String robotName) {
        return Command.create("mine", robotName, new JSONArray()).execute(world);
    }

    private Robot launchDefaultIntruder() {
        return launchIntruder(ROBOT_NAME, 0, 0);
    }

    @Test
    void intruderCanPlaceMineAndAutomaticallyMovesForward() {
        Robot robot = launchDefaultIntruder();
        assertEquals(Direction.NORTH, robot.getDirection());
        String result = mineCommand("digger");
        assertTrue(result.startsWith("OK#"));

        assertTrue(world.hasMineAt(0, 0));
        assertRobotPosition(robot, 0, 1);
        assertFalse(world.hasMineAt(0, 1));
        assertEquals("SETMINE", robot.getStatus());
    }

    @Test
    void gunRobotCannotPlaceMine() {
        Robot soldier = new Robot("shooter", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(soldier);
        String result = mineCommand("shooter");
        assertTrue(result.startsWith("ERROR#"));
        assertFalse(world.hasMineAt(0, 0));
    }

    @Test
    void cannotPlaceMineOnAnOccupiedCoordinate() {
        launchDefaultIntruder();
        world.placeMine(0, 0);
        String result = mineCommand("digger");
        assertTrue(result.startsWith("ERROR#"));
    }

    @Test
    void robotCannotMoveWhileSettingMine() {
        Robot robot = launchDefaultIntruder();
        robot.setStatus("SETMINE");

        assertTrue(new ForwardCommand("digger", new JSONArray("[1]")).execute(world).startsWith("ERROR#"));
        assertTrue(new BackCommand("digger", new JSONArray("[1]")).execute(world).startsWith("ERROR#"));
        assertTrue(new TurnCommand("digger", new JSONArray("[\"left\"]")).execute(world).startsWith("ERROR#"));
        assertTrue(mineCommand("digger").startsWith("ERROR#"));

        assertRobotPosition(robot, 0, 0);
    }

    @Test
    void shieldsAreDisabledWhileSettingMine() {
        Robot robot = launchDefaultIntruder();
        robot.startMining();
        robot.takeHit();
        assertTrue(robot.isDead());
    }

    @Test
    void blockedAutoForwardStepsOntoOwnMine() {
        System.setProperty("obstacles", "0,1");
        world = new World();
        Robot robot = launchIntruder("digger", 0, 0);
        int shieldsBefore = robot.getShield();

        String result = mineCommand("digger");
        assertTrue(result.startsWith("OK#"));
        assertTrue(result.contains("own mine"));

        assertRobotPosition(robot, 0, 0);
        assertFalse(world.hasMineAt(0, 0));
        assertEquals(Math.max(0, shieldsBefore - 3), robot.getShield());
        assertEquals("SETMINE", robot.getStatus());
    }

    @Test
    void steppingOnAMineReducesShieldsByThree() {
        Robot robot = new Robot("walker", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);
        world.placeMine(0, 1);
        int shieldsBefore = robot.getShield();

        String result = new ForwardCommand("walker", new JSONArray("[1]")).execute(world);
        assertTrue(result.startsWith("OK#"));
        assertTrue(result.contains("mine"));

        assertRobotPosition(robot, 0, 1);
        assertEquals(Math.max(0, shieldsBefore - 3), robot.getShield());
        assertFalse(world.hasMineAt(0, 1));
    }

    @Test
    void ownMineDamagesJustTheSame() {
        Robot robot = launchIntruder("digger", 0, 0);
        int shieldsBefore = robot.getShield();
        world.placeMine(0, 1);

        String result = new ForwardCommand("digger", new JSONArray("[1]")).execute(world);
        assertTrue(result.startsWith("OK#"));
        assertEquals(Math.max(0, shieldsBefore - 3), robot.getShield());
    }

    @Test
    void mineDetonatesOnlyOnce() {
        launchIntruder("digger", 0, 0);
        world.placeMine(0, 1);
        new ForwardCommand("digger", new JSONArray("[1]")).execute(world);
        Robot robot = world.getARobot("digger");
        int shieldsAfterFirstHit = robot.getShield();

        String secondMove = new ForwardCommand("digger", new JSONArray("[1]")).execute(world);
        assertEquals("OK#Done", secondMove);
        assertEquals(shieldsAfterFirstHit, robot.getShield());
        assertRobotPosition(robot, 0, 2);
    }

    @Test
    void movingBackwardsOntoAMineAlsoHurts() {
        Robot robot = launchIntruder("digger", 0, 1);
        int shieldsBefore = robot.getShield();
        world.placeMine(0, 0);

        String result = new BackCommand("digger", new JSONArray("[1]")).execute(world);
        assertTrue(result.startsWith("OK#"));
        assertRobotPosition(robot, 0, 0);
        assertEquals(Math.max(0, shieldsBefore - 3), robot.getShield());
    }

    @Test
    void mineIsDetectedWithinQuarterOfVisibilityRange() {
        launchIntruder("digger", 0, 0);
        world.placeMine(0, 1);

        String result = new LookCommand("digger", new JSONArray()).execute(world);
        JSONArray objects = new JSONArray(result.substring("OK#".length()));
        boolean found = false;

        for (int i = 0; i < objects.length(); i++) {
            JSONObject object = objects.getJSONObject(i);
            if ("MINE".equals(object.getString("type"))) {
                found = true;
                assertEquals(1, object.getInt("distance"));
                assertEquals("NORTH", object.getString("direction"));
            }
        }
        assertTrue(found);
    }

    @Test
    void mineBeyondQuarterOfVisibilityRangeIsInvisible() {
        launchIntruder("digger", 0, 0);
        world.placeMine(0, 2);

        String result = new LookCommand("digger", new JSONArray()).execute(world);
        JSONArray objects = new JSONArray(result.substring("OK#".length()));

        for (int i = 0; i < objects.length(); i++) {
            assertNotEquals("MINE", objects.getJSONObject(i).getString("type"));
        }
    }

    @Test
    void fullRequestFlowReturnsOkAndState() {
        launchIntruder("digger", 0, 0);
        String response = new Request().handleRequest(
                "{\"robot\": \"digger\", \"command\": \"mine\", \"arguments\": []}", world);
        JSONObject json = new JSONObject(response);

        assertEquals("OK", json.getString("result"));
        assertEquals("Done", json.getJSONObject("data").getString("message"));
        assertEquals("SETMINE", json.getJSONObject("state").getString("status"));
        assertEquals(0, json.getJSONObject("state").getJSONArray("position").getInt(0));
        assertEquals(1, json.getJSONObject("state").getJSONArray("position").getInt(1));
    }

    @Test
    void robotDestroyedByItsOwnMineIsHandledWithoutState() {
        System.setProperty("obstacles", "0,1");
        world = new World();
        Robot doomed = new Robot("digger", world, 0, 0, RobotTypes.INTRUDER);
        world.addRobot(doomed);
        doomed.takeHit();

        String response = new Request().handleRequest(
                "{\"robot\": \"digger\", \"command\": \"mine\", \"arguments\": []}", world);
        JSONObject json = new JSONObject(response);

        assertTrue(json.has("state"));
        assertEquals("DEAD", json.getJSONObject("state").getString("status"));
        assertNull(world.getARobot("digger"));
        assertTrue(doomed.isDead());
    }

    // --- Helper Methods ---

    private void assertRobotPosition(Robot robot, int expectedX, int expectedY) {
        assertEquals(expectedX, robot.getX());
        assertEquals(expectedY, robot.getY());
    }
}