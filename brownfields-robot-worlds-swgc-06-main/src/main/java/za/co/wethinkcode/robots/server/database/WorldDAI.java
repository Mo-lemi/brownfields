package za.co.wethinkcode.robots.server.database;

import net.lemnik.eodsql.BaseQuery;
import net.lemnik.eodsql.Select;
import net.lemnik.eodsql.Update;

public interface WorldDAI extends BaseQuery {

    @Select("SELECT COUNT(*) FROM worlds WHERE name = ?{1}")
    int countWorldsByName(String name);

    @Select("SELECT name, width, height FROM worlds WHERE name = ?{1}")
    WorldDO selectWorldByName(String name);

    @Update("INSERT OR REPLACE INTO worlds (name, width, height) VALUES (?{1}, ?{2}, ?{3})")
    void insertWorld(String name, int width, int height);

    @Update("DELETE FROM obstacles WHERE world_name = ?{1}")
    void deleteObstacles(String worldName);

    @Update("DELETE FROM pits WHERE world_name = ?{1}")
    void deletePits(String worldName);

    @Update("DELETE FROM mines WHERE world_name = ?{1}")
    void deleteMines(String worldName);

    @Update("INSERT INTO obstacles (world_name, type, x, y) VALUES (?{1}, ?{2}, ?{3}, ?{4})")
    void insertObstacle(String worldName, String type, int x, int y);

    @Update("INSERT INTO pits (world_name, x, y) VALUES (?{1}, ?{2}, ?{3})")
    void insertPit(String worldName, int x, int y);

    @Update("INSERT INTO mines (world_name, x, y) VALUES (?{1}, ?{2}, ?{3})")
    void insertMine(String worldName, int x, int y);

    // BULLETPROOF SINGLE-ROW SELECTS (No DataSet, No Cursor Bugs!)
    @Select("SELECT world_name, type, x, y FROM obstacles WHERE world_name = ?{1} ORDER BY rowid LIMIT 1 OFFSET ?{2}")
    ObstacleDO selectObstacleAt(String worldName, int offset);

    @Select("SELECT world_name, x, y FROM pits WHERE world_name = ?{1} ORDER BY rowid LIMIT 1 OFFSET ?{2}")
    PitDO selectPitAt(String worldName, int offset);

    @Select("SELECT world_name, x, y FROM mines WHERE world_name = ?{1} ORDER BY rowid LIMIT 1 OFFSET ?{2}")
    MineDO selectMineAt(String worldName, int offset);
}