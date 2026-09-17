package za.co.wethinkcode.robots.server;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.client.clientcommands.Command;
import za.co.wethinkcode.robots.client.clientcommands.MineCommand;

import static org.junit.jupiter.api.Assertions.*;

/**
 * More MineCommand unit tests: the per-kind rejection, the messages, and
 * mines interacting with later movement.
 */
public class TestMineCommandMore {

    private World world;

    @BeforeEach
    void setUp() {
        System.setProperty("setMineTime", "0");
        System.setProperty("obstacles", "none");
        System.setProperty("pits", "none");
        world = new World(20, 20);
    }

    private String mine(String name) {
        return Command.create("mine", name, new JSONArray()).execute(world);
    }

    private Robot launch(String name, int x, int y, RobotTypes type) {
        Robot robot = new Robot(name, world, x, y, type);
        world.addRobot(robot);
        return robot;
    }

    @Test
    void everyGunKindIsRejected() {
        RobotTypes[] gunKinds = {
                RobotTypes.JUGGERNAUT, RobotTypes.TANK, RobotTypes.SOLDIER,
                RobotTypes.MARKSMAN, RobotTypes.SNIPER};
        for (RobotTypes kind : gunKinds) {
            World fresh = new World(20, 20);
            Robot robot = new Robot(kind.name().toLowerCase(), fresh, 0, 0, kind);
            fresh.addRobot(robot);
            String result = Command.create("mine", kind.name().toLowerCase(),
                    new JSONArray()).execute(fresh);
            assertTrue(result.startsWith("ERROR#"), kind + " should not mine: " + result);
        }
    }

    @Test
    void aDeadRobotCannotMine() {
        Robot robot = launch("digger", 0, 0, RobotTypes.INTRUDER);
        robot.setDead(); // removed from the world

        String result = mine("digger");

        assertEquals("Error#robot not found", result);
    }


    @Test
    void walkingBackOverYourOwnMineDetonatesIt() {
        launch("digger", 0, 0, RobotTypes.INTRUDER);
        world.placeMine(0, 1);   // a mine one step ahead

        String result = new za.co.wethinkcode.robots.client.clientcommands.ForwardCommand(
                "digger", new JSONArray("[1]")).execute(world);

        assertTrue(result.contains("mine"), result);
        assertEquals(0, world.getARobot("digger").getShield(), "3 shields lost");
    }

    @Test
    void doneIsTheProtocolMessageForACleanMine() {
        launch("digger", 0, 0, RobotTypes.INTRUDER);
        assertEquals("OK#Done", mine("digger"));
    }

    @Test
    void fellIsTheProtocolMessageForAPitFall() {
        world.addPit(new za.co.wethinkcode.robots.server.obstacles.PitObstacle(0, 1));
        launch("digger", 0, 0, RobotTypes.INTRUDER);

        String result = mine("digger");

        assertEquals("OK#Fell", result);
        assertTrue(world.getARobot("digger") == null
                        || world.getARobot("digger").isDead(),
                "falling into the pit is fatal");
    }

}
