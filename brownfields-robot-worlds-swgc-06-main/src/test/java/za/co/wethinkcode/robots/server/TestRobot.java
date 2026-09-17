package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestRobot {
    @Test
    void robotCreation(){
        World world = new World();
        Robot robot = new Robot("bob", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);

        Assertions.assertEquals("bob", robot.getName());
        Assertions.assertEquals(0, robot.getX());
        Assertions.assertEquals(0, robot.getY());
        Assertions.assertEquals(Direction.NORTH,robot.getDirection());
        Assertions.assertEquals(RobotTypes.SOLDIER.getShield(), robot.getShield());
        Assertions.assertEquals(RobotTypes.SOLDIER.getShots(), robot.getShots());
        Assertions.assertEquals(RobotTypes.SOLDIER.getDistance(), robot.getDistance());
        Assertions.assertEquals("NORMAL", robot.getStatus());
    }

    @Test
    void setPosition(){
        World world = new World();
        Robot robot = new Robot("bob", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);

        Assertions.assertEquals(0, robot.getX());
        Assertions.assertEquals(0, robot.getY());
        robot.setPosition(10,7);
        Assertions.assertEquals(10, robot.getX());
        Assertions.assertEquals(7, robot.getY());
    }

    @Test
    void setDead(){
        World world = new World();
        Robot robot = new Robot("bob", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);

        Assertions.assertEquals("NORMAL", robot.getStatus());
        robot.setDead();
        Assertions.assertEquals("DEAD", robot.getStatus());
    }

    @Test
    void takeHit(){
        World world = new World();
        Robot robot = new Robot("bob", world, 0, 0, RobotTypes.SNIPER);
        world.addRobot(robot);

        Assertions.assertFalse(robot.isDead());
        robot.takeHit();
        Assertions.assertFalse(robot.isDead());
        robot.takeHit();
        Assertions.assertTrue(robot.isDead());
    }
}
