package za.co.wethinkcode.robots.server;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestLook extends AbstractAcceptanceTest {

    private static final String SHARED_ROBOT_NAME = "TestRobot";

    @BeforeEach
    void launchRobot() {

        String launchRequest = "{"
                + "\"robot\":\"" + SHARED_ROBOT_NAME + "\","
                + "\"command\":\"launch\","
                + "\"arguments\":[\"sniper\"]"
                + "}";

        JsonNode response = serverClient.sendRequest(launchRequest);

        assertNotNull(response);
        assertEquals("OK", response.get("result").asText());
    }

    @Test
    void lookReturnsOkDataObjectsAndState() {

        // Given that I am connected to a running Robot Worlds server
        assertTrue(serverClient.isConnected());

        // When I send a valid look request to the server
        String request = "{"
                + "\"robot\":\"" + SHARED_ROBOT_NAME + "\","
                + "\"command\":\"look\","
                + "\"arguments\":[]"
                + "}";

        JsonNode response = serverClient.sendRequest(request);

        // Then I should get a valid response from the server
        assertNotNull(response);
        assertEquals("OK", response.get("result").asText());

        // And the response should contain data
        JsonNode data = response.get("data");
        assertNotNull(data);

        // And the data should contain an objects array
        JsonNode objects = data.get("objects");
        assertNotNull(objects);
        assertTrue(objects.isArray());

        // And each object should contain direction, type and distance
        for (JsonNode object : objects) {
            assertNotNull(object.get("direction"));
            assertNotNull(object.get("type"));
            assertNotNull(object.get("distance"));
        }

        // And the response should contain the robot state
        assertNotNull(response.get("state"));
    }

    @Test
    void lookShouldDetectWorldEdge() {

        // Given that I am connected to a running Robot Worlds server
        assertTrue(serverClient.isConnected());

        // When the robot sends a valid look request to the server
        String request = "{"
                + "\"robot\":\"" + SHARED_ROBOT_NAME + "\","
                + "\"command\":\"look\","
                + "\"arguments\":[]"
                + "}";

        JsonNode response = serverClient.sendRequest(request);

        // Then I should get a valid response from the server
        assertNotNull(response);
        assertEquals("OK", response.get("result").asText());

        // And the objects array should contain the EDGE of the world
        JsonNode objects = response.get("data").get("objects");

        assertNotNull(objects);
        assertTrue(objects.isArray());

        // Check that at least one EDGE was detected
        boolean foundEdge = false;

        for (JsonNode object : objects) {
            if ("EDGE".equals(object.get("type").asText())) {
                foundEdge = true;
                break;
            }
        }

        assertTrue(
                foundEdge,
                "Should see at least one EDGE in the objects array. Actual: " + objects
        );
    }

    @Test
    void seeRobotsAndObstacles() {
        // Given a world of size 2x2 with an obstacle at [0,1]
        // The @BeforeEach already launched "TestRobot" at (0,0).
        // I need to launch 7 MORE robots to fill the remaining 8 spaces in the world.

        for (int i = 1; i <= 7; i++) {
            String launchRequest = "{"
                    + "\"robot\":\"FillerBot" + i + "\","
                    + "\"command\":\"launch\","
                    + "\"arguments\":[\"sniper\"]"
                    + "}";
            JsonNode fillerResponse = serverClient.sendRequest(launchRequest);

            // If this fails, it means the world isn't configured correctly,
            // or the server rejected the launch.
            assertEquals("OK", fillerResponse.get("result").asText(),
                    "Failed to launch filler robot " + i + ". Server said: " + fillerResponse);
        }

        // When I ask the first robot to look
        String request = "{"
                + "\"robot\":\"" + SHARED_ROBOT_NAME + "\","
                + "\"command\":\"look\","
                + "\"arguments\":[]"
                + "}";
        JsonNode response = serverClient.sendRequest(request);

        // Then I should get a valid response back
        assertEquals("OK", response.get("result").asText(), "Look failed: " + response);
        JsonNode objects = response.get("data").get("objects");
        assertNotNull(objects);

        // Count objects that are exactly 1 step away
        int obstacleCount = 0;
        int robotCount = 0;

        for (JsonNode object : objects) {
            if (object.get("distance").asInt() == 1) {
                String type = object.get("type").asText();
                if ("OBSTACLE".equals(type)) {
                    obstacleCount++;
                } else if ("ROBOT".equals(type)) {
                    robotCount++;
                }
            }
        }

        // Then here I assert the exact scenario requirements
        assertEquals(1, obstacleCount,
                "Should see exactly 1 OBSTACLE at distance 1. Actual objects: " + objects);
        assertEquals(3, robotCount,
                "Should see exactly 3 ROBOTs at distance 1. Actual objects: " + objects);
    }

    @Test
    void lookShouldSeeObstacleAtDistanceOne() {

        // Given a world of size 2x2 with an obstacle at [0,1]
        // And TestRobot has already been launched by @BeforeEach
        assertTrue(serverClient.isConnected());

        // When I ask the robot to look
        String request = "{"
                + "\"robot\":\"" + SHARED_ROBOT_NAME + "\","
                + "\"command\":\"look\","
                + "\"arguments\":[]"
                + "}";

        JsonNode response = serverClient.sendRequest(request);

        // Then I should get a valid response
        assertNotNull(response);
        assertEquals("OK", response.get("result").asText());

        // And the response should contain objects
        JsonNode objects = response.get("data").get("objects");
        assertNotNull(objects);
        assertTrue(objects.isArray());

        // And there should be an OBSTACLE at distance 1
        boolean foundObstacle = false;

        for (JsonNode object : objects) {
            if ("OBSTACLE".equals(object.get("type").asText())
                    && object.get("distance").asInt() == 1) {

                foundObstacle = true;
                break;
            }
        }

        assertTrue(
                foundObstacle,
                "Should see an OBSTACLE at distance 1. Actual objects: " + objects
        );
    }
}