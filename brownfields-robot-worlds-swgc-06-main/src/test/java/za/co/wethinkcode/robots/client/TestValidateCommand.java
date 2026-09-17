package za.co.wethinkcode.robots.client;

import org.json.JSONArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.server.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestValidateCommand {

    private final ValidateCommand validateCommand = new ValidateCommand("bob");

    @Test
    void ValidLaunchCommand(){
        String command = "launch sniper bob";

        String expected = "{\"robot\":\"bob\",\"arguments\":[\"sniper\",\"bob\"],\"command\":\"launch\"}";
        String result = validateCommand.handleCommand(command);
        Assertions.assertEquals(expected, result);
    }

    @Test
    void ValidForwardCommand(){
        runLaunchValidateCommand();
        String command = "forward 2";

        String expected = "{\"robot\":\"bob\",\"arguments\":[\"2\"],\"command\":\"forward\"}";
        String result = validateCommand.handleCommand(command);
        Assertions.assertEquals(expected, result);
    }

    @Test
    void ValidBackCommand(){
        runLaunchValidateCommand();
        String command = "back 5";

        String expected = "{\"robot\":\"bob\",\"arguments\":[\"5\"],\"command\":\"back\"}";
        String result = validateCommand.handleCommand(command);
        Assertions.assertEquals(expected, result);
    }

    @Test
    void ValidTurnCommand(){
        runLaunchValidateCommand();
        String command = "turn right";

        String expected = "{\"robot\":\"bob\",\"arguments\":[\"right\"],\"command\":\"turn\"}";
        String result = validateCommand.handleCommand(command);
        Assertions.assertEquals(expected, result);

        command = "turn left";

        expected = "{\"robot\":\"bob\",\"arguments\":[\"left\"],\"command\":\"turn\"}";
        result = validateCommand.handleCommand(command);
        Assertions.assertEquals(expected, result);
    }

    @Test
    void ValidSingleWordCommands(){
        runLaunchValidateCommand();

        String command = "look";
        String expected = "{\"robot\":\"bob\",\"arguments\":[],\"command\":\"look\"}";
        String result = validateCommand.handleCommand(command);
        Assertions.assertEquals(expected, result);

        command = "state";
        expected = "{\"robot\":\"bob\",\"arguments\":[],\"command\":\"state\"}";
        result = validateCommand.handleCommand(command);
        Assertions.assertEquals(expected, result);

        command = "fire";
        expected = "{\"robot\":\"bob\",\"arguments\":[],\"command\":\"fire\"}";
        result = validateCommand.handleCommand(command);
        Assertions.assertEquals(expected, result);

        command = "reload";
        expected = "{\"robot\":\"bob\",\"arguments\":[],\"command\":\"reload\"}";
        result = validateCommand.handleCommand(command);
        Assertions.assertEquals(expected, result);

        command = "repair";
        expected = "{\"robot\":\"bob\",\"arguments\":[],\"command\":\"repair\"}";
        result = validateCommand.handleCommand(command);
        Assertions.assertEquals(expected, result);
    }

    @Test
    void InvalidCommands(){
        String command = "forward 3";
        String expected = "";
        String result = validateCommand.handleCommand(command);
        Assertions.assertEquals(expected, result);

        runLaunchValidateCommand();

        List<String> commands = new ArrayList<>(Arrays.asList("forward", "for 2", "move forward 2", "back, back t",
                "bac 4", "right", "turn left 3", "tur right", "right turn","look 3", "fire 2", "loo", "stated", "reloa",
                "repairs"));

        for (String com : commands){
            result = validateCommand.handleCommand(com);
            Assertions.assertEquals(expected,result);
        }

    }

    private void runLaunchValidateCommand(){
        validateCommand.handleCommand("launch sniper bob");
    }
}
