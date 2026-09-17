package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the bare two-argument World constructor's defaults,
 * which the tests rely on everywhere.
 */
public class TestWorldBareConstructorDefaults {

    @Test
    void dimensionsAreTakenFromTheArguments() {
        World world = new World(13, 17);
        assertEquals(13, world.getWidth());
        assertEquals(17, world.getHeight());
    }

    @Test
    void thePlainWorldIsNotCentred() {
        World world = new World(10, 10);
        assertEquals(0, world.getMinX(), "x starts at 0");
        assertEquals(9, world.getMaxX(), "x ends at width-1");
        assertEquals(0, world.getMinY());
        assertEquals(9, world.getMaxY());
    }

    @Test
    void theGameTuningDefaultsAreSensible() {
        World world = new World(10, 10);
        assertEquals(5, world.getShields());
        assertEquals(5, world.getShots());
        assertEquals(2, world.getReloadTime());
        assertEquals(3, world.getRepairTime());
        assertEquals(3, world.getSetMineTime());
        assertEquals(5, world.getLookDistance());
    }

    @Test
    void aFreshPlainWorldIsEmpty() {
        World world = new World(10, 10);
        assertEquals(0, world.getRobots().size());
        assertEquals(0, world.getObstacles().size());
        assertEquals(0, world.getPits().size());
        assertEquals(0, world.getMines().size());
    }
}
