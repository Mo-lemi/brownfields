package za.co.wethinkcode.robots.webapi;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.server.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * More Web API tests: headers, unknown routes, and mine visibility in
 * the world payload.
 */
public class TestRobotWorldApiMore {

    private static RobotWorldApi api;
    private static String base;
    private static World world;

    @BeforeAll
    static void startApi() {
        System.setProperty("obstacles", "none");
        System.setProperty("pits", "none");
        world = new World(20, 20);
        api = new RobotWorldApi(world, new za.co.wethinkcode.robots.server.database.SqliteWorldRepository());
        api.start(0);
        base = "http://localhost:" + api.port();
    }

    @AfterAll
    static void stopApi() {
        api.stop();
        System.clearProperty("obstacles");
        System.clearProperty("pits");
    }

    @Test
    void theWorldEndpointAnswersWithJsonContentType() {
        HttpResponse<String> response = Unirest.get(base + "/world").asString();

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getHeaders().getFirst("Content-Type"));
    }

    @Test
    void anUnknownRouteIsA404() {
        HttpResponse<String> response = Unirest.get(base + "/robots").asString();

        assertEquals(404, response.getStatus(), "Javalin's default 404 for unmapped routes");
    }

    @Test
    void placedMinesAppearInTheWorldPayload() {
        world.placeMine(9, 9);

        HttpResponse<String> response = Unirest.get(base + "/world").asString();
        JSONObject json = new JSONObject(response.getBody());

        boolean found = false;
        for (int i = 0; i < json.getJSONArray("mines").length(); i++) {
            JSONObject mine = json.getJSONArray("mines").getJSONObject(i);
            if (mine.getInt("x") == 9 && mine.getInt("y") == 9) {
                found = true;
            }
        }
        assertTrue(found, "the placed mine must appear in GET /world");
    }

    @Test
    void theApiPortIsReadableAfterStartingOnAPortZero() {
        assertTrue(api.port() > 0, "the OS-assigned port is exposed for tests");
    }
}
