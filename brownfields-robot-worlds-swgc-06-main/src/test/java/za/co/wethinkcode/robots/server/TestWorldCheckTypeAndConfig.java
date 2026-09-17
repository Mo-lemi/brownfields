package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for World.checkType's five answers, the config getters, and
 * the world_config.properties defaults that ship with the project.
 */
public class TestWorldCheckTypeAndConfig {

    private void clearObjectProps() {
        System.clearProperty("obstacles");
        System.clearProperty("pits");
        System.clearProperty("width");
        System.clearProperty("height");
    }

    @Test
    void aFreeCellHasNoType() {
        clearObjectProps();
        World world = new World(20, 20);
        assertEquals("", world.checkType(5, 5));
    }

    @Test
    void aRobotCellIsTypedR() {
        clearObjectProps();
        World world = new World(20, 20);
        Robot robot = new Robot("hal", world, 5, 5, RobotTypes.SOLDIER);
        world.addRobot(robot);
        assertEquals("r", world.checkType(5, 5));
    }

    @Test
    void anObstacleCellIsTypedO() {
        clearObjectProps();
        World world = new World(20, 20);
        world.addObstacle(new za.co.wethinkcode.robots.server.obstacles.MountainObstacle(5, 5));
        assertEquals("o", world.checkType(5, 5));
    }

    @Test
    void aPitCellIsTypedP() {
        clearObjectProps();
        World world = new World(20, 20);
        world.addPit(new za.co.wethinkcode.robots.server.obstacles.PitObstacle(5, 5));
        assertEquals("p", world.checkType(5, 5));
    }

    @Test
    void anOutOfBoundsCellIsTypedE() {
        clearObjectProps();
        World world = new World(20, 20);
        assertEquals("e", world.checkType(100, 100));
    }

    @Test
    void theConfiguredTimesAreReadable() {
        clearObjectProps();
        World world = new World(20, 20);
        assertEquals(2, world.getReloadTime());
        assertEquals(3, world.getRepairTime());
        assertEquals(3, world.getSetMineTime());
        assertEquals(5, world.getLookDistance());
    }

    @Test
    void theConfigFileShipsWithAnObstacleAndAPit() {
        // the committed world_config.properties contains obstacles=0,2
        // and pits=0,4 - a fresh config-driven world starts with them
        clearObjectProps();
        World world = new World();
        assertNotNull(world.getAObstacle(0, 2), "config obstacle at (0,2)");
        assertTrue(world.hasPitAt(0, 4), "config pit at (0,4)");
    }

    @Test
    void configuredObjectsAppearInCheckType() {
        clearObjectProps();
        World world = new World();
        assertEquals("o", world.checkType(0, 2));
        assertEquals("p", world.checkType(0, 4));
    }
}
