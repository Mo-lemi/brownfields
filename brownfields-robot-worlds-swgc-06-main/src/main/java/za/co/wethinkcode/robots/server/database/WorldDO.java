package za.co.wethinkcode.robots.server.database;

// This is the passive data for the 'worlds' table

import net.lemnik.eodsql.ResultColumn;

public class WorldDO {
    @ResultColumn("name") public String name;
    @ResultColumn("width") public int width;
    @ResultColumn("height") public int height;

    // EoDSQL requires a public no-argument constructor
    public WorldDO() {}
}
