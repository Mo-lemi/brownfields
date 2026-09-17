package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.server.servercommands.DumpCommand;
import za.co.wethinkcode.robots.server.servercommands.ErrorCommand;
import za.co.wethinkcode.robots.server.servercommands.QuitCommand;
import za.co.wethinkcode.robots.server.servercommands.RobotsCommand;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the server console commands' execute() behaviour, using
 * stdout capture so the printed output can be verified.
 */
public class TestServerConsoleCommands {

    private World world;
    private ByteArrayOutputStream captured;
    private PrintStream original;

    @BeforeEach
    void setUp() {
        world = new World(20, 20);
        captured = new ByteArrayOutputStream();
        original = System.out;
        System.setOut(new PrintStream(captured));
    }

    @AfterEach
    void tearDown() {
        System.setOut(original);
    }

    private String output() {
        return captured.toString();
    }

    @Test
    void quitCommandPrintsShutdownMessage() {
        assertTrue(new QuitCommand(world).execute());
        assertTrue(output().contains("Closing the Server"), output());
    }

    @Test
    void errorCommandPrintsInvalidCommandMessage() {
        assertTrue(new ErrorCommand(world).execute());
        assertTrue(output().contains("Invalid command"), output());
    }

    @Test
    void robotsCommandReportsAnEmptyWorld() {
        assertTrue(new RobotsCommand(world).execute());
        assertTrue(output().contains("no Robots"), output());
    }

    @Test
    void robotsCommandListsEveryRobotWithItsStatus() {
        Robot hal = new Robot("hal", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(hal);
        hal.setStatus("NORMAL");

        assertTrue(new RobotsCommand(world).execute());
        assertTrue(output().contains("hal"), output());
        assertTrue(output().contains("NORMAL"), output());
    }

    @Test
    void dumpCommandExecutesOnASmallWorld() {
        World small = new World(5, 5);
        small.addObstacle(new za.co.wethinkcode.robots.server.obstacles.MountainObstacle(2, 2));
        Robot robot = new Robot("hal", small, 0, 0, RobotTypes.SOLDIER);
        small.addRobot(robot);

        assertTrue(new DumpCommand(small).execute());
        assertTrue(output().contains("@"), "the robot renders as @ on the map: " + output());
        assertTrue(output().contains("#"), "obstacles render as # on the map: " + output());
    }

    @Test
    void dumpCommandShowsPits() {
        World small = new World(5, 5);
        small.addPit(new za.co.wethinkcode.robots.server.obstacles.PitObstacle(1, 1));

        assertTrue(new DumpCommand(small).execute());
        assertTrue(output().contains("pit") || output().contains("P"), output());
    }
}
