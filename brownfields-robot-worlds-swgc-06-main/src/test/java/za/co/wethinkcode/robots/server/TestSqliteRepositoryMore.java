package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.server.database.PersistenceException;
import za.co.wethinkcode.robots.server.database.SqliteWorldRepository;
import za.co.wethinkcode.robots.server.database.WorldRepository;
import za.co.wethinkcode.robots.server.obstacles.LakeObstacle;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * More data-access tests using an injected connection provider backed by
 * a temporary database file: overwrite behaviour, unknown worlds,
 * malformed rows and empty worlds.
 */
public class TestSqliteRepositoryMore {

    private File databaseFile;
    private Supplier<Connection> provider;
    private WorldRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        databaseFile = File.createTempFile("rw-repo-test", ".db");
        String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS worlds (" +
                    "name TEXT PRIMARY KEY, width INTEGER NOT NULL, height INTEGER NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS obstacles (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, world_name TEXT NOT NULL, " +
                    "type TEXT NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, " +
                    "size INTEGER DEFAULT 1)");
            statement.execute("CREATE TABLE IF NOT EXISTS pits (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, world_name TEXT NOT NULL, " +
                    "x INTEGER NOT NULL, y INTEGER NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS mines (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, world_name TEXT NOT NULL, " +
                    "x INTEGER NOT NULL, y INTEGER NOT NULL)");
        }
        provider = () -> {
            try {
                return DriverManager.getConnection(url);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };
        repository = new SqliteWorldRepository(provider::get);
    }

    @AfterEach
    void tearDown() {
        if (databaseFile != null && databaseFile.exists()) {
            assertTrue(databaseFile.delete() || databaseFile.delete(),
                    "the temp database should be removable");
        }
    }

    @Test
    void worldExistsIsFalseBeforeAndTrueAfterSaving() {
        World world = new World(5, 5);
        assertFalse(repository.worldExists("alpha"));
        repository.saveWorld("alpha", world);
        assertTrue(repository.worldExists("alpha"));
    }

    @Test
    void overwritingAWorldDoesNotDuplicateItsObstacles() {
        World first = new World(5, 5);
        first.addObstacle(new za.co.wethinkcode.robots.server.obstacles.MountainObstacle(1, 1));
        first.addObstacle(new za.co.wethinkcode.robots.server.obstacles.LakeObstacle(2, 2));
        repository.saveWorld("alpha", first);

        World second = new World(5, 5);
        second.addObstacle(new za.co.wethinkcode.robots.server.obstacles.MountainObstacle(3, 3));
        repository.saveWorld("alpha", second);

        World loaded = repository.loadWorld("alpha");
        assertEquals(1, loaded.getObstacles().size(), "the old rows were deleted");
        assertNotNull(loaded.getAObstacle(3, 3), "the new rows are in");
    }

    @Test
    void loadWorldReturnsNullForAnUnknownName() {
        assertNull(repository.loadWorld("never-saved"));
    }

    @Test
    void anEmptyWorldRoundTripsWithoutObjects() {
        repository.saveWorld("empty", new World(5, 5));

        World loaded = repository.loadWorld("empty");
        assertEquals(5, loaded.getWidth());
        assertEquals(0, loaded.getObstacles().size());
        assertEquals(0, loaded.getPits().size());
    }

    @Test
    void aMalformedObstacleTypeFailsTheLoad() throws Exception {
        World world = new World(5, 5);
        world.addObstacle(new za.co.wethinkcode.robots.server.obstacles.MountainObstacle(1, 1));
        repository.saveWorld("alpha", world);

        // corrupt the row the way a broken writer would
        try (Connection connection = provider.get();
             Statement statement = connection.createStatement()) {
            statement.execute("UPDATE obstacles SET type = 'dragon' WHERE world_name = 'alpha'");
        }

        assertThrows(PersistenceException.class, () -> repository.loadWorld("alpha"),
                "an unknown obstacle type must fail loudly, not silently");
    }

    @Test
    void lakesSurviveTheRoundTripWithTheirType() {
        World world = new World(5, 5);
        world.addObstacle(new LakeObstacle(2, 2));
        repository.saveWorld("lake-world", world);

        World loaded = repository.loadWorld("lake-world");
        assertEquals("lake", loaded.getObstacles().getFirst().getType(),
                "the obstacle type survives the round trip");
    }
}
