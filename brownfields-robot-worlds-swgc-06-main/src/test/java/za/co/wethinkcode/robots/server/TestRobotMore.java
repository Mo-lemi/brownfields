package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * More Robot unit tests: counters, no-op behaviour on dead robots,
 * getters and the world-config caps.
 */
public class TestRobotMore {

    private World world;

    @BeforeEach
    void setUp() {
        world = new World(20, 20);
    }

    private Robot soldier(String name) {
        Robot robot = new Robot(name, world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);
        return robot;
    }

    @Test
    void hitsCountOnAMiningDeath() {
        Robot robot = soldier("hal");
        robot.startMining();
        robot.takeHit();
        assertEquals(1, robot.getHits(), "the hit still counts before death");
    }

    @Test
    void takeHitOnADeadRobotIsANoOp() {
        Robot robot = soldier("hal");
        robot.setDead();
        int hitsBefore = robot.getHits();
        robot.takeHit();
        assertEquals(hitsBefore, robot.getHits(), "dead robots take no further hits");
        assertTrue(robot.isDead());
    }

    @Test
    void shotsCanBeSpentAndReadBack() {
        Robot robot = soldier("hal");
        robot.setShots(1);
        assertEquals(1, robot.getShots());
    }

    @Test
    void everyRobotStartsFacingNorth() {
        Robot robot = soldier("hal");
        assertEquals(Direction.NORTH, robot.getDirection());
    }

    @Test
    void getTypeReturnsTheConfiguredKind() {
        assertEquals(RobotTypes.SOLDIER, soldier("hal").getType());
        assertEquals(RobotTypes.INTRUDER,
                new Robot("digger", world, 0, 0, RobotTypes.INTRUDER).getType());
    }

    @Test
    void worldShieldCapLimitsTheStartingShield() {
        System.setProperty("shields", "2");
        World capped = new World();
        Robot robot = new Robot("hal", capped, 0, 0, RobotTypes.SOLDIER);
        assertEquals(2, robot.getShield(), "min(type 3, world 2) = 2");
        System.clearProperty("shields");
    }

    @Test
    void worldShotCapLimitsTheStartingShots() {
        System.setProperty("shots", "2");
        World capped = new World();
        Robot robot = new Robot("hal", capped, 0, 0, RobotTypes.SOLDIER);
        assertEquals(2, robot.getShots(), "min(type 3, world 2) = 2");
        System.clearProperty("shots");
    }

    @Test
    void statusIsNormalAtConstruction() {
        assertEquals("NORMAL", soldier("hal").getStatus());
    }
}
