package za.co.wethinkcode.robots.server.database;

import net.lemnik.eodsql.ResultColumn;

/**
 * Passive Data Object for the 'obstacles' table.
 * This single DO handles Mountains, Lakes, Pits, and Mines
 * because they all share the same table structure.
 */

public class ObstacleDO {
    @ResultColumn("world_name") public String worldName;
    @ResultColumn("type") public String type;
    @ResultColumn("x") public int x;
    @ResultColumn("y") public int y;
    public ObstacleDO() {}
}
