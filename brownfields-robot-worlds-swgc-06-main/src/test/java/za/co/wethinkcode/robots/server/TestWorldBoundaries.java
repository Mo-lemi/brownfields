package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for World boundaries, the coordinate system variants and
 * the state-copy methods (restoreState / loadFromDatabase).
 */
public class TestWorldBoundaries {

    @BeforeEach
    void setUp() {
        // an explicitly configured world - the guarantee stays out
        System.setProperty("obstacles", "none");
        System.setProperty("pits", "none");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("obstacles");
        System.clearProperty("pits");
        System.clearProperty("width");
        System.clearProperty("height");
    }

    @Test
    void centeredWorldBoundaries() {
        // pin the size so the test does not depend on the config file
        // or on properties leaked by other test classes
        System.setProperty("width", "20");
        System.setProperty("height", "20");
        World world = new World();
        assertEquals(-10, world.getMinX());
        assertEquals(10, world.getMaxX());
        assertEquals(-10, world.getMinY());
        assertEquals(10, world.getMaxY());
        assertTrue(world.isInsideBounds(10, 10), "the corner cell is inside");
        assertTrue(world.isInsideBounds(-10, -10), "the opposite corner too");
        assertFalse(world.isInsideBounds(11, 0), "outside the east edge");
        assertFalse(world.isInsideBounds(0, -11), "outside the south edge");
        // the cell just inside the corner is free and enterable
        assertTrue(world.isValidPosition(9, 9));
        assertFalse(world.isValidPosition(11, 0));
    }

    @Test
    void plainWorldBoundariesStartAtZero() {
        // the (width, height) constructor is not centred: 0 .. size-1
        World world = new World(3, 3);
        assertEquals(0, world.getMinX());
        assertEquals(2, world.getMaxX());
        assertTrue(world.isValidPosition(2, 2));
        assertFalse(world.isValidPosition(3, 0));
        assertFalse(world.isValidPosition(-1, 0));
    }

    @Test
    void isInsideBoundsMatchesTheBounds() {
        World world = new World(5, 5);
        assertTrue(world.isInsideBounds(0, 0));
        assertTrue(world.isInsideBounds(4, 4));
        assertFalse(world.isInsideBounds(5, 0));
        assertFalse(world.isInsideBounds(-1, 2));
    }

    @Test
    void restoreStateCopiesDimensionsObstaclesAndPits() {
        World saved = new World(5, 5);
        saved.addObstacle(new za.co.wethinkcode.robots.server.obstacles.MountainObstacle(1, 1));
        saved.addPit(new za.co.wethinkcode.robots.server.obstacles.PitObstacle(3, 3));

        World active = new World(20, 20);
        active.restoreState(saved);

        assertEquals(5, active.getWidth());
        assertEquals(5, active.getHeight());
        assertEquals(1, active.getObstacles().size());
        assertEquals(1, active.getPits().size());
        assertTrue(active.hasPitAt(3, 3));
    }

    @Test
    void restoreStateReplacesObjectsAndClearsRobots() {
        World saved = new World(5, 5);
        saved.addObstacle(new za.co.wethinkcode.robots.server.obstacles.MountainObstacle(1, 1));
        saved.addPit(new za.co.wethinkcode.robots.server.obstacles.PitObstacle(3, 3));

        World active = new World(20, 20);
        Robot activeRobot = new Robot("active", active, 1, 1, RobotTypes.SOLDIER);
        active.addRobot(activeRobot);
        active.restoreState(saved);

        assertEquals(5, active.getWidth());
        assertEquals(1, active.getObstacles().size());
        assertTrue(active.hasPitAt(3, 3));
        assertNull(active.getARobot("active"),
                "restore clears runtime robot state - robots are not saved objects");
    }

    @Test
    void placeMineRejectsADuplicateCoordinate() {
        World world = new World(5, 5);
        assertTrue(world.placeMine(1, 1));
        assertFalse(world.placeMine(1, 1), "one mine per coordinate");
        assertEquals(1, world.getMines().size());

        world.explodeMineAt(1, 1);
        assertFalse(world.hasMineAt(1, 1), "the detonated mine is gone");
    }

    @Test
    void addPitAcceptsOverlappingPits() {
        World world = new World(5, 5);
        world.addPit(new za.co.wethinkcode.robots.server.obstacles.PitObstacle(2, 2));
        world.addPit(new za.co.wethinkcode.robots.server.obstacles.PitObstacle(2, 2));

        assertEquals(2, world.getPits().size(), "pits may overlap each other");
        assertTrue(world.hasPitAt(2, 2));
    }
}
