package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.server.database.WorldRepository;
import za.co.wethinkcode.robots.server.obstacles.MountainObstacle;
import za.co.wethinkcode.robots.server.servercommands.SaveCommand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveCommandTest {

    @Test
    void savesTheCurrentWorldThroughTheRepository() {
        World world = new World(20, 15);
        world.addObstacle(new MountainObstacle(3, 4));
        RecordingRepository repository = new RecordingRepository();

        SaveCommand command = new SaveCommand(world, "testWorld", repository);

        assertTrue(command.execute());
        assertEquals("testWorld", repository.savedName);
        assertEquals(world, repository.savedWorld);
    }

    @Test
    void cancelsWhenOverwriteIsNotConfirmed() {
        World world = new World(20, 15);
        RecordingRepository repository = new RecordingRepository();
        repository.existingWorld = true;

        SaveCommand command = new SaveCommand(
                world, "testWorld", repository, () -> "n");

        assertFalse(command.execute());
        assertFalse(repository.wasSaved());
    }

    @Test
    void overwritesWhenOverwriteIsConfirmed() {
        World world = new World(20, 15);
        RecordingRepository repository = new RecordingRepository();
        repository.existingWorld = true;

        SaveCommand command = new SaveCommand(
                world, "testWorld", repository, () -> "y");

        assertTrue(command.execute());
        assertTrue(repository.wasSaved());
    }

    private static class RecordingRepository implements WorldRepository {
        private String savedName;
        private World savedWorld;
        private boolean existingWorld;

        @Override
        public boolean worldExists(String name) {
            return existingWorld;
        }

        @Override
        public void saveWorld(String name, World world) {
            savedName = name;
            savedWorld = world;
        }

        @Override
        public World loadWorld(String name) {
            return null;
        }

        private boolean wasSaved() {
            return savedWorld != null;
        }
    }
}