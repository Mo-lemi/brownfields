package za.co.wethinkcode.robots.server;

import org.json.JSONArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.client.clientcommands.Command;
import za.co.wethinkcode.robots.client.clientcommands.LaunchCommand;
import za.co.wethinkcode.robots.server.obstacles.MountainObstacle;
import za.co.wethinkcode.robots.server.obstacles.Obstacle;
import za.co.wethinkcode.robots.server.protocol.Response;


public class TestWorld {

    @BeforeEach
    void setUp() {
        // Force the world size to 20x20 for these specific tests
        System.setProperty("width", "20");
        System.setProperty("height", "20");
    }

    @Test
    void worldInit(){
        World world = new World();
        Assertions.assertEquals(20, world.getHeight());
        Assertions.assertEquals(20, world.getWidth());
        Assertions.assertEquals(5, world.getShields());
        Assertions.assertEquals(5, world.getShots());
        Assertions.assertEquals(2, world.getReloadTime());
        Assertions.assertEquals(3, world.getRepairTime());
        Assertions.assertEquals(5, world.getLookDistance());
    }

    @Test
    void getRobotWithName(){
        World world = new World();
        Robot robot = new Robot("bob", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);

        Robot result = world.getARobot("bob");
        Assertions.assertEquals(robot, result);
    }

    @Test
    void getRobotWithCoords(){
        World world = new World();
        Robot robot = new Robot("bob", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);

        Robot result = world.getARobot(0,0);
        Assertions.assertEquals(robot, result);
    }

    @Test
    void invalidRobotName(){
        World world = new World();
        Robot robot1 = new Robot("bob", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot1);

        Assertions.assertTrue(world.isValidRobotName("Jeff"));
        Assertions.assertTrue(world.isValidRobotName("Jim"));
        Assertions.assertFalse(world.isValidRobotName("bob"));
    }

    @Test
    void isValidPosition(){
        World world = new World();
        Assertions.assertTrue(world.isValidPosition(0,0));

        //Add robot at (0,0)
        Robot robot1 = new Robot("bob", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot1);

        //(0,0) is now occupied, so it should be false
        Assertions.assertFalse(world.isValidPosition(0,0));

        //Coordinates within [-10, 10] are valid open spaces
        Assertions.assertTrue(world.isValidPosition(-1,0));
        Assertions.assertTrue(world.isValidPosition(0,-1));
        Assertions.assertTrue(world.isValidPosition(-1,-1));

        //Out-of-bounds coordinates (outside [-10,10]) return false
        Assertions.assertFalse(world.isValidPosition(20,0));
        Assertions.assertFalse(world.isValidPosition(0,20));

        //adding more
        Assertions.assertFalse(world.isValidPosition(11,0));
        Assertions.assertFalse(world.isValidPosition(0,11));
        Assertions.assertFalse(world.isValidPosition(-11,0));
        Assertions.assertFalse(world.isValidPosition(0,-11));
    }

    @Test
    void removeRobot(){
        World world = new World();
        Assertions.assertEquals(0, world.getRobots().size());
        Robot robot = new Robot("bob", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);
        Assertions.assertEquals(1, world.getRobots().size());
        world.removeRobot(robot);
        Assertions.assertEquals(0, world.getRobots().size());
    }

    @Test
    void checkForOpenSpaces(){
        World world = new World();
        Assertions.assertTrue(world.checkForOpenSpaces());
    }

    @Test
    void checkType(){
        World world = new World();

        String result = world.checkType(0,0);
        Assertions.assertEquals("", result);

        result = world.checkType(100, 0);
        Assertions.assertEquals("e", result);
        result = world.checkType(0, 100);
        Assertions.assertEquals("e", result);


        Robot robot = new Robot("bob", world, 0, 0, RobotTypes.SOLDIER);
        world.addRobot(robot);

        result = world.checkType(0,0);
        Assertions.assertEquals("r", result);
    }
    @Test
    void worldWithoutObstaclesFull(){
        String oldWidth = System.getProperty("width");
        String oldHeight = System.getProperty("height");

        try {
            //Set world size parameter to 2x2
            System.setProperty("width", "2");
            System.setProperty("height", "2");

            World world = new World();
            //Fill all 9 available positions
            int count = 1;
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {

                    Robot robot = new Robot("robot" + count, world, x, y, RobotTypes.SOLDIER);
                    world.addRobot(robot);
                    count++;
                }
            }




            //Confirm no open space remain
            Assertions.assertFalse(world.checkForOpenSpaces());
        } finally {
            if (oldWidth != null) System.setProperty("width",oldWidth);
            if (oldHeight != null) System.setProperty("height",oldHeight);
        }
    }
    @Test
    void worldWithObstacleFull(){
        String oldWidth = System.getProperty("width");
        String oldHeight = System.getProperty("height");

        try{
            // Set world size to 2x2(-1 to 1 along both axes = 9 total positions)
            System.setProperty("width", "2");
            System.setProperty("height","2");

            World world = new World();

            //Add non-passable obstacle at coordinate [1, 1]
            world.addObstacle(new MountainObstacle(1, 1));


            //Successfully launched 8 robots into the world
            int robotCount = 1;
            for (int x = -1; x <=1; x++) {
                for (int y = -1; y <= 1; y++) {
                    if (x == 1 && y == 1) continue; //Skip obstacle location

                    Robot robot = new Robot("robot" + robotCount, world, x, y, RobotTypes.SOLDIER);
                    world.addRobot(robot);
                    robotCount++;
                }
            }

            //Verify no open spaces remain (9 total coordinates -1 obstacle - 8 robots = 0 open)
            Assertions.assertFalse(world.checkForOpenSpaces());

            //Prepare JSONArray argument containing the robot type
            JSONArray args = new JSONArray();
            args.put("soldier");

            //Construct LaunchCommand with (robotName, args)
            LaunchCommand launchCommand = new LaunchCommand("soldier robot9", args);

            //Execute command and assert the returned error string
            String response = launchCommand.execute(world);

            //Then check for error response with message "No more space in this world"
            Assertions.assertEquals("ERROR#No more space in this world", response);
        }finally {
            //Clean up system properties
            if (oldWidth != null) System.setProperty("width",oldWidth);
            if (oldHeight != null) System.setProperty("height",oldHeight);
        }

    }

}
