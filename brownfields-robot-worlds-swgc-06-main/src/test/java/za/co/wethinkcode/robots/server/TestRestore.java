package za.co.wethinkcode.robots.server;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import za.co.wethinkcode.robots.server.database.WorldRepository;
import za.co.wethinkcode.robots.server.database.PersistenceException;
import za.co.wethinkcode.robots.server.obstacles.MountainObstacle;
import za.co.wethinkcode.robots.server.obstacles.PitObstacle;
import za.co.wethinkcode.robots.server.servercommands.RestoreCommand;

import static org.junit.jupiter.api.Assertions.*;


public class TestRestore {
    private World activeWorld;
    private World savedWorld;

    @BeforeEach
    void setUp() {
        activeWorld = new World(20,20);
        savedWorld = new World(10,10);
    }
    @Test
    @DisplayName("restoreState updates active world dimensions to saved snapshot")
    void testRestoreDimensions() {
        activeWorld.restoreState(savedWorld);

        assertEquals(10, activeWorld.getWidth(), "Active world width should update to 10");
        assertEquals(10, activeWorld.getHeight(), "Active world height should update to 10");

    }
    @Test
    @DisplayName("restoreState populates saved obstacles into active world")
    void testRestoreSavedObstacles() {
        savedWorld.addObstacle(new MountainObstacle(2,2));

        activeWorld.restoreState(savedWorld);

        assertEquals(1, activeWorld.getObstacles().size(), "Active world should have 1 restored obstacle");
        assertNotNull(activeWorld.getAObstacle(2,2), "Obstacle at (2,2) should be present after restore");
    }
    @Test
    @DisplayName("restoreState clears active obstacles that were not in ")
    void testClearPostSaveObstacles(){
        savedWorld.addObstacle(new MountainObstacle(2,2));
        activeWorld.addObstacle(new MountainObstacle(5,5));

        activeWorld.restoreState(savedWorld);

        assertNull(activeWorld.getAObstacle(5,5), "Post-save obstacle at (5,5) should be removed");
        assertNotNull(activeWorld.getAObstacle(2,2), "Saved obstacle at (2,2) should remain");
    }

    @Test
    @DisplayName("restoreState rehydrates pits and mines from the saved snapshot")
    void testRestorePitsAndMines() {
        savedWorld.addPit(new PitObstacle(3, 4));
        savedWorld.placeMine(6, 7);

        activeWorld.restoreState(savedWorld);

        assertEquals(1, activeWorld.getPits().size(), "Saved pit should be restored");
        assertTrue(activeWorld.hasPitAt(3, 4), "Pit at (3,4) should remain after restore");
        assertTrue(activeWorld.hasMineAt(6, 7), "Mine at (6,7) should be restored");
    }

    @Test
    @DisplayName("restore command applies the repository snapshot")
    void testRestoreCommandUsesRepository() {
        WorldRepository repository = new WorldRepository() {
            @Override
            public boolean worldExists(String name) {
                return true;
            }

            @Override
            public void saveWorld(String name, World world) {
                throw new UnsupportedOperationException();
            }

            @Override
            public World loadWorld(String name) {
                return savedWorld;
            }
        };
        RestoreCommand command =
                new RestoreCommand(activeWorld, "saved", repository);

        assertTrue(command.execute());
        assertEquals(savedWorld.getWidth(), activeWorld.getWidth());
        assertEquals(savedWorld.getHeight(), activeWorld.getHeight());
    }

    @Test
    @DisplayName("restore command leaves the active world unchanged on failure")
    void testRestoreCommandDoesNotPartiallyApplyFailure() {
        activeWorld.addObstacle(new MountainObstacle(5, 5));
        WorldRepository repository = new WorldRepository() {
            @Override
            public boolean worldExists(String name) {
                return true;
            }

            @Override
            public void saveWorld(String name, World world) {
                throw new UnsupportedOperationException();
            }

            @Override
            public World loadWorld(String name) {
                throw new PersistenceException("malformed saved world");
            }
        };

        RestoreCommand command =
                new RestoreCommand(activeWorld, "broken", repository);

        assertFalse(command.execute());
        assertEquals(20, activeWorld.getWidth());
        assertEquals(20, activeWorld.getHeight());
        assertNotNull(activeWorld.getAObstacle(5, 5));
    }
}
