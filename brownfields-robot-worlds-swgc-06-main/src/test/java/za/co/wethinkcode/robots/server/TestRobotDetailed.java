package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Detailed unit tests for Robot: shield and hit mechanics, mine damage,
 * the SetMine flags, death handling and the repair/reload caps.
 */
public class TestRobotDetailed {

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
    void takeHitReducesShieldsOneAtATime() {
        Robot robot = soldier("hal");
        robot.takeHit();
        assertEquals(2, robot.getShield());
        robot.takeHit();
        assertEquals(1, robot.getShield());
        assertFalse(robot.isDead());
    }

    @Test
    void hitsCounterIncreasesWithEveryHit() {
        Robot robot = soldier("hal");
        robot.takeHit();
        robot.takeHit();
        assertEquals(2, robot.getHits());
    }

    @Test
    void robotDiesWhenShieldIsDepleted() {
        Robot robot = soldier("sniper"); // shield 3
        robot.takeMineHit();                 // shield 3 -> 0, still alive
        assertEquals(0, robot.getShield());
        assertFalse(robot.isDead());
        robot.takeHit();                 // no shield left -> dead
        assertTrue(robot.isDead());
    }

    @Test
    void takeMineHitLeavesShieldAtZeroButAlive() {
        Robot robot = soldier("hal");    // shield 3
        robot.takeMineHit();             // three hits: 3 -> 2 -> 1 -> 0
        assertEquals(0, robot.getShield());
        assertFalse(robot.isDead(), "exactly 3 shields absorbed the mine");
    }

    @Test
    void takeMineHitKillsARobotWithDamagedShields() {
        Robot robot = soldier("hal");    // shield 3
        robot.takeHit();                 // shield 2 - not enough for a mine
        robot.takeMineHit();
        assertTrue(robot.isDead());
    }

    @Test
    void takeMineHitDoesNotResurrectDeadRobots() {
        Robot robot = soldier("hal");
        robot.setDead();
        robot.takeMineHit();
        assertTrue(robot.isDead());
        assertEquals(0, robot.getShield());
    }

    @Test
    void setDeadRemovesTheRobotFromTheWorld() {
        Robot robot = soldier("hal");
        robot.setDead();
        assertTrue(robot.isDead());
        assertNull(world.getARobot("hal"));
    }

    @Test
    void miningFlagLifecycleMatchesTheStatus() {
        Robot robot = soldier("hal");
        assertFalse(robot.isMining());

        robot.startMining();
        assertTrue(robot.isMining());
        assertEquals("SETMINE", robot.getStatus());

        // the vulnerability window ends when the mine is in the ground...
        robot.stopMining();
        // ...but the status only changes once the response went out
        assertEquals("SETMINE", robot.getStatus());
        // and the shields absorb again - a hit is no longer instantly fatal
        robot.takeHit();
        assertFalse(robot.isDead(), "after stopMining the shields work again");
    }

    @Test
    void takeHitWhileMiningKillsInstantly() {
        Robot robot = soldier("hal");    // shield 3 would survive a hit
        robot.startMining();
        robot.takeHit();
        assertTrue(robot.isDead(), "shields are disabled while setting a mine");
    }

    @Test
    void repairShieldRestoresToTheTypeCap() {
        Robot robot = soldier("hal");    // type shield 3, world allows 5
        robot.takeHit();
        assertEquals(2, robot.getShield());
        robot.repairShield();
        assertEquals(3, robot.getShield(), "capped at the type's shield, not the world's");
    }

    @Test
    void reloadShotsRestoresToTheTypeCap() {
        Robot robot = soldier("hal");    // type shots 3, world allows 5
        robot.setShots(0);
        robot.reloadShots();
        assertEquals(3, robot.getShots());
    }

    @Test
    void positionAndDirectionCanBeSet() {
        Robot robot = soldier("hal");
        robot.setPosition(4, -2);
        robot.setDirection(Direction.EAST);
        assertEquals(4, robot.getX());
        assertEquals(-2, robot.getY());
        assertEquals(Direction.EAST, robot.getDirection());
    }
}
