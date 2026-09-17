package za.co.wethinkcode.robots.server.database;

import za.co.wethinkcode.robots.server.World;

public interface WorldRepository {
    boolean worldExists(String name);
    void saveWorld(String name, World world);
    World loadWorld(String name);
}