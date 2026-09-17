package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the RobotTypes catalogue: every kind's numbers and the
 * mine-capability rule (no gun means mines are allowed).
 */
public class TestRobotTypes {

    @Test
    void juggernautStats() {
        assertEquals(5, RobotTypes.JUGGERNAUT.getShield());
        assertEquals(5, RobotTypes.JUGGERNAUT.getShots());
        assertEquals(1, RobotTypes.JUGGERNAUT.getDistance());
    }

    @Test
    void tankStats() {
        assertEquals(4, RobotTypes.TANK.getShield());
        assertEquals(4, RobotTypes.TANK.getShots());
        assertEquals(2, RobotTypes.TANK.getDistance());
    }

    @Test
    void soldierStats() {
        assertEquals(3, RobotTypes.SOLDIER.getShield());
        assertEquals(3, RobotTypes.SOLDIER.getShots());
        assertEquals(3, RobotTypes.SOLDIER.getDistance());
    }

    @Test
    void marksmanStats() {
        assertEquals(2, RobotTypes.MARKSMAN.getShield());
        assertEquals(2, RobotTypes.MARKSMAN.getShots());
        assertEquals(4, RobotTypes.MARKSMAN.getDistance());
    }

    @Test
    void sniperStats() {
        assertEquals(1, RobotTypes.SNIPER.getShield());
        assertEquals(1, RobotTypes.SNIPER.getShots());
        assertEquals(5, RobotTypes.SNIPER.getDistance());
    }

    @Test
    void intruderHasNoGunButHasShields() {
        assertEquals(3, RobotTypes.INTRUDER.getShield());
        assertEquals(0, RobotTypes.INTRUDER.getShots(), "the Intruder carries no gun");
        assertEquals(0, RobotTypes.INTRUDER.getDistance());
    }

    @Test
    void onlyGunlessKindsCanPlaceMines() {
        assertFalse(RobotTypes.JUGGERNAUT.canPlaceMine());
        assertFalse(RobotTypes.TANK.canPlaceMine());
        assertFalse(RobotTypes.SOLDIER.canPlaceMine());
        assertFalse(RobotTypes.MARKSMAN.canPlaceMine());
        assertFalse(RobotTypes.SNIPER.canPlaceMine());
        assertTrue(RobotTypes.INTRUDER.canPlaceMine(),
                "the Intruder is the mine-laying kind");
    }
}
