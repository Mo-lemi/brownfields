package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.server.servercommands.ErrorCommand;
import za.co.wethinkcode.robots.server.servercommands.PurgeCommand;
import za.co.wethinkcode.robots.server.servercommands.QuitCommand;
import za.co.wethinkcode.robots.server.servercommands.RestoreCommand;
import za.co.wethinkcode.robots.server.servercommands.RobotsCommand;
import za.co.wethinkcode.robots.server.servercommands.SaveCommand;
import za.co.wethinkcode.robots.server.servercommands.ServerCommand;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the server console command factory: every console
 * instruction maps to the right command, names are normalised, and
 * unknown input produces the error command.
 */
public class TestServerCommandFactory {

    private World world;

    @BeforeEach
    void setUp() {
        world = new World(20, 20);
    }

    @Test
    void quitMapsToQuitCommand() {
        ServerCommand command = ServerCommand.create("quit", world);
        assertTrue(command instanceof QuitCommand);
        assertEquals("quit", command.getName());
    }

    @Test
    void robotsMapsToRobotsCommand() {
        ServerCommand command = ServerCommand.create("robots", world);
        assertTrue(command instanceof RobotsCommand);
        assertEquals("robots", command.getName());
    }

    @Test
    void dumpMapsToDumpCommand() {
        ServerCommand command = ServerCommand.create("dump", world);
        assertTrue(command instanceof za.co.wethinkcode.robots.server.servercommands.DumpCommand);
        assertEquals("dump", command.getName());
    }

    @Test
    void purgeMapsToPurgeCommandWithTheRobotName() {
        ServerCommand command = ServerCommand.create("purge hal", world);
        assertTrue(command instanceof PurgeCommand);
        assertEquals("purge", command.getName());
    }

    @Test
    void saveMapsToSaveCommandWithTheWorldName() {
        ServerCommand command = ServerCommand.create("save alpha", world);
        assertTrue(command instanceof SaveCommand);
        assertEquals("save", command.getName());
    }

    @Test
    void restoreMapsToRestoreCommandWithTheWorldName() {
        ServerCommand command = ServerCommand.create("restore alpha", world);
        assertTrue(command instanceof RestoreCommand);
        assertEquals("restore", command.getName());
    }

    @Test
    void unknownInstructionMapsToErrorCommand() {
        ServerCommand command = ServerCommand.create("fly me to the moon", world);
        assertTrue(command instanceof ErrorCommand);
        assertEquals("error", command.getName());
    }

    @Test
    void instructionIsNormalisedToLowerCase() {
        ServerCommand command = ServerCommand.create("QUIT", world);
        assertTrue(command instanceof QuitCommand);
        assertEquals("quit", command.getName());
    }

    @Test
    void extraWhitespaceIsTrimmed() {
        ServerCommand command = ServerCommand.create("   robots   ", world);
        assertTrue(command instanceof RobotsCommand);
    }

    @Test
    void errorCommandPrintsAndSucceeds() {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
        try {
            ServerCommand command = ServerCommand.create("nonsense", world);
            assertTrue(command.execute());
            assertTrue(captured.toString().contains("Invalid command"));
        } finally {
            System.setOut(original);
        }
    }
}
