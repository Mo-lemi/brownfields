package za.co.wethinkcode.robots.client;

import org.json.JSONArray;
import org.json.JSONObject;


import java.util.Set;

/**
 * Validates and formats client commands into JSON.
 */
public class ValidateCommand {

    private static final Set<String> EMPTY_COMMANDS = Set.of(
            "fire", "reload", "repair", "look", "state", "mine"
    );

    private static final Set<String> ROBOT_TYPES = Set.of(
            "juggernaut", "tank", "soldier", "marksman", "sniper", "intruder"
    );

    private String robotName;
    private boolean launched;
    private final JSONObject json = new JSONObject();

    public ValidateCommand(String robotName) {
        this.robotName = robotName;
    }

    public String handleCommand(String input) {
        Command command = new Command(input);

        switch (command.getName()) {
            case "off":
                return "off";

            case "help":
                showCommands();
                return "";

            case "types":
                showTypes();
                return "";
        }

        if (!validCommand(command)) {
            System.out.println("ERROR: " + input
                    + " is an invalid command.\nType help for all the commands.");
            return "";
        }

        if (!launched || robotName.isEmpty()) {
            System.out.println("ERROR: use launch command first e.g. launch <type> <name>");
            return "";
        }

        return createJson(command);
    }

    public boolean validCommand(Command command) {
        switch (command.getName()) {
            case "launch":
                return validateLaunch(command);

            case "forward":
            case "back":
                return validateMove(command);

            case "turn":
                return validateTurn(command);

            default:
                return EMPTY_COMMANDS.contains(command.getName())
                        && command.getArguments().isEmpty();
        }
    }

    private boolean validateLaunch(Command command) {
        JSONArray args = command.getArguments();

        if (args.length() != 2) {
            return false;
        }

        if (!ROBOT_TYPES.contains(args.getString(0))) {
            System.out.println("ERROR: Invalid robot type.");
            return false;
        }

        robotName = args.getString(1);
        launched = true;
        return true;
    }

    private boolean validateMove(Command command) {
        JSONArray args = command.getArguments();
        return args.length() == 1 && isInteger(args.getString(0));
    }

    private boolean validateTurn(Command command) {
        JSONArray args = command.getArguments();

        if (args.length() != 1) {
            return false;
        }

        String direction = args.getString(0);
        return direction.equals("left") || direction.equals("right");
    }

    public String createJson(Command command) {
        json.put("robot", robotName);
        json.put("command", command.getName());
        json.put("arguments", command.getArguments());
        return json.toString();
    }

    public boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void showCommands() {
        System.out.println("""
                Commands:
                <> - variable needed.
                -----------------------------------------
                - forward <steps>
                - back <steps>
                - turn <left/right>
                - look
                - repair
                - fire
                - reload
                - mine
                - state
                - off
                -----------------------------------------
                """);
    }

    public void showTypes() {
        System.out.println("""
                Types:
                ---------------
                - Juggernaut
                - Tank
                - Soldier
                - Marksman
                - Sniper
                - Intruder (places mines, no gun)
                ---------------
                """);
    }
    public boolean isEmptyRobotName(){return robotName.isEmpty();}
    public void setRobotName(String s) {
        this.robotName = s;
    }

    /**
     * Represents a parsed user command.
     */
    private static class Command {

        private final String name;
        private final JSONArray arguments = new JSONArray();

        public Command(String input) {
            String[] tokens = input.toLowerCase().trim().split(" ");

            name = tokens[0];

            for (int i = 1; i < tokens.length; i++) {
                arguments.put(tokens[i]);
            }
        }

        public String getName() {
            return name;
        }

        public JSONArray getArguments() {
            return arguments;
        }
    }
}