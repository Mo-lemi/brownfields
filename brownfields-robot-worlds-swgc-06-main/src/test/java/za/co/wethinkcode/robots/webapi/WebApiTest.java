package za.co.wethinkcode.robots.webapi;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import za.co.wethinkcode.robots.server.Robot;
import za.co.wethinkcode.robots.server.RobotTypes;
import za.co.wethinkcode.robots.server.World;
import za.co.wethinkcode.robots.server.database.SqliteWorldRepository;
import za.co.wethinkcode.robots.server.obstacles.MountainObstacle;
import za.co.wethinkcode.robots.server.obstacles.PitObstacle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Web API tests: start the Javalin server on a free port, then verify the
 * HTTP status codes and JSON payloads of the world endpoints with Unirest.
 *
 * The tests are ordered: the plain GET /world assertions run first, then
 * the restore scenarios (which mutate the shared active world).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WebApiTest {

    private static RobotWorldApi api;
    private static String base;
    private static World world;
    private static SqliteWorldRepository repository;

    @BeforeAll
    static void startApi() {
        // a deterministic world: a 10x10 grid with one obstacle and one pit
        System.setProperty("obstacles", "none");
        System.setProperty("pits", "none");
        world = new World(10, 10);
        world.addObstacle(new MountainObstacle(2, 2));
        world.addPit(new PitObstacle(4, 4));

        repository = new SqliteWorldRepository();
        api = new RobotWorldApi(world, repository);
        api.start(0); // port 0: the OS picks a free port for the tests
        base = "http://localhost:" + api.port();
    }

    @AfterAll
    static void stopApi() {
        api.stop();
        System.clearProperty("obstacles");
        System.clearProperty("pits");
    }

    private Robot launchRobot(String name) {
        Robot robot = new Robot(name, world, 1, 1, RobotTypes.SOLDIER);
        world.addRobot(robot);
        return robot;
    }

    @Test
    @Order(1)
    void getCurrentWorldReturnsAllObjects() {
        launchRobot("hal");

        HttpResponse<String> response = Unirest.get(base + "/world").asString();

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getHeaders().getFirst("Content-Type"));

        JSONObject json = new JSONObject(response.getBody());
        assertEquals(10, json.getJSONObject("dimensions").getInt("width"));
        assertEquals(10, json.getJSONObject("dimensions").getInt("height"));

        JSONArray obstacles = json.getJSONArray("obstacles");
        boolean foundObstacle = false;
        for (int i = 0; i < obstacles.length(); i++) {
            JSONObject o = obstacles.getJSONObject(i);
            if (o.getInt("x") == 2 && o.getInt("y") == 2) {
                foundObstacle = true;
                assertEquals("mountain", o.getString("type"));
            }
        }
        assertTrue(foundObstacle, "the configured obstacle must be in the payload");

        JSONArray pits = json.getJSONArray("pits");
        boolean foundPit = false;
        for (int i = 0; i < pits.length(); i++) {
            JSONObject p = pits.getJSONObject(i);
            if (p.getInt("x") == 4 && p.getInt("y") == 4) {
                foundPit = true;
            }
        }
        assertTrue(foundPit, "the configured pit must be in the payload");

        JSONArray robots = json.getJSONArray("robots");
        boolean foundRobot = false;
        for (int i = 0; i < robots.length(); i++) {
            JSONObject r = robots.getJSONObject(i);
            if (r.getString("name").equals("hal")) {
                foundRobot = true;
                assertEquals(1, r.getJSONArray("position").getInt(0));
                assertEquals(1, r.getJSONArray("position").getInt(1));
                assertEquals("NORTH", r.getString("direction"));
            }
        }
        assertTrue(foundRobot, "the launched robot must be in the payload");
    }

    @Test
    @Order(2)
    void restoreWorldFromDatabaseReturnsItsObjects() {
        // arrange: save a small world under a unique name
        World saved = new World(6, 6);
        saved.addObstacle(new MountainObstacle(5, 5));
        repository.saveWorld("webapi-restore-world", saved);

        HttpResponse<String> response = Unirest.get(base + "/world/webapi-restore-world").asString();

        assertEquals(200, response.getStatus());
        JSONObject json = new JSONObject(response.getBody());
        assertEquals(6, json.getJSONObject("dimensions").getInt("width"));
        assertTrue(json.getJSONArray("obstacles").length() > 0,
                "the restored world must contain its saved obstacle");
        boolean foundSavedObstacle = false;
        for (int i = 0; i < json.getJSONArray("obstacles").length(); i++) {
            JSONObject o = json.getJSONArray("obstacles").getJSONObject(i);
            if (o.getInt("x") == 5 && o.getInt("y") == 5) {
                foundSavedObstacle = true;
            }
        }
        assertTrue(foundSavedObstacle, "the saved obstacle at (5,5) must be restored");
    }

    @Test
    @Order(3)
    void restoreReplacesTheCurrentActiveWorld() {
        World saved = new World(8, 8);
        saved.addObstacle(new MountainObstacle(6, 6));
        repository.saveWorld("webapi-restore-world-2", saved);

        Unirest.get(base + "/world/webapi-restore-world-2").asString();

        // the restore happened INTO the active world - a plain GET /world
        // now reports the restored dimensions and objects
        HttpResponse<String> response = Unirest.get(base + "/world").asString();
        JSONObject json = new JSONObject(response.getBody());
        assertEquals(8, json.getJSONObject("dimensions").getInt("width"));
        boolean foundSavedObstacle = false;
        for (int i = 0; i < json.getJSONArray("obstacles").length(); i++) {
            JSONObject o = json.getJSONArray("obstacles").getJSONObject(i);
            if (o.getInt("x") == 6 && o.getInt("y") == 6) {
                foundSavedObstacle = true;
            }
        }
        assertTrue(foundSavedObstacle, "the active world now holds the restored obstacle");
    }

    @Test
    @Order(4)
    void unknownWorldReturns404() {
        HttpResponse<String> response = Unirest.get(base + "/world/no-such-world").asString();

        assertEquals(404, response.getStatus());
    }

    @Test
    @Order(5)
    void postRobotUsesTheStandardSocketProtocolPayloads() {
        JSONObject request = new JSONObject()
                .put("robot", "web-hal")
                .put("command", "launch")
                .put("arguments", new JSONArray().put("soldier"));

        HttpResponse<String> response = Unirest.post(base + "/robot/web-hal")
                .header("Content-Type", "application/json")
                .body(request.toString())
                .asString();

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getHeaders().getFirst("Content-Type"));

        JSONObject json = new JSONObject(response.getBody());
        assertEquals("OK", json.getString("result"));
        assertTrue(json.getJSONObject("data").has("position"));
        assertTrue(json.has("state"));
        assertEquals("NORTH", json.getJSONObject("state").getString("direction"));
    }

    @Test
    @Order(6)
    void postRobotWithMalformedJsonReturnsTheStandardErrorPayload() {
        HttpResponse<String> response = Unirest.post(base + "/robot/bad-request")
                .header("Content-Type", "application/json")
                .body("this is not JSON")
                .asString();

        assertEquals(200, response.getStatus());

        JSONObject json = new JSONObject(response.getBody());
        assertEquals("ERROR", json.getString("result"));
        assertEquals("Could not parse arguments",
                json.getJSONObject("data").getString("message"));
    }

    @Test
    @Order(7)
    void robotLaunchedViaPostAppearsInTheCurrentWorld() {
        JSONObject request = new JSONObject()
                .put("robot", "world-view")
                .put("command", "launch")
                .put("arguments", new JSONArray().put("sniper"));

        HttpResponse<String> launchResponse = Unirest.post(base + "/robot/world-view")
                .header("Content-Type", "application/json")
                .body(request.toString())
                .asString();
        assertEquals(200, launchResponse.getStatus());
        assertEquals("OK", new JSONObject(launchResponse.getBody()).getString("result"));

        JSONObject worldJson = new JSONObject(Unirest.get(base + "/world").asString().getBody());
        boolean robotFound = false;
        for (int i = 0; i < worldJson.getJSONArray("robots").length(); i++) {
            if ("world-view".equals(worldJson.getJSONArray("robots")
                    .getJSONObject(i).getString("name"))) {
                robotFound = true;
                break;
            }
        }
        assertTrue(robotFound, "a robot launched through POST must appear in GET /world");
    }

    @Test
    @Order(8)
    void postRobotWithUnsupportedCommandReturnsTheStandardErrorPayload() {
        JSONObject request = new JSONObject()
                .put("robot", "hal")
                .put("command", "fly")
                .put("arguments", new JSONArray());

        HttpResponse<String> response = Unirest.post(base + "/robot/hal")
                .header("Content-Type", "application/json")
                .body(request.toString())
                .asString();

        assertEquals(200, response.getStatus());

        JSONObject json = new JSONObject(response.getBody());
        assertEquals("ERROR", json.getString("result"));
        assertTrue(json.getJSONObject("data").getString("message")
                .startsWith("Unsupported command fly"));
    }

    @Test
    @Order(9)
    void postStateReturnsTheStandardRobotStatePayload() {
        JSONObject launchRequest = new JSONObject()
                .put("robot", "state-robot")
                .put("command", "launch")
                .put("arguments", new JSONArray().put("soldier"));
        HttpResponse<String> launchResponse = Unirest.post(base + "/robot/state-robot")
                .header("Content-Type", "application/json")
                .body(launchRequest.toString())
                .asString();
        assertEquals("OK", new JSONObject(launchResponse.getBody()).getString("result"));

        JSONObject stateRequest = new JSONObject()
                .put("robot", "state-robot")
                .put("command", "state")
                .put("arguments", new JSONArray());
        HttpResponse<String> response = Unirest.post(base + "/robot/state-robot")
                .header("Content-Type", "application/json")
                .body(stateRequest.toString())
                .asString();

        assertEquals(200, response.getStatus());
        JSONObject json = new JSONObject(response.getBody());
        assertEquals("OK", json.getString("result"));
        assertTrue(json.getJSONObject("data").isEmpty());

        JSONObject state = json.getJSONObject("state");
        assertEquals(2, state.getJSONArray("position").length());
        assertEquals("NORTH", state.getString("direction"));
        assertEquals(3, state.getInt("shields"));
        assertEquals(3, state.getInt("shots"));
        assertEquals("NORMAL", state.getString("status"));
    }

    @Test
    @Order(10)
    void postCommandForUnknownRobotReturnsTheStandardErrorPayload() {
        JSONObject request = new JSONObject()
                .put("robot", "ghost")
                .put("command", "state")
                .put("arguments", new JSONArray());

        HttpResponse<String> response = Unirest.post(base + "/robot/ghost")
                .header("Content-Type", "application/json")
                .body(request.toString())
                .asString();

        assertEquals(200, response.getStatus());
        JSONObject json = new JSONObject(response.getBody());
        assertEquals("ERROR", json.getString("result"));
        assertEquals("Robot does not exist in the world",
                json.getJSONObject("data").getString("message"));
    }

}
