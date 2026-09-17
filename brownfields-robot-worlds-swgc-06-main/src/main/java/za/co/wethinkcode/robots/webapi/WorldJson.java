package za.co.wethinkcode.robots.webapi;

import org.json.JSONArray;
import org.json.JSONObject;
import za.co.wethinkcode.robots.server.Robot;
import za.co.wethinkcode.robots.server.World;
import za.co.wethinkcode.robots.server.obstacles.Obstacle;

/**
 * Converts a World into the JSON structure served by the Web API.
 *
 * This class belongs to the Web API layer: it only READS the domain's
 * public API and produces the HTTP view of the world. The World itself
 * knows nothing about JSON or HTTP.
 */
public class WorldJson {

    private WorldJson() {
    }

    /**
     * Builds the JSON representation of the world: its dimensions and all
     * objects currently in it (obstacles, pits, mines and robots).
     *
     * @param world The world to serialise.
     * @return The JSON object for the HTTP response body.
     */
    public static JSONObject of(World world) {
        JSONObject json = new JSONObject();
        json.put("dimensions", new JSONObject()
                .put("width", world.getWidth())
                .put("height", world.getHeight()));
        json.put("obstacles", obstacles(world));
        json.put("pits", pits(world));
        json.put("mines", mines(world));
        json.put("robots", robots(world));
        return json;
    }

    private static JSONArray obstacles(World world) {
        JSONArray array = new JSONArray();
        for (Obstacle obstacle : world.getObstacles()) {
            array.put(new JSONObject()
                    .put("type", obstacle.getType())
                    .put("x", obstacle.getX())
                    .put("y", obstacle.getY()));
        }
        return array;
    }

    private static JSONArray pits(World world) {
        JSONArray array = new JSONArray();
        for (za.co.wethinkcode.robots.server.obstacles.PitObstacle pit : world.getPits()) {
            array.put(new JSONObject()
                    .put("x", pit.getX())
                    .put("y", pit.getY()));
        }
        return array;
    }

    private static JSONArray mines(World world) {
        JSONArray array = new JSONArray();
        for (int[] mine : world.getMines()) {
            array.put(new JSONObject()
                    .put("x", mine[0])
                    .put("y", mine[1]));
        }
        return array;
    }

    private static JSONArray robots(World world) {
        JSONArray array = new JSONArray();
        for (Robot robot : world.getRobots()) {
            array.put(new JSONObject()
                    .put("name", robot.getName())
                    .put("position", new JSONArray()
                            .put(robot.getX())
                            .put(robot.getY()))
                    .put("direction", robot.getDirection().name())
                    .put("shields", robot.getShield())
                    .put("shots", robot.getShots())
                    .put("status", robot.getStatus()));
        }
        return array;
    }
}
