package za.co.wethinkcode.robots.server.database;

import net.lemnik.eodsql.ResultColumn;

public class PitDO {
    @ResultColumn("world_name") public String worldName;
    @ResultColumn("x") public int x;
    @ResultColumn("y") public int y;

    public PitDO() {}
}
