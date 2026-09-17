package za.co.wethinkcode.robots.client.clientcommands;

import org.json.JSONArray;
import za.co.wethinkcode.robots.server.World;

/**
 * Abstract base class for all robot commands.
 * Provides common properties and functionality shared by all commands,
 * including the robot name, command name, and command arguments.
 */
public abstract class Command {

    private String robotName;
    private String commandName;
    private JSONArray arguments;

    /**
     * Executes the command in the given world .
     * Subclasses must implement this to define specific command behavior.
     *
     * @param world The World instance in which the command is executed.
     * @return A string result indicating success, failure, or other command output.
     */
    public abstract String execute(World world);

    /**
     * Constructs a command with only a robot name.
     *
     * @param name The name of the robot this command is associated with.
     */
    public Command(String name){
        robotName = name;
        commandName = "";
    }

    /**
     * Constructs a command with a command name, robot name, and arguments.
     *
     * @param commandName The name/type of the command (e.g., "move", "fire").
     * @param name The name of the robot this command applies to.
     * @param args A JSONArray containing arguments relevant to the command.
     */
    public Command(String commandName, String name, JSONArray args) {
        this(name);
        this.commandName = commandName;
        this.arguments = args;
    }

    /**
     * Gets the name of the robot this command operates on.
     *
     * @return The robot's name.
     */
    public String getRobotName(){return robotName;}

    /**
     * Gets the name/type of this command.
     *
     * @return The command name.
     */
    public String getCommandName(){return commandName;}

    /**
     * Gets the arguments passed to this command.
     *
     * @return A JSONArray of arguments.
     */
    public JSONArray getArguments(){return arguments;}

    /**
     * Method to create a Command subclass instance based on command type.
     *
     * @param type The command type string (e.g., "launch", "forward").
     * @param robotName The name of the robot this command is created for.
     * @param args A JSONArray of arguments relevant to the command.
     * @return An instance of a subclass of Command corresponding to the type.
     * @throws IllegalArgumentException if the command type is unsupported.
     */
    public static Command create(String type, String robotName, JSONArray args) {
        switch (type){
            case "launch":
                return new LaunchCommand(robotName, args);
            case "forward":
                return new ForwardCommand(robotName, args);
            case "back":
                return new BackCommand(robotName, args);
            case "turn":
                return new TurnCommand(robotName, args);
            case "state":
                return new StateCommand(robotName, args);
            case "fire":
                return new FireCommand(robotName, args);
            case "repair":
                return new RepairCommand(robotName, args);
            case "reload":
                return new ReloadCommand(robotName, args);
            case "look":
                return new LookCommand(robotName, args);
            case "mine":
                return new MineCommand(robotName, args);


            default:
                throw new IllegalArgumentException("Unsupported command " + type +  " " + args.join(" "));
        }
    }
    public String getObjectType(String worldType) {
        return switch (worldType) {
            case "r" -> "ROBOT";
            case "o" -> "OBSTACLE";
            case "p" -> "PIT";
            case "e" -> "EDGE";
            default -> "";
        };
    }
}

