package za.co.wethinkcode.robots.server.servercommands;

import za.co.wethinkcode.robots.server.World;

/**
 * A server command used to indicate an invalid or unrecognized command input.
 */
public class ErrorCommand extends ServerCommand{

    /**
     * Executes the error command, which prints an error message indicating
     * that the input command is invalid.
     *
     * @return true after displaying the error message.
     */
    @Override
    public boolean execute() {
        System.out.println("ERROR : Invalid command!");
        return true;
    }

    /**
     * Constructs an ErrorCommand with the given world context.
     *
     * @param world The world in which the error occurred.
     */
    public ErrorCommand(World world){super("error", world);}
}
