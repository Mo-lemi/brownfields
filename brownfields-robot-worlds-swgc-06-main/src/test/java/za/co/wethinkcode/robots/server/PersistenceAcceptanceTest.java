package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.server.database.DatabaseManager;
import za.co.wethinkcode.robots.server.obstacles.LakeObstacle;
import za.co.wethinkcode.robots.server.obstacles.PitObstacle;
import za.co.wethinkcode.robots.server.servercommands.RestoreCommand;
import za.co.wethinkcode.robots.server.servercommands.SaveCommand;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceAcceptanceTest {

    private static final String WORLD_NAME = "persistenceAcceptanceWorld";

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.initializeDatabase();
        clearDatabase();
    }

    @AfterEach
    void tearDown() throws Exception {
        clearDatabase();
    }

    @Test
    void savesAndRestoresWorldGeometryWithoutRobots() {
        World savedWorld = new World(12, 10);
        savedWorld.addObstacle(new LakeObstacle(3, 4));
        savedWorld.addPit(new PitObstacle(6, 7));
        savedWorld.placeMine(1, 2);

        assertTrue(new SaveCommand(savedWorld, WORLD_NAME).execute());

        World activeWorld = new World(20, 20);
        activeWorld.addRobot(new Robot(
                "temporary", activeWorld, 0, 0, RobotTypes.JUGGERNAUT));
        activeWorld.addObstacle(new LakeObstacle(8, 8));

        assertTrue(new RestoreCommand(activeWorld, WORLD_NAME).execute());

        assertEquals(12, activeWorld.getWidth());
        assertEquals(10, activeWorld.getHeight());
        assertEquals(1, activeWorld.getObstacles().size());
        assertTrue(activeWorld.getAObstacle(3, 4) instanceof LakeObstacle);
        assertTrue(activeWorld.hasPitAt(6, 7));
        assertTrue(activeWorld.hasMineAt(1, 2));
        assertTrue(activeWorld.getRobots().isEmpty());
        assertFalse(activeWorld.hasMineAt(8, 8));
    }

    private void clearDatabase() throws Exception {
        try (Connection connection = DatabaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM obstacles");
            statement.executeUpdate("DELETE FROM pits");
            statement.executeUpdate("DELETE FROM mines");
            statement.executeUpdate("DELETE FROM worlds");
        }
    }
}
