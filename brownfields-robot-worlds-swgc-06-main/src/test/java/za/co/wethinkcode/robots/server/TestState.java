package za.co.wethinkcode.robots.server;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestState extends AbstractAcceptanceTest {

    @Test
    void stateOfNonexistentRobotShouldFail(){
        // Given that I am connected to a running Robot Worlds server
        assertTrue(serverClient.isConnected());

        // Generating unique name for a non-existent robot
        String ghostName = "ghostbot_" + System.currentTimeMillis();

        // When I ask the world for the ghost's state
        String request = "{" + "\"robot\": \"" + ghostName + "\"," + "\"command\": \"state\"," + "\"arguments\": []" + "}";
        JsonNode response = serverClient.sendRequest(request);

        // Then I should get an ERROR response
        assertNotNull(response.get("result"));
        assertEquals("ERROR", response.get("result").asText());

        // And a message explaining the robot does not exist
        assertNotNull(response.get("data"));
        assertNotNull(response.get("data").get("message"));
        assertFalse(response.get("data").get("message").asText().isEmpty());
    }

    @Test
    void validStateRequestShouldReturnStateData(){
        // GIVEN that I am connected to a running Robot Worlds server
        assertTrue(serverClient.isConnected());

        String sharedRobotName = "TestRobot1";

        // AND I have successfully launched the robot into the world first
        // FIXED: Changed "shooter" to a valid type ("sniper") and removed the extra "5", "5" arguments
        String launchRequest = "{" + "\"robot\":\"" + sharedRobotName + "\"," + "\"command\":\"launch\"," + "\"arguments\":[\"sniper\"]" + "}";
        JsonNode launchResponse = serverClient.sendRequest(launchRequest);
        assertEquals("OK", launchResponse.get("result").asText(), "Prerequisite failed: Robot could not be launched");

        // When I send a request with the command "state"
        String request = "{" + "\"robot\":\"" + sharedRobotName + "\"," + "\"command\":\"state\"," + "\"arguments\":[]" + "}";
        JsonNode response = serverClient.sendRequest(request);

        // THEN I should get an OK response
        assertNotNull(response.get("result"));
        assertEquals("OK", response.get("result").asText());

        // AND the response payload should contain the current state data
        JsonNode state = response.get("state");
        assertNotNull(state, "Response should contain a 'state' object");

        // Validate position is a 2-element array [x, y]
        assertNotNull(state.get("position"), "State should contain 'position'");
        assertTrue(state.get("position").isArray(), "Position should be an array");
        assertEquals(2, state.get("position").size(), "Position array should have 2 elements (x,y)");

        // Validate the remaining required state fields exist
        assertNotNull(state.get("direction"), "State should contain 'direction'");
        assertNotNull(state.get("shields"), "State should contain 'shields'");
        assertNotNull(state.get("shots"), "State should contain 'shots'");
        assertNotNull(state.get("status"), "State should contain 'status'");
    }
}