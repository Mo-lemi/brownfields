package za.co.wethinkcode.robots.client;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests proving the client's input handling is case-insensitive.
 */
public class TestValidateUppercase {

    @Test
    void uppercaseLaunchIsAcceptedAndLowercased() {
        ValidateCommand validator = new ValidateCommand("");
        JSONObject json = new JSONObject(validator.handleCommand("LAUNCH SNIPER HAL"));

        assertEquals("launch", json.getString("command"));
        assertEquals("sniper", json.getJSONArray("arguments").getString(0));
    }

    @Test
    void uppercaseMovementIsAcceptedAfterLaunch() {
        ValidateCommand validator = new ValidateCommand("");
        validator.handleCommand("LAUNCH SNIPER HAL");

        JSONObject json = new JSONObject(validator.handleCommand("FORWARD 2"));

        assertEquals("forward", json.getString("command"));
        assertEquals("2", json.getJSONArray("arguments").getString(0));
    }
}
