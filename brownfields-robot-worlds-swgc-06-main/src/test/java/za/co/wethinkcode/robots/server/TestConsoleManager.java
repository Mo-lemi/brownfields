package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConsoleManager: it reads admin commands from stdin and
 * stops quietly when the stream ends (the detached-Docker case).
 * Note: the "quit" input is deliberately NOT tested - it calls
 * System.exit and would terminate the test JVM.
 */
public class TestConsoleManager {

    private World world;
    private ByteArrayOutputStream captured;
    private PrintStream originalOut;
    private java.io.InputStream originalIn;

    @BeforeEach
    void setUp() {
        world = new World(20, 20);
        captured = new ByteArrayOutputStream();
        originalOut = System.out;
        originalIn = System.in;
        System.setOut(new PrintStream(captured));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    private void feed(String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }


    @Test
    void runWithEmptyStdinReturnsImmediately() {
        feed("");
        assertDoesNotThrow(() -> new ConsoleManager(world).run());
    }

    @Test
    void runWithAnUnknownCommandStillSurvives() {
        feed("fly me to the moon\nrobots\n");
        assertDoesNotThrow(() -> new ConsoleManager(world).run());
        assertTrue(captured.toString().contains("Invalid command"), output());
    }

    @Test
    void runDoesNotChangeTheWorld() {
        feed("robots\ndump\n");
        new ConsoleManager(world).run();
        assertEquals(0, world.getRobots().size(), "console commands do not add robots");
    }

    private String output() {
        return captured.toString();
    }
}
