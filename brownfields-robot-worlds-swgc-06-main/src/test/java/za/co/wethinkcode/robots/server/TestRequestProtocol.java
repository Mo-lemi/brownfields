package za.co.wethinkcode.robots.server;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.server.protocol.Request;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the request handler's protocol edge cases: unknown
 * commands, malformed JSON, unknown robots, and the happy paths.
 */
public class TestRequestProtocol {

    private World world;

    @BeforeEach
    void setUp() {
        world = new World(20, 20);
    }

    private String send(String json) {
        return new Request().handleRequest(json, world);
    }

    @Test
    void unknownCommandReturnsAnError() {
        JSONObject json = new JSONObject(send(
                "{\"robot\": \"hal\", \"command\": \"fly\", \"arguments\": []}"));

        assertEquals("ERROR", json.getString("result"));
        assertTrue(json.getJSONObject("data").getString("message")
                .startsWith("Unsupported command"));
    }

    @Test
    void malformedJsonReturnsAnErrorInsteadOfCrashing() {
        JSONObject json = new JSONObject(send("this is not json at all"));

        assertEquals("ERROR", json.getString("result"));
        assertEquals("Could not parse arguments",
                json.getJSONObject("data").getString("message"));
    }

    @Test
    void commandForAMissingRobotReturnsAnError() {
        JSONObject json = new JSONObject(send(
                "{\"robot\": \"ghost\", \"command\": \"state\", \"arguments\": []}"));

        assertEquals("ERROR", json.getString("result"));
        assertEquals("Robot does not exist in the world",
                json.getJSONObject("data").getString("message"));
    }

    @Test
    void launchReturnsOkWithPositionAndState() {
        JSONObject json = new JSONObject(send(
                "{\"robot\": \"hal\", \"command\": \"launch\", \"arguments\": [\"sniper\"]}"));

        assertEquals("OK", json.getString("result"));
        assertEquals(2, json.getJSONObject("data").getJSONArray("position").length());
        assertEquals("NORTH", json.getJSONObject("state").getString("direction"));
    }

    @Test
    void stateReturnsOkWithTheRobotState() {
        send("{\"robot\": \"hal\", \"command\": \"launch\", \"arguments\": [\"soldier\"]}");

        JSONObject json = new JSONObject(send(
                "{\"robot\": \"hal\", \"command\": \"state\", \"arguments\": []}"));

        assertEquals("OK", json.getString("result"));
        assertEquals("NORMAL", json.getJSONObject("state").getString("status"));
        assertEquals(3, json.getJSONObject("state").getInt("shields"));
        assertEquals(2, json.getJSONObject("state").getJSONArray("position").length());
    }
}
