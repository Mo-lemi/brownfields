package za.co.wethinkcode.robots.server.servercommands;

import za.co.wethinkcode.robots.server.Robot;
import za.co.wethinkcode.robots.server.World;

import java.util.List;

/**
 * Command that dumps the current state of the world.
 * This includes a visual representation of the map grid with robots and obstacles,
 * as well as any other relevant world information.
 */
public class DumpCommand extends ServerCommand {

    /**
     * Executes the dump command, which outputs the current state of the world.
     * Displays a grid representing robots (@), obstacles (#), and empty spaces (.),
     * followed by additional world state details.
     *
     * @return true after successfully dumping the state.
     */
    @Override
    public boolean execute() {
        World world = getWorld();

        // Loop from top edge (maxY) down to bottom edge (minY)
        for (int y = world.getMaxY(); y >= world.getMinY(); y--) {
            // Loop from left edge (minX) to right edge (maxX)
            for (int x = world.getMinX(); x <= world.getMaxX(); x++) {
                switch (world.checkType(x, y)) {
                    case "r":
                        System.out.print(" @ ");
                        break;
                    case "o":
                        System.out.print(" # ");
                        break;
                    case "p":
                        System.out.print(" * "); // Symbol for pits
                        break;
                    default:
                        System.out.print(" . ");
                }
            }
            System.out.println();
        }

        System.out.println("key:\n- @ = robot\n- # = obstacle\n- * = pit\n- . = empty block\n");
        world.WorldState();
        return true;
    }

    /**
     * Constructor for creating a DumpCommand with a specified world.
     *
     * @param world The world whose state will be dumped.
     */
    public DumpCommand(World world) {
        super("dump", world);
    }
}