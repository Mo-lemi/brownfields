package za.co.wethinkcode.robots.server.obstacles;

/**
 * Represents a mountain obstacle on the map.
 * Mountains cannot be seen through or passed through.
 */
public class MountainObstacle extends Obstacle {

    /**
     * Creates a mountain obstacle at the specified position.
     *
     * @param x The x-coordinate of the mountain.
     * @param y The y-coordinate of the mountain.
     */
    public MountainObstacle(int x, int y) {
        super("mountain",x, y);
    }

    /**
     * Indicates that mountains are not transparent.
     *
     * @return false, since mountains blocks vision.
     */
    @Override
    public boolean isTransparent() {
        return false;
    }

    /**
     * Indicates that mountains are not passable.
     *
     * @return false, since mountains block movement.
     */
    @Override
    public boolean isPassable() {
        return false;
    }
}
