package za.co.wethinkcode.robots.server.servercommands;

import za.co.wethinkcode.robots.server.World;

/**
 * Command that handles quitting the server.
 */
public class QuitCommand extends ServerCommand {

    /**
     * Executes the quit command, which shuts down the server by printing a shutdown message.
     *
     * @return true after the command is executed.
     */
    @Override
    public boolean execute() {
        System.out.println("Closing the Server...");
        return true;
    }

    /**
     * Constructor for creating a QuitCommand with a specified world.
     *
     * @param world The world associated with this command.
     */
    public QuitCommand(World world) {
        super("quit", world);
    }
}