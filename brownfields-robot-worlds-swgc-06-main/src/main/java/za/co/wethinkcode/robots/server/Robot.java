package za.co.wethinkcode.robots.server;

/**
 * Represents a robot with a name, position, direction, shields, shots, and status.
 */
public class Robot {
    private final String name;
    private int x;
    private int y;
    private Direction direction;
    private int shield;
    private int shots;
    private String status;
    private final World world;
    private int hits;
    private int distance;
    private RobotTypes type;
    private boolean mining;


    /**
     * Constructor to initialize a robot with the given name and starting position.
     * The robot starts with full shield and shots based on its type, and a status of NORMAL.
     *
     * @param name  The name of the robot.
     * @param world The world the robot belongs to.
     * @param x     The x-coordinate of the robot's initial position.
     * @param y     The y-coordinate of the robot's initial position.
     * @param type  The type of the robot.
     */
    public Robot(String name, World world,int x, int y, RobotTypes type) {
        this.name = name;
        this.world = world;
        this.x = x;  // randomly generated starting position
        this.y = y;
        this.type = type;
        repairShield();
        reloadShots();
        this.distance = type.getDistance();
        this.direction = Direction.NORTH;  // Start facing North
        this.status = "NORMAL";
        this.hits = 0;

    }

    /**
     * @return the robot's name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the x-coordinate of the robot.
     */
    public int getX() {
        return x;
    }

    /**
     * @return the y-coordinate of the robot.
     */
    public int getY() {
        return y;
    }

    /**
     * @return the direction the robot is currently facing.
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * @return the current shield value of the robot.
     */
    public int getShield() { return shield; }

    /**
     * @return the number of shots remaining for the robot.
     */
    public int getShots() { return shots; }

    /**
     * @return the current status of the robot e.g., NORMAL, RELOAD, DEAD.
     */
    public String getStatus() {
        return status;
    }

    /**
     * @return the distance the robot can shoot based on its type.
     */
    public int getDistance() {
        return distance;
    }

    /**
     * Sets the robot's position.
     *
     * @param x The new x-coordinate.
     * @param y The new y-coordinate.
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Sets the number of shots available to the robot.
     *
     * @param shots Number of shots.
     */
    public void setShots(int shots){this.shots = shots;}

    /**
     * Reloads the robot's shots up to the minimum of its type's capacity and the world's limit.
     */
    public void reloadShots(){this.shots = Math.min(type.getShots(), world.getShots());}

    /**
     * Repairs the robot's shield up to the minimum of its type's capacity and the world's limit.
     */
    public void repairShield(){this.shield = Math.min(type.getShield(), world.getShields());}

    /**
     * Sets the robot's status.
     *
     * @param status The new status string.
     */
    public void setStatus(String status){this.status = status;}

    /**
     * Updates the robot's direction.
     *
     * @param direction New direction to face.
     */
    public void setDirection(Direction direction){
        this.direction = direction;
    }

    /**
     * Kills the robot by marking it as DEAD, setting shield and shots to 0, and removing it from the world.
     */
    public void setDead() {
        this.status = "DEAD";
        this.shield = 0;
        this.shots = 0;
        world.removeRobot(this);
    }

    /**
     * Checks if the robot is dead.
     *
     * @return true if the robot is dead, false otherwise.
     */
    public boolean isDead() {
        return "DEAD".equalsIgnoreCase(this.status);
    }

    /**
     * @return the type (kind) of this robot.
     */
    public RobotTypes getType() {
        return type;
    }

    /**
     * Checks if the robot is currently in SetMine mode.
     * While setting a mine the robot cannot move at all.
     *
     * @return true if the robot is busy setting a mine.
     */
    public boolean isMining() {
        return "SETMINE".equalsIgnoreCase(this.status);
    }

    /**
     * Puts the robot into SetMine mode: the status reads SETMINE and the
     * shields are disabled until the mine is in the ground.
     */
    public void startMining() {
        this.mining = true;
        this.status = "SETMINE";
    }

    /**
     * Ends the shield-disabling window - the mine is in the ground.
     * The status stays SETMINE until the response has been sent, but the
     * robot is vulnerable to normal damage again.
     */
    public void stopMining() {
        this.mining = false;
    }

    /**
     * Applies a hit to the robot. Reduces shield, or kills the robot if shield is depleted.
     * While the robot is actually setting a mine its shields are disabled,
     * so any hit destroys it.
     */
    public void takeHit() {
        if (isDead()) {
            return;
        }
        hits++;
        if (mining) {
            // shields are disabled during SetMine mode - a sitting duck
            setDead();
            return;
        }
        if (shield > 0) {
            shield--;
        }
        else {
            setDead();
        }
    }

    /**
     * Applies the damage for stepping on a mine: three hits against the
     * robot's shields. A robot takes the same damage from its own mine as
     * from anyone else's.
     */
    public void takeMineHit() {
        for (int i = 0; i < 3 && !isDead(); i++) {
            takeHit();
        }
    }

    /**
     * @return the number of hits the robot has taken.
     */
    public int getHits() {
        return hits;
    }

    /**
     * Prints the current status of the robot. Useful for debugging.
     */
    public void printState() {
        System.out.println(getStatus());
    }

}
