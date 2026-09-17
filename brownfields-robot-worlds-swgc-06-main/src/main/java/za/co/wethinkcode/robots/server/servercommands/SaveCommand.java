package za.co.wethinkcode.robots.server.servercommands;

import za.co.wethinkcode.robots.server.World;
import za.co.wethinkcode.robots.server.database.PersistenceException;
import za.co.wethinkcode.robots.server.database.SqliteWorldRepository;
import za.co.wethinkcode.robots.server.database.WorldRepository;

import java.util.Scanner;
import java.util.function.Supplier;

public class SaveCommand extends ServerCommand {
    private final String worldName;
    private final WorldRepository repository;
    private final Supplier<String> confirmationReader;

    public SaveCommand(World world, String worldName) {
        this(world, worldName, new SqliteWorldRepository(),
                () -> new Scanner(System.in).nextLine());
    }

    public SaveCommand(World world, String worldName,
                       WorldRepository repository) {
        this(world, worldName, repository,
                () -> new Scanner(System.in).nextLine());
    }

    public SaveCommand(World world, String worldName,
                       WorldRepository repository,
                       Supplier<String> confirmationReader) {
        super("save", world);
        this.worldName = worldName;
        this.repository = repository;
        this.confirmationReader = confirmationReader;
    }

    @Override
    public boolean execute() {
        if (worldName == null || worldName.trim().isEmpty()) {
            System.out.println("Please provide a world name. e.g., save <world_name>");
            return false;
        }

        try {
            if (repository.worldExists(worldName)) {
                System.out.print("A world named '" + worldName
                        + "' already exists. Overwrite? (y/n): ");
                String response = confirmationReader.get().trim().toLowerCase();
                if (!response.equals("y")) {
                    System.out.println("Save cancelled.");
                    return false;
                }
            }

            repository.saveWorld(worldName, getWorld());
            System.out.println("World '" + worldName + "' saved successfully.");
            return true;
        } catch (PersistenceException e) {
            System.err.println("Save failed for world '" + worldName
                    + "': " + e.getMessage());
            return false;
        }
    }
}