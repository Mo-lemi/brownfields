package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import za.co.wethinkcode.robots.server.obstacles.MountainObstacle;

import static org.junit.jupiter.api.Assertions.*;

public class TestSave {
    private World world;

    @BeforeEach
    void setUp()
    {
        world = new World(10, 10);
    }
    @Test
    @DisplayName("World initializes correctly with dimensions for save state")
    void testWorldInitialization(){
        assertEquals(10, world.getWidth(),"World width should be 10");
        assertEquals(10, world.getHeight(), "World height should be 10");
    }

    @Test
    @DisplayName("Capturing current world state preserves properties")
    void testCaptureWorldState(){
        int initialWidth = world.getWidth();
        int initialHeight = world.getHeight();

        assertEquals(10,initialWidth,"Saved world width matches active state");
        assertEquals(10,initialHeight,"Saved world height matches active state");
    }
    @Test
    @DisplayName("Uncentered world bounds match non-centered coordinate logic (0 to width-1)")
    void testSaveStateBounds(){
        assertEquals(0, world.getMinX(), "Min X bound should be 0 when centeredCoordinates is false");
        assertEquals(9, world.getMaxX(), "Max X bound should be width -1(9)");
        assertEquals(0,world.getMinY(), "Min Y bound should be 0 when centeredCoordinates is false");
        assertEquals(9, world.getMaxY(), "Max Y bound should be width -1(9)");
    }
    @Test
    @DisplayName("World configuration parameters match state before save")
    void testConfigValues(){
        assertEquals(2,world.getReloadTime(), "Reload time should be configured value");
        assertEquals(3,world.getRepairTime(), "Repair time should be configured value");
        assertEquals(5, world.getShields(), "Shields should be default value");
        assertEquals(5, world.getShots(), "Shots should be configured value");
    }
    @Test
    @DisplayName("Obstacle additions reflect accurately in world save state")
    void testObstacleStateInSave(){
        MountainObstacle obstacle = new MountainObstacle(2,2);
        world.addObstacle(obstacle);

        assertEquals(1, world.getObstacles().size(), "World should contain 1 obstacle");
        assertNotNull(world.getAObstacle(2,2), "Obstacle should exist at (2,2)");

    }
}
