package za.co.wethinkcode.robots.server;

/**
 * Enum representing different types of robots, each with specific shield strength,
 * number of shots, and shooting distance.
 */
public enum RobotTypes {
    JUGGERNAUT(5,5,1),
    TANK(4,4,2),
    SOLDIER(3,3,3),
    MARKSMAN(2,2,4),
    SNIPER(1,1,5),
    INTRUDER(3,0,0);

    private int shield;
    private int shots;
    private int distance;

    /**
     * Constructor for RobotTypes enum.
     * @param shield The shield strength of the robot type.
     * @param shots The number of shots the robot can fire.
     * @param distance The shooting distance of the robot.
     */
    RobotTypes(int shield, int shots, int distance){
        this.shield = shield;
        this.shots = shots;
        this.distance = distance;
    }

    /**
     * Gets the shield strength of the robot type.
     * @return shield strength as an int.
     */
    public int getShield(){return shield;}

    /**
     * Gets the number of shots available for the robot type.
     * @return number of shots as an int.
     */
    public int getShots(){return shots;}

    /**
     * Gets the shooting distance of the robot type.
     * @return shooting distance as an int.
     */
    public int getDistance(){return distance;}

    /**
     * A type can place mines when it has no gun - a robot that can lay
     * mines cannot carry an active weapon (see the passive attack spec).
     * @return true if this type is allowed to use the mine command.
     */
    public boolean canPlaceMine(){return shots == 0;}

}
