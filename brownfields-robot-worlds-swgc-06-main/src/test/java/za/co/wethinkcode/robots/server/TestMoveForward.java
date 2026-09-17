package za.co.wethinkcode.robots.server;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.client.clientcommands.Command;
import za.co.wethinkcode.robots.client.clientcommands.ForwardCommand;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

    public class TestMoveForward {

        @Test
        void moveForwardAtNorthEdge() {

            // Given a world of size 1x1 with no obstacles
            World world = new World(1, 1);

            // And HAL is launched at [0,0]
            Robot hal = new Robot("HAL", world, 0, 0, RobotTypes.SNIPER);
            world.addRobot(hal);

            // When HAL moves forward by 5 steps
            JSONArray args = new JSONArray(List.of(5));
            Command forward = new ForwardCommand("HAL", args);

            String response = forward.execute(world);

            // Then the response should indicate the NORTH edge
            assertEquals("OK#At the NORTH edge", response);

            // And HAL should remain at [0,0]
            assertEquals(0, hal.getX());
            assertEquals(0, hal.getY());
        }
}
