package za.co.wethinkcode.robots.server;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.client.clientcommands.*;
import za.co.wethinkcode.robots.server.obstacles.MountainObstacle;
import za.co.wethinkcode.robots.server.servercommands.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCommands {
    private za.co.wethinkcode.robots.server.World world;
    private Robot jim;
    private Robot john;
    private Robot anna;
    private Robot mimi;
    private Robot pam;
    private Robot james;
    private Robot max;

    @BeforeEach
    void setUp() {
        System.clearProperty("obstacles");
        //System.setProperty("width", "20");
        //System.setProperty("height", "20");

        //world = new za.co.wethinkcode.robots.server.World();
        world = new World(20, 20);

        max = new Robot("max", world, 7, 4, RobotTypes.SNIPER);
        james = new Robot("james", world, 1, 12, RobotTypes.SNIPER);
        pam = new Robot("pam", world, 5, 9, RobotTypes.JUGGERNAUT);
        mimi = new Robot("mimi", world, 4, 5, RobotTypes.SNIPER);
        anna = new Robot("anna", world, 7, 2, RobotTypes.SNIPER);
        john = new Robot("john", world, 3, 7, RobotTypes.SNIPER);
        jim = new Robot("jim", world, 0, 0, RobotTypes.SNIPER);
    }

    @Test
    void ServerCommand(){

        ServerCommand command = new ErrorCommand(world);
        assertEquals("error", command.getName());

        command = new DumpCommand(world);
        assertEquals("dump", command.getName());

        command = new QuitCommand(world);
        assertEquals("quit", command.getName());

        command = new RobotsCommand(world);
        assertEquals("robots", command.getName());
    }


    @Test
    void turnCommand(){
        world.addRobot(jim);

        assertEquals(Direction.NORTH, jim.getDirection());

        JSONArray args = new JSONArray(List.of("left"));
        Command turn = new TurnCommand("jim", args);
        turn.execute(world);

        assertEquals(Direction.WEST, jim.getDirection());
        turn.execute(world);
        assertEquals(Direction.SOUTH, jim.getDirection());

        args = new JSONArray(List.of("right"));
        turn = new TurnCommand("jim", args);

        turn.execute(world);
        assertEquals(Direction.WEST, jim.getDirection());
    }

    @Test
    void backCommand(){

        world.addRobot(max);

        JSONArray args = new JSONArray(List.of(1));
        Command back = new BackCommand("max", args);
        back.execute(world);

        assertEquals(3, max.getY());
    }

    @Test
    void forwardCommand(){

        world.addRobot(james);

        JSONArray args = new JSONArray(List.of(1));
        Command forward = new ForwardCommand("james", args);
        forward.execute(world);

        assertEquals(13, james.getY());
    }

    @Test
    void reloadCommand(){

        world.addRobot(pam);

        JSONArray args = new JSONArray();
        Command reload = new ReloadCommand("pam", args);
        pam.setShots(0);
        reload.execute(world);
        assertEquals(5, pam.getShots());
    }

    @Test
    void repairCommand(){
        world.addRobot(mimi);

        JSONArray args = new JSONArray();
        Command repair = new RepairCommand("mimi", args);
        mimi.takeHit();
        repair.execute(world);
        assertEquals(1, mimi.getShield());
    }

    @Test
    void stateCommand(){
        world.addRobot(john);

        JSONArray args = new JSONArray();
        Command state = new StateCommand("john", args);
        String result = state.execute(world);
        assertEquals("OK", result);
    }

    @Test
    void fireCommand(){
        world.addRobot(anna);

        JSONArray args = new JSONArray();
        Command fire = new FireCommand("anna", args);

        fire.execute(world);
        assertEquals(0, anna.getShots());
    }

    @Test
    void reloadCommandRespectsClassAmmoCap() {
        // GIVEN a Sniper with a maximum capacity of 1 shot
        World localWorld = new World(10, 10);
        Robot sniper = new Robot("hawkeye", localWorld, 0, 0, RobotTypes.SNIPER);
        localWorld.addRobot(sniper);

        // Empty the weapon
        sniper.setShots(0);

        // WHEN the reload command is executed
        Command reload = new ReloadCommand("hawkeye", new JSONArray());
        reload.execute(localWorld);

        // THEN it should only reload 1 shot, not the world default of 5
        assertEquals(1, sniper.getShots(), "Sniper should reload to max 1 shot");
    }

    @Test
    void launchCommandPrioritizesCenterSpawn() {
        // Clear the world to ensure it's empty before launching
        world.getRobots().clear();

        JSONArray args = new JSONArray();
        args.put("sniper");
        Command launch = new LaunchCommand("centerBot", args);

        String response = launch.execute(world); // Use the global 'world' instance

        assertEquals("OK", response);
        Robot spawned = world.getARobot("centerBot");
        Assertions.assertNotNull(spawned);
        assertEquals(0, spawned.getX(), "First spawn must default to X=0");
        assertEquals(0, spawned.getY(), "First spawn must default to Y=0");
    }


    @Test
    void lookAroundWithObjectInView() {
        // GIVEN my robot is launched and there is an obstacle 2 steps ahead to the North
        World localWorld = new World(1, 3);
        Robot robot = new Robot("HAL", localWorld, 0, 0, RobotTypes.SNIPER);
        localWorld.addRobot(robot);
        localWorld.addObstacle(new MountainObstacle(0, 2));

        // WHEN I send a request with the command "look"
        Command look = new LookCommand("HAL", new JSONArray());

        // execute() returns a string formatted as "OK#[{"distance":2,"direction":"NORTH",...}]"
        String responseString = look.execute(localWorld);

        // THEN I should get an OK response
        assertTrue(responseString.startsWith("OK"), "Response should start with OK");

        // AND the response objects array should contain an entry with direction "NORTH", type "OBSTACLE", and distance 2
        String jsonArrayPart = responseString.split("#")[1];
        JSONArray objects = new JSONArray(jsonArrayPart);

        boolean foundObstacle = false;
        for (int i = 0; i < objects.length(); i++) {
            JSONObject obj = objects.getJSONObject(i);

            // Guard clauses: if it doesn't match, skip to the next object
            if (!obj.getString("direction").equals("NORTH")) {
                continue;
            }
            if (!obj.getString("type").contains("OBSTACLE")) {
                continue;
            }
            if (obj.getInt("distance") != 2) {
                continue;
            }

            // If it passes all the checks above, we found our obstacle!
            foundObstacle = true;
            break;
        }

        assertTrue(foundObstacle, "Expected an OBSTACLE to be NORTH at distance 2. Actual: " + objects.toString());
    }
    @Test
    void fireCommandDetectsHitAndAppliesDamage() {
        // GIVEN a Shooter and a target aligned to the North
        World localWorld = new World(10, 10);
        Robot shooter = new Robot("shooter", localWorld, 0, 0, RobotTypes.SOLDIER);
        Robot target = new Robot("target", localWorld, 0, 2, RobotTypes.SNIPER);
        localWorld.addRobot(shooter);
        localWorld.addRobot(target);

        // WHEN the fire command is executed
        Command fire = new FireCommand("shooter", new JSONArray());
        String response = fire.execute(localWorld);

        // THEN it should report a Hit at distance 2 and reduce target shields
        assertTrue(response.contains("Hit"), "Response should indicate a Hit");
        assertTrue(response.contains("target"), "Response should name the hit target");
        assertEquals(1, target.getShield(), "Sniper target should take fatal damage or be destroyed");
    }

    @Test
    void fireCommandDetectsMissWhenPathClear() {
        // GIVEN a Shooter with no robot in the line of fire
        World localWorld = new World(10, 10);
        Robot shooter = new Robot("shooter", localWorld, 0, 0, RobotTypes.SOLDIER);
        localWorld.addRobot(shooter);

        // WHEN the fire command is executed
        Command fire = new FireCommand("shooter", new JSONArray());
        String response = fire.execute(localWorld);

        // THEN it should report a Miss
        assertTrue(response.contains("Miss"), "Response should indicate a Miss");
        assertEquals(2, shooter.getShots(), "Shooter should have decremented ammo");
    }
}

