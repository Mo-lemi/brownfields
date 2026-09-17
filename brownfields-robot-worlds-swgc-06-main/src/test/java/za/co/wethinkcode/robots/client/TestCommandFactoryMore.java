package za.co.wethinkcode.robots.client;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.client.clientcommands.BackCommand;
import za.co.wethinkcode.robots.client.clientcommands.Command;
import za.co.wethinkcode.robots.client.clientcommands.FireCommand;
import za.co.wethinkcode.robots.client.clientcommands.ForwardCommand;
import za.co.wethinkcode.robots.client.clientcommands.LaunchCommand;
import za.co.wethinkcode.robots.client.clientcommands.LookCommand;
import za.co.wethinkcode.robots.client.clientcommands.MineCommand;
import za.co.wethinkcode.robots.client.clientcommands.ReloadCommand;
import za.co.wethinkcode.robots.client.clientcommands.RepairCommand;
import za.co.wethinkcode.robots.client.clientcommands.StateCommand;
import za.co.wethinkcode.robots.client.clientcommands.TurnCommand;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the client-side command factory: every command name maps
 * to its class, and unknown names are rejected with a helpful message.
 */
public class TestCommandFactoryMore {

    @Test
    void everyCommandNameMapsToItsClass() {
        JSONArray noArgs = new JSONArray();
        assertEquals(LaunchCommand.class,
                Command.create("launch", "hal", new JSONArray("[\"sniper\"]")).getClass());
        assertEquals(ForwardCommand.class,
                Command.create("forward", "hal", new JSONArray("[1]")).getClass());
        assertEquals(BackCommand.class,
                Command.create("back", "hal", new JSONArray("[1]")).getClass());
        assertEquals(TurnCommand.class,
                Command.create("turn", "hal", new JSONArray("[\"left\"]")).getClass());
        assertEquals(StateCommand.class,
                Command.create("state", "hal", noArgs).getClass());
        assertEquals(FireCommand.class,
                Command.create("fire", "hal", noArgs).getClass());
        assertEquals(RepairCommand.class,
                Command.create("repair", "hal", noArgs).getClass());
        assertEquals(ReloadCommand.class,
                Command.create("reload", "hal", noArgs).getClass());
        assertEquals(LookCommand.class,
                Command.create("look", "hal", noArgs).getClass());
        assertEquals(MineCommand.class,
                Command.create("mine", "hal", noArgs).getClass());
    }

    @Test
    void unknownCommandsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Command.create("fly", "hal", noArgs()));
    }

    @Test
    void theRejectionMessageNamesTheCommand() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Command.create("fly", "hal", noArgs()));
        assertTrue(exception.getMessage().contains("Unsupported command"), exception.getMessage());
    }

    private JSONArray noArgs() {
        return new JSONArray();
    }
}
