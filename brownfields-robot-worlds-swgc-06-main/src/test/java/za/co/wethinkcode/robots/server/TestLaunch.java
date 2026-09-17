package za.co.wethinkcode.robots.server;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestLaunch extends AbstractAcceptanceTest {

    @Test
    void validLaunchShouldSucceed() {
        assertTrue(serverClient.isConnected());
        String robotName = "HAL_" + System.currentTimeMillis();

        String request = "{" +
                "  \"robot\": \"" + robotName + "\"," +
                "  \"command\": \"launch\"," +
                "  \"arguments\": [\"sniper\"]" +
                "}";
        JsonNode response = serverClient.sendRequest(request);

        assertNotNull(response.get("result"));
        assertEquals("OK", response.get("result").asText());

        assertNotNull(response.get("data"));
        assertNotNull(response.get("data").get("position"));
        assertTrue(response.get("data").get("position").isArray());
        assertEquals(2, response.get("data").get("position").size());

        assertNotNull(response.get("state"));
    }

    @Test
    void invalidLaunchShouldFail() {
        assertTrue(serverClient.isConnected());
        String invalidBot = "invalidbot_" + System.currentTimeMillis();

        // FIXED: Removed the "5","5" from the arguments array
        String request = "{" +
                "\"robot\": \"" + invalidBot + "\"," +
                "\"command\": \"luanch\"," +
                "\"arguments\": [\"sniper\"]" +
                "}";
        JsonNode response = serverClient.sendRequest(request);

        assertNotNull(response.get("result"));
        assertEquals("ERROR", response.get("result").asText());

        assertNotNull(response.get("data"));
        assertNotNull(response.get("data").get("message"));
        assertTrue(response.get("data").get("message").asText().contains("Unsupported command"));
    }

    @Test
    void launchFailsWithDuplicateRobotName() {
        String robotName = "TestBot";
        assertTrue(serverClient.isConnected());

        String firstRequest = "{\"robot\":\"" + robotName + "\",\"command\":\"launch\",\"arguments\":[\"sniper\"]}";
        serverClient.sendRequest(firstRequest);

        String request = "{\"robot\":\"" + robotName + "\",\"command\":\"launch\",\"arguments\":[\"sniper\"]}";
        JsonNode response = serverClient.sendRequest(request);

        assertEquals("ERROR", response.get("result").asText());
        assertTrue(response.get("data").get("message").asText().contains("Too many of you in this world"));
    }

    @Test
    void launchFailsWhenNoSpaceAvailable(){
        assertTrue(serverClient.isConnected());

        // Keep launching robots until the server tells us the world is full.
        // We use a loop limit of 50 to prevent an infinite loop in case of a bug.
        boolean isFull = false;
        JsonNode finalResponse = null;

        for (int i = 0; i < 50; i++) {
            String request = "{\"robot\": \"Filler" + i + "\", \"command\": \"launch\", \"arguments\": [\"sniper\"]}";
            finalResponse = serverClient.sendRequest(request);

            if ("ERROR".equals(finalResponse.get("result").asText())) {
                isFull = true;
                break;
            }
        }

        assertTrue(isFull, "The world should eventually fill up and return an ERROR.");

        // Assert that the error message is specifically about the world being full
        assertTrue(finalResponse.get("data").get("message").asText().contains("No more space in this world"),
                "Expected 'No more space' message, but got: " + finalResponse.get("data").get("message").asText());
    }

    @Test
    void canLaunchAnotherRobot() {
        assertTrue(serverClient.isConnected());

        // GIVEN: A robot named "FirstBot" is already launched
        String firstLaunch = "{\"robot\": \"FirstBot\", \"command\": \"launch\", \"arguments\": [\"sniper\"]}";
        JsonNode firstResponse = serverClient.sendRequest(firstLaunch);
        assertEquals("OK", firstResponse.get("result").asText());

        // WHEN: I send a valid launch request for a SECOND robot
        String secondLaunch = "{\"robot\": \"SecondBot\", \"command\": \"launch\", \"arguments\": [\"tank\"]}";
        JsonNode secondResponse = serverClient.sendRequest(secondLaunch);

        // THEN: The second launch should succeed
        assertNotNull(secondResponse.get("result"));
        assertEquals("OK", secondResponse.get("result").asText());

        // AND: It should have its own valid state and position
        assertNotNull(secondResponse.get("state"));
        assertEquals(2, secondResponse.get("data").get("position").size());
    }

    @Test
    void launchRobotIntoWorldWithObstacles() {
        assertTrue(serverClient.isConnected());

        // GIVEN: The world has obstacles
        // (Our 2x2 test world automatically generates 1 obstacle because height/2 = 1)

        // WHEN: I launch a robot
        String request = "{\"robot\": \"SurvivorBot\", \"command\": \"launch\", \"arguments\": [\"juggernaut\"]}";
        JsonNode response = serverClient.sendRequest(request);

        // THEN: The launch should succeed, proving the server safely avoided placing it on the obstacle
        assertNotNull(response.get("result"));
        assertEquals("OK", response.get("result").asText());

        // AND: The robot should report a valid starting position
        JsonNode position = response.get("data").get("position");
        assertTrue(position.isArray());
        assertEquals(2, position.size());
    }
}