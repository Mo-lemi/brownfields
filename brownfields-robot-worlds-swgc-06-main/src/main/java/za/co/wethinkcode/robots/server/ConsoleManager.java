package za.co.wethinkcode.robots.server;

import za.co.wethinkcode.robots.server.servercommands.ServerCommand;
import java.util.Scanner;

public class ConsoleManager implements Runnable {
    private final World world;

    public ConsoleManager(World world) {
        this.world = world;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine();
            System.out.println();
            ServerCommand command = ServerCommand.create(input, world);
            command.execute();

            if ("quit".equals(command.getName())) {
                System.exit(0);
            }
        }
    }
}