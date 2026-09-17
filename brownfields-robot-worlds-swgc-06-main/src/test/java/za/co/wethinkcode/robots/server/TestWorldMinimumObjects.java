package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the minimum-object guarantee: a world created with no
 * configured obstacles or pits gets one of each, placed away from the
 * launch coordinate. Explicitly configured worlds are taken as-is.
 */
public class TestWorldMinimumObjects {

    @BeforeEach
    void setUp() {
        System.clearProperty("width");
        System.clearProperty("height");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("obstacles");
        System.clearProperty("pits");
        System.clearProperty("width");
        System.clearProperty("height");
    }

    @Test
    void aBareWorldGetsOneObstacleAndOnePit() {
        System.setProperty("obstacles", "none");
        System.setProperty("pits", "none");
        World world = new World();

        assertEquals(1, world.getObstacles().size());
        assertEquals(1, world.getPits().size());
        assertNull(world.getAObstacle(0, 0), "not on the launch coordinate");
        assertFalse(world.hasPitAt(0, 0), "not on the launch coordinate");
    }

    @Test
    void configuredObstaclesMeanNoGuaranteedObstacle() {
        System.setProperty("obstacles", "0,1");
        System.setProperty("pits", "none");
        World world = new World();

        assertEquals(1, world.getObstacles().size(), "exactly the configured one");
    }

    @Test
    void configuredObstaclesMeanNoGuaranteedPitEither() {
        System.setProperty("obstacles", "0,1");
        System.setProperty("pits", "none");
        World world = new World();

        assertEquals(0, world.getPits().size(),
                "an explicitly configured world is not 'helped' with a pit");
    }

    @Test
    void configuredPitsStillSkipTheGuarantee() {
        System.setProperty("obstacles", "none");
        System.setProperty("pits", "0,4");
        World world = new World();

        assertEquals(0, world.getObstacles().size());
        assertEquals(1, world.getPits().size());
    }
}
