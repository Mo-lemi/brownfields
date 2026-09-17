package za.co.wethinkcode.robots.server;

import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.client.clientcommands.RepairCommand;
import za.co.wethinkcode.robots.client.clientcommands.ReloadCommand;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for repair and reload: both restore the robot to its type's
 * capacity (capped by the world config) after their configured delay.
 */
public class TestRepairReloadCommands {

    private World world;

    @BeforeEach
    void setUp() {
        // zero delay so the tests stay fast
        System.setProperty("obstacles", "none");
        System.setProperty("pits", "none");
        System.setProperty("reloadTime", "0");
        System.setProperty("repairTime", "0");
        world = new World(20, 20);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("obstacles");
        System.clearProperty("pits");
        System.clearProperty("reloadTime");
        System.clearProperty("repairTime");
    }

    @Test
    void repairRestoresDamagedShields() {
        Robot robot = new Robot("hal", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);
        robot.takeHit();
        assertEquals(2, robot.getShield());

        String result = new RepairCommand("hal", new JSONArray()).execute(world);

        assertEquals("OK#Done", result);
        assertEquals(3, robot.getShield(), "restored to the SOLDIER cap");
        assertEquals("REPAIR", robot.getStatus());
    }

    @Test
    void repairNeverExceedsTheTypeCap() {
        Robot robot = new Robot("hal", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);

        new RepairCommand("hal", new JSONArray()).execute(world);

        assertEquals(3, robot.getShield(), "the world allows 5, the type only 3");
    }

    @Test
    void reloadRestoresSpentShots() {
        Robot robot = new Robot("hal", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);
        robot.setShots(1);

        String result = new ReloadCommand("hal", new JSONArray()).execute(world);

        assertEquals("OK#Done", result);
        assertEquals(3, robot.getShots());
        assertEquals("RELOAD", robot.getStatus());
    }

    @Test
    void reloadGivesTheTypeCapacityNotTheWorldCapacity() {
        Robot robot = new Robot("sniper", world, 0, 0, RobotTypes.SNIPER);
        world.addRobot(robot);
        robot.setShots(0);

        new ReloadCommand("sniper", new JSONArray()).execute(world);

        assertEquals(1, robot.getShots(), "a SNIPER only ever carries 1 shot");
    }
}
