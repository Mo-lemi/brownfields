package za.co.wethinkcode.robots.server.obstacles;

/**
 * Represents a pit obstacle on the map.
 * Pits can be seen through but cannot be passed through.
 */
public class PitObstacle extends Obstacle {

    /**
     * Creates a pit obstacle at the specified position.
     *
     * @param x The x-coordinate of the pit.
     * @param y The y-coordinate of the pit.
     */
    public PitObstacle(int x, int y) {
        super("pit", x, y);
    }

    /**
     * Indicates that pits are transparent.
     *
     * @return true, since pits are transparent.
     */
    @Override
    public boolean isTransparent() {
        return true;
    }

    /**
     * Indicates that pits are not passable.
     *
     * @return false, since pits block movement.
     */
    @Override
    public boolean isPassable() {
        return false;
    }
}
