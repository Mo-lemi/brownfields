package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the mine coordinate getters and the mines list.
 */
public class TestWorldMineGetters {

    @Test
    void getMineAtReturnsTheCoordinates() {
        World world = new World(20, 20);
        world.placeMine(3, 7);

        int[] mine = world.getMineAt(3, 7);
        assertNotNull(mine);
        assertEquals(3, mine[0]);
        assertEquals(7, mine[1]);
    }

    @Test
    void getMineAtReturnsNullForFreeCoordinates() {
        World world = new World(20, 20);
        assertNull(world.getMineAt(5, 5));
    }

    @Test
    void theMinesListReflectsPlacements() {
        World world = new World(20, 20);
        world.placeMine(1, 1);
        world.placeMine(2, 2);
        assertEquals(2, world.getMines().size());

        world.explodeMineAt(1, 1);
        assertEquals(1, world.getMines().size());
    }

    @Test
    void canPlaceMineAtRejectsOutOfBoundsCoordinates() {
        World world = new World(5, 5);
        assertFalse(world.canPlaceMineAt(100, 100), "outside the world");
        assertTrue(world.canPlaceMineAt(2, 2), "inside the world");
    }

    @Test
    void placeMineRejectsOutOfBoundsCoordinates() {
        World world = new World(5, 5);
        assertFalse(world.placeMine(-50, -50));
        assertEquals(0, world.getMines().size());
    }
}
