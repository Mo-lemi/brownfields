package za.co.wethinkcode.robots.server.database;

import net.lemnik.eodsql.QueryTool;
import za.co.wethinkcode.robots.server.World;
import za.co.wethinkcode.robots.server.obstacles.LakeObstacle;
import za.co.wethinkcode.robots.server.obstacles.MountainObstacle;
import za.co.wethinkcode.robots.server.obstacles.Obstacle;
import za.co.wethinkcode.robots.server.obstacles.PitObstacle;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class SqliteWorldRepository implements WorldRepository {

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }

    private final ConnectionProvider connectionProvider;

    public SqliteWorldRepository() {
        this(DatabaseManager::getConnection);
    }

    public SqliteWorldRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    /** Gives the EoDSQL-generated implementation of the DAI. */
    private WorldDAI dai(Connection conn) {
        return QueryTool.getQuery(conn, WorldDAI.class);
    }

    @Override
    public boolean worldExists(String name) {
        try (Connection conn = connectionProvider.getConnection()) {
            return dai(conn).countWorldsByName(name) > 0;
        } catch (SQLException e) {
            throw new PersistenceException("Database error checking if world exists", e);
        }
    }

    @Override
    public void saveWorld(String name, World world) {
        try (Connection conn = connectionProvider.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                WorldDAI dao = dai(conn);

                // 1. Clearing previous geometry (overwrite support)
                dao.deleteObstacles(name);
                dao.deletePits(name);
                dao.deleteMines(name);

                // 2. World dimensions
                dao.insertWorld(name, world.getWidth(), world.getHeight());

                // 3. Obstacles (Save everything, including Pits if they are in this list)
                for (Obstacle o : world.getObstacles()) {
                    String typeStr = o.getType();
                    if (typeStr == null || typeStr.isBlank()) {
                        typeStr = "mountain";
                        if (o instanceof LakeObstacle) typeStr = "lake";
                        if (o instanceof PitObstacle) typeStr = "pit";
                    }
                    dao.insertObstacle(name, typeStr.toLowerCase(), o.getX(), o.getY());
                }

                // 4. Pits (Only save if NOT already in the obstacles list to prevent duplication)
                for (PitObstacle p : world.getPits()) {
                    boolean alreadySaved = false;
                    for (Obstacle o : world.getObstacles()) {
                        if (o instanceof PitObstacle && o.getX() == p.getX() && o.getY() == p.getY()) {
                            alreadySaved = true;
                            break;
                        }
                    }
                    if (!alreadySaved) {
                        dao.insertPit(name, p.getX(), p.getY());
                    }
                }

                // 5. Mines
                for (int[] m : world.getMines()) {
                    dao.insertMine(name, m[0], m[1]);
                }

                conn.commit();

            } catch (SQLException | RuntimeException e) {
                try { conn.rollback(); } catch (SQLException rbEx) { e.addSuppressed(rbEx); }
                if (e instanceof SQLException sqle) throw new PersistenceException("Database error saving world", sqle);
                throw (RuntimeException) e;
            } finally {
                if (conn.getAutoCommit() != originalAutoCommit) conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Database error saving world", e);
        }
    }

    @Override
    public World loadWorld(String name) {
        try (Connection conn = connectionProvider.getConnection()) {
            WorldDAI dao = dai(conn);

            WorldDO worldDO = dao.selectWorldByName(name);
            if (worldDO == null) return null;

            World loadedWorld = new World(worldDO.width, worldDO.height);

            // 1. Fetch Obstacles
            for (int i = 0; ; i++) {
                ObstacleDO o = dao.selectObstacleAt(name, i);
                if (o == null) break;
                loadedWorld.addObstacle(createObstacle(o.type, o.x, o.y));
            }

            // 2. Fetch Pits (Only add if not already added via the obstacles table)
            for (int i = 0; ; i++) {
                PitDO p = dao.selectPitAt(name, i);
                if (p == null) break;

                boolean alreadyLoaded = false;
                for (Obstacle o : loadedWorld.getObstacles()) {
                    if (o instanceof PitObstacle && o.getX() == p.x && o.getY() == p.y) {
                        alreadyLoaded = true;
                        break;
                    }
                }
                if (!alreadyLoaded) {
                    loadedWorld.addPit(new PitObstacle(p.x, p.y));
                }
            }

            // 3. Fetch Mines (Bypass placeMine to allow negative coordinates from centered worlds)
            for (int i = 0; ; i++) {
                MineDO m = dao.selectMineAt(name, i);
                if (m == null) break;
                loadedWorld.getMines().add(new int[]{m.x, m.y});
            }

            return loadedWorld;

        } catch (SQLException e) {
            throw new PersistenceException("Database error loading world", e);
        }
    }

    private Obstacle createObstacle(String type, int x, int y) {
        return switch (type.toLowerCase()) {
            case "mountain" -> new MountainObstacle(x, y);
            case "lake" -> new LakeObstacle(x, y);
            case "pit" -> new PitObstacle(x, y);
            default -> throw new PersistenceException("Malformed obstacle type: " + type);
        };
    }
}