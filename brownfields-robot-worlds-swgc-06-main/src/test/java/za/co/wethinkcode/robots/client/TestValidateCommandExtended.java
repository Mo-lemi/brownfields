package za.co.wethinkcode.robots.client;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended unit tests for the client's command validation: the launch
 * contract, per-command argument rules, and the JSON that gets sent.
 */
public class TestValidateCommandExtended {

    @Test
    void offReturnsTheOffSignal() {
        ValidateCommand validator = new ValidateCommand("");
        assertEquals("off", validator.handleCommand("off"));
    }

    @Test
    void helpAndTypesPrintWithoutSendingAnything() {
        ValidateCommand validator = new ValidateCommand("");
        assertEquals("", validator.handleCommand("help"));
        assertEquals("", validator.handleCommand("types"));
    }

    @Test
    void commandsBeforeLaunchAreRejected() {
        ValidateCommand validator = new ValidateCommand("");
        assertEquals("", validator.handleCommand("state"));
        assertEquals("", validator.handleCommand("forward 2"));
    }

    @Test
    void launchNeedsExactlyTwoArguments() {
        ValidateCommand validator = new ValidateCommand("");
        assertEquals("", validator.handleCommand("launch sniper"));
        assertEquals("", validator.handleCommand("launch sniper HAL extra"));
    }

    @Test
    void launchRejectsUnknownTypes() {
        ValidateCommand validator = new ValidateCommand("");
        assertEquals("", validator.handleCommand("launch dragon HAL"));
    }

    @Test
    void validLaunchBuildsTheLaunchJson() {
        ValidateCommand validator = new ValidateCommand("");
        JSONObject json = new JSONObject(validator.handleCommand("launch sniper HAL"));

        assertEquals("hal", json.getString("robot"), "input is lowercased");
        assertEquals("launch", json.getString("command"));
        assertEquals("sniper", json.getJSONArray("arguments").getString(0));
        assertEquals("hal", json.getJSONArray("arguments").getString(1));
    }

    @Test
    void afterLaunchCommandsUseTheRobotName() {
        ValidateCommand validator = new ValidateCommand("");
        validator.handleCommand("launch sniper HAL");

        JSONObject json = new JSONObject(validator.handleCommand("state"));
        assertEquals("hal", json.getString("robot"));
        assertEquals("state", json.getString("command"));
    }

    @Test
    void mineIsValidAfterLaunch() {
        ValidateCommand validator = new ValidateCommand("");
        validator.handleCommand("launch intruder digger");

        JSONObject json = new JSONObject(validator.handleCommand("mine"));
        assertEquals("mine", json.getString("command"));
        assertEquals(0, json.getJSONArray("arguments").length());
    }

    @Test
    void moveCommandsNeedAnInteger() {
        ValidateCommand validator = new ValidateCommand("");
        validator.handleCommand("launch sniper HAL");
        assertEquals("", validator.handleCommand("forward abc"));
        assertEquals("", validator.handleCommand("back 1.5"));
    }

    @Test
    void turnNeedsLeftOrRight() {
        ValidateCommand validator = new ValidateCommand("");
        validator.handleCommand("launch sniper HAL");
        assertEquals("", validator.handleCommand("turn around"));
        assertEquals("", validator.handleCommand("turn left now"));
    }

    @Test
    void noArgumentCommandsRejectExtraWords() {
        ValidateCommand validator = new ValidateCommand("");
        validator.handleCommand("launch sniper HAL");
        assertEquals("", validator.handleCommand("look now"));
        assertEquals("", validator.handleCommand("state please"));
    }
}
