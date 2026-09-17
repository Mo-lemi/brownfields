package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.server.database.DatabaseManager;
import za.co.wethinkcode.robots.server.database.PersistenceException;
import za.co.wethinkcode.robots.server.database.SqliteWorldRepository;
import za.co.wethinkcode.robots.server.obstacles.LakeObstacle;
import za.co.wethinkcode.robots.server.obstacles.MountainObstacle;
import za.co.wethinkcode.robots.server.obstacles.Obstacle;
import za.co.wethinkcode.robots.server.obstacles.PitObstacle;
import za.co.wethinkcode.robots.server.servercommands.RestoreCommand;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SqlNoDataSourceInspection")
class SqliteWorldRepositoryTest {
    private SqliteWorldRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.initializeDatabase();
        clearDatabase();
        repository = new SqliteWorldRepository();
    }

    @AfterEach
    void tearDown() throws Exception {
        clearDatabase();
    }

    private void clearDatabase() throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM obstacles");
            stmt.executeUpdate("DELETE FROM pits");
            stmt.executeUpdate("DELETE FROM mines");
            stmt.executeUpdate("DELETE FROM worlds");
        }
    }

    @Test
    void shouldReturnTrueWhenWorldExists() throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("INSERT INTO worlds (name, width, height) VALUES ('testWorld', 100, 100)");
        }
        assertTrue(repository.worldExists("testWorld"));
    }

    @Test
    void shouldReturnFalseWhenWorldDoesNotExist() {
        assertFalse(repository.worldExists("doesNotExist"));
    }

    @Test
    void shouldReportConnectionFailureWhenCheckingWorld() {
        SqliteWorldRepository failingRepository =
                new SqliteWorldRepository(() -> {
                    throw new java.sql.SQLException("connection failed");
                });
        assertThrows(PersistenceException.class,
                () -> failingRepository.worldExists("testWorld"));
    }

    @Test
    void shouldSaveWorld() {
        repository.saveWorld("testWorld", new World(100, 100));
        assertTrue(repository.worldExists("testWorld"));
    }

    @Test
    void shouldSaveWorldDimensions() throws Exception {
        repository.saveWorld("dimensionWorld", new World(200, 150));
        try (Connection conn = DatabaseManager.getConnection();
             var pstmt = conn.prepareStatement(
                     "SELECT width, height FROM worlds WHERE name = ?")) {
            pstmt.setString(1, "dimensionWorld");
            try (var rs = pstmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(200, rs.getInt("width"));
                assertEquals(150, rs.getInt("height"));
            }
        }
    }

    @Test
    void shouldSaveMultipleObstacles() {
        World world = new World(100, 100);
        world.addObstacle(new MountainObstacle(10, 20));
        world.addObstacle(new LakeObstacle(30, 40));
        world.addObstacle(new PitObstacle(50, 60));
        repository.saveWorld("obstacleWorld", world);

        World loadedWorld = repository.loadWorld("obstacleWorld");
        assertNotNull(loadedWorld);
        assertEquals(3, loadedWorld.getObstacles().size());
    }

    @Test
    void shouldLoadWorld() {
        repository.saveWorld("loadWorld", new World(100, 100));
        World loadedWorld = repository.loadWorld("loadWorld");
        assertNotNull(loadedWorld);
        assertEquals(100, loadedWorld.getWidth());
        assertEquals(100, loadedWorld.getHeight());
    }

    @Test
    void shouldReturnNullWhenLoadingNonExistingWorld() {
        assertNull(repository.loadWorld("doesNotExist"));
    }

    @Test
    void shouldSaveAndLoadMountainObstacle() {
        verifyObstaclePersistence(new MountainObstacle(10, 20), "mountainWorld", MountainObstacle.class);
    }

    @Test
    void shouldSaveAndLoadLakeObstacle() {
        verifyObstaclePersistence(new LakeObstacle(30, 40), "lakeWorld", LakeObstacle.class);
    }

    @Test
    void shouldSaveAndLoadPitObstacle() {
        verifyObstaclePersistence(new PitObstacle(50, 60), "pitWorld", PitObstacle.class);
    }

    @Test
    void shouldReplaceExistingWorld() {
        World firstWorld = new World(100, 100);
        firstWorld.addObstacle(new MountainObstacle(10, 10));
        repository.saveWorld("replaceWorld", firstWorld);

        World secondWorld = new World(200, 200);
        secondWorld.addObstacle(new LakeObstacle(20, 20));
        repository.saveWorld("replaceWorld", secondWorld);

        World loadedWorld = repository.loadWorld("replaceWorld");
        assertWorldState(loadedWorld, 200, 200, 1, 0, 0);

        Obstacle obstacle = loadedWorld.getObstacles().getFirst();
        assertTrue(obstacle instanceof LakeObstacle);
        assertEquals(20, obstacle.getX());
        assertEquals(20, obstacle.getY());
    }

    @Test
    void shouldKeepNamedWorldsIndependent() {
        World firstWorld = new World(80, 60);
        firstWorld.addObstacle(new MountainObstacle(10, 10));

        World secondWorld = new World(40, 30);
        secondWorld.addPit(new PitObstacle(5, 6));
        secondWorld.placeMine(2, 3);

        repository.saveWorld("firstWorld", firstWorld);
        repository.saveWorld("secondWorld", secondWorld);

        assertWorldState(repository.loadWorld("firstWorld"), 80, 60, 1, 0, 0);

        World loadedSecondWorld = repository.loadWorld("secondWorld");
        assertWorldState(loadedSecondWorld, 40, 30, 0, 1, 1);
        assertTrue(loadedSecondWorld.hasPitAt(5, 6));
        assertTrue(loadedSecondWorld.hasMineAt(2, 3));
    }

    @Test
    void shouldPersistWorldGeometryIncludingPitsAndMines() {
        World world = new World(50, 50);
        world.addObstacle(new MountainObstacle(10, 20));
        world.addObstacle(new LakeObstacle(25, 30));
        world.addPit(new PitObstacle(12, 12));
        world.placeMine(5, 5);
        world.placeMine(7, 9);
        repository.saveWorld("geometryWorld", world);

        World loadedWorld = repository.loadWorld("geometryWorld");
        assertWorldState(loadedWorld, 50, 50, 2, 1, 2);
        assertTrue(loadedWorld.hasPitAt(12, 12));
        assertTrue(loadedWorld.hasMineAt(5, 5));
        assertTrue(loadedWorld.hasMineAt(7, 9));
    }

    @Test
    void shouldRestoreWorldGeometryIncludingPitsAndMines() {
        World world = new World(60, 60);
        world.addObstacle(new MountainObstacle(3, 4));
        world.addPit(new PitObstacle(8, 8));
        world.placeMine(1, 1);
        world.placeMine(2, 3);
        repository.saveWorld("restoreGeometryWorld", world);

        World restoredWorld = new World(1, 1);
        RestoreCommand restoreCommand = new RestoreCommand(restoredWorld, "restoreGeometryWorld");
        assertTrue(restoreCommand.execute());

        assertWorldState(restoredWorld, 60, 60, 1, 1, 2);
        assertTrue(restoredWorld.hasPitAt(8, 8));
        assertTrue(restoredWorld.hasMineAt(1, 1));
        assertTrue(restoredWorld.hasMineAt(2, 3));
    }

    // --- Helper Methods to resolve CodeScene Biomarkers ---

    private void assertWorldState(World world, int width, int height, int obsCount, int pitCount, int mineCount) {
        assertNotNull(world, "World should not be null");
        assertEquals(width, world.getWidth(), "Width mismatch");
        assertEquals(height, world.getHeight(), "Height mismatch");
        assertEquals(obsCount, world.getObstacles().size(), "Obstacles count mismatch");
        assertEquals(pitCount, world.getPits().size(), "Pits count mismatch");
        assertEquals(mineCount, world.getMines().size(), "Mines count mismatch");
    }

    private void verifyObstaclePersistence(Obstacle obstacle, String worldName, Class<? extends Obstacle> type) {
        World world = new World(100, 100);
        world.addObstacle(obstacle);
        repository.saveWorld(worldName, world);

        World loadedWorld = repository.loadWorld(worldName);
        assertNotNull(loadedWorld);
        assertEquals(1, loadedWorld.getObstacles().size());

        Obstacle loadedObstacle = loadedWorld.getObstacles().getFirst();
        assertTrue(type.isInstance(loadedObstacle), "Expected instance of " + type.getSimpleName());
        assertEquals(obstacle.getX(), loadedObstacle.getX(), "X coordinate mismatch");
        assertEquals(obstacle.getY(), loadedObstacle.getY(), "Y coordinate mismatch");
    }
}