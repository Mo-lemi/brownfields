package za.co.wethinkcode.robots.server.obstacles;

/**
 * Base class for all obstacles in the world.
 * Provides common properties and methods for obstacle position and type.
 */
public abstract class Obstacle {
    private final int x;
    private final int y;
    private final String type;

    /**
     * Creates an obstacle at the given position with the specified type.
     *
     * @param type The type/name of the obstacle.
     * @param x    The x-coordinate of the obstacle.
     * @param y    The y-coordinate of the obstacle.
     */
    public Obstacle(String type, int x, int y){
        this.type = type;
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the x-coordinate of the obstacle.
     *
     * @return x position.
     */
    public int getX(){
        return x;
    }

    /**
     * Returns the y-coordinate of the obstacle.
     *
     * @return y position.
     */
    public int getY(){
        return y;
    }

    /**
     * Returns the type of the obstacle.
     *
     * @return Obstacle type as a string.
     */
    public String getType(){return type;}

    /**
     * Checks if the obstacle is transparent (can be seen through).
     *
     * @return true if transparent, false otherwise.
     */
    public abstract boolean isTransparent();

    /**
     * Checks if the obstacle is passable (can be moved through).
     *
     * @return true if passable, false otherwise.
     */
    public abstract boolean isPassable();

    /**
     * Returns a string representation of the obstacle's position.
     *
     * @return A string in the format: "Obstacle at (x, y)".
     */
    @Override
    public String toString(){
        return "Obstacle at (" + x + ", " + y + ")";
    }
}
