package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.server.Robot;
import za.co.wethinkcode.robots.server.RobotTypes;
import za.co.wethinkcode.robots.server.World;
import za.co.wethinkcode.robots.server.servercommands.PurgeCommand;

import static org.junit.jupiter.api.Assertions.*;

public class PurgeCommandTest {
    private World world;
    private Robot targetRobot;

    @BeforeEach
    void setUp(){
        world = new World();
        targetRobot = new Robot("HAL", world, 0,0,RobotTypes.SOLDIER);

        world.addRobot(targetRobot);
    }
    @Test
    void executeShouldKillExistingRobot(){
        PurgeCommand purgeCommand = new PurgeCommand(world, "HAL");
        boolean result = purgeCommand.execute();
        assertTrue(result);
        assertTrue(targetRobot.isDead());
    }
    @Test
    void executeShouldFailWhenRobotDoesNotExist(){
        PurgeCommand purgeCommand = new PurgeCommand(world, "Unknown");
        boolean result = purgeCommand.execute();
        assertFalse(result);
    }
    @Test
    void executeShouldFailWhenRobotNameIsEmpty(){
        PurgeCommand purgeCommand = new PurgeCommand(world,"");
        boolean result = purgeCommand.execute();
        assertFalse(result);

    }


}
