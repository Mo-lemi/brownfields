package za.co.wethinkcode.robots.server.servercommands;

import za.co.wethinkcode.robots.server.World;
import za.co.wethinkcode.robots.server.database.PersistenceException;
import za.co.wethinkcode.robots.server.database.SqliteWorldRepository;
import za.co.wethinkcode.robots.server.database.WorldRepository;


public class RestoreCommand extends ServerCommand {
    private final String worldName;
    private final WorldRepository repository;

    public RestoreCommand(World world, String worldName) {
        this(world, worldName, new SqliteWorldRepository());
    }

    public RestoreCommand(World world, String worldName,
                          WorldRepository repository) {
        super("restore", world);
        this.worldName = worldName;
        this.repository = repository;
    }

    @Override
    public boolean execute() {
        if (worldName == null || worldName.trim().isEmpty()) {
            System.out.println("Please provide a world name. e.g., restore <world_name>");
            return false;
        }

        try {
            World restoredWorld = repository.loadWorld(worldName);
            if (restoredWorld == null) {
                System.out.println("World '" + worldName + "' does not exist in the database.");
                return false;
            }

            getWorld().restoreState(restoredWorld);

            System.out.println("World '" + worldName + "' restored successfully.");
            System.out.println("Dimensions: " + restoredWorld.getWidth()
                    + "x" + restoredWorld.getHeight());
            System.out.println("Obstacles restored: "
                    + restoredWorld.getObstacles().size());
            System.out.println("Pits restored: " + restoredWorld.getPits().size());
            System.out.println("Mines restored: " + restoredWorld.getMines().size());
            return true;

        } catch (PersistenceException e) {
            System.err.println("Restore failed for world '" + worldName
                    + "': " + e.getMessage());
            return false;
        }
    }
}


