package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The mine damage matrix: how a 3-hit blast lands on every starting
 * shield value.
 */
public class TestMineDamageMatrix {

    private World world;

    @BeforeEach
    void setUp() {
        world = new World(20, 20);
    }

    private Robot withShield(int shield) {
        Robot robot = new Robot("hal", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);
        robot.repairShield();          // type cap 3, world cap 5 -> 3
        while (robot.getShield() > shield) {
            robot.takeHit();           // tune down to the wanted value
        }
        return robot;
    }

    @Test
    void fiveShieldsSurviveWithTwoLeft() {
        Robot robot = new Robot("j", world, 0, 0, RobotTypes.JUGGERNAUT);
        world.addRobot(robot);
        robot.takeMineHit();
        assertEquals(2, robot.getShield());
        assertFalse(robot.isDead());
    }

    @Test
    void threeShieldsSurviveWithZeroLeft() {
        Robot robot = withShield(3);
        robot.takeMineHit();
        assertEquals(0, robot.getShield());
        assertFalse(robot.isDead(), "the third hit lands on a shield, not the robot");
    }

    @Test
    void twoShieldsMeansDeath() {
        Robot robot = withShield(2);
        robot.takeMineHit();
        assertTrue(robot.isDead(), "hit 1 and 2 drain the shields, hit 3 kills");
    }

    @Test
    void oneShieldMeansDeath() {
        Robot robot = withShield(1);
        robot.takeMineHit();
        assertTrue(robot.isDead());
    }

    @Test
    void jurgernautTakesAMineBetterThanASniper() {
        Robot juggernaut = new Robot("j", world, 0, 0, RobotTypes.JUGGERNAUT);
        world.addRobot(juggernaut);
        Robot sniper = new Robot("s", world, 1, 0, RobotTypes.SNIPER);
        world.addRobot(sniper);

        juggernaut.takeMineHit();
        sniper.takeMineHit();

        assertEquals(2, juggernaut.getShield());
        assertTrue(sniper.isDead());
    }
}
