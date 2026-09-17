package za.co.wethinkcode.robots.server.servercommands;

import za.co.wethinkcode.robots.server.World;

/**
 * Base class for all server commands.
 * Defines common behavior and provides a method to create server commands.
 */
public abstract class ServerCommand {
    private String name;
    private World world;

    /**
     * Executes the server command.
     *
     * @return true if the command executed successfully, false otherwise.
     */
    public abstract boolean execute();

    /**
     * Constructor for creating a ServerCommand with a specified name and world.
     *
     * @param name  The name of the server command.
     * @param world The world in which the command will be executed.
     */
    public ServerCommand(String name, World world) {
        this.name = name.trim().toLowerCase();
        this.world = world;
    }

    /**
     * Gets the name of the command.
     *
     * @return the name of the command.
     */
    public String getName() {return name;}

    /**
     * Gets the world in which the command will be executed.
     *
     * @return the current world instance.
     */
    public World getWorld() {return world;}

    /**
     * Factory method to create a specific ServerCommand based on the instruction.
     *
     * @param instruction The instruction e.g., "quit", "robots", "dump", "purge".
     * @param world       The world context to execute the command in.
     * @return A corresponding ServerCommand instance.
     */
    public static ServerCommand create(String instruction, World world) {
        String[] instructions = instruction.trim().toLowerCase().split("\\s+");

        instruction = instructions[0];
        String robotName = instructions.length > 1 ? instructions[1] : "";


        return switch (instruction) {
            case "quit" -> new QuitCommand(world);
            case "robots" -> new RobotsCommand(world);
            case "dump" -> new DumpCommand(world);
            case "purge" -> new PurgeCommand(world, robotName);
            case "save" -> new SaveCommand(world, robotName);
            case "restore" -> new RestoreCommand(world, robotName);
            default -> new ErrorCommand(world);
        };

    }
}
