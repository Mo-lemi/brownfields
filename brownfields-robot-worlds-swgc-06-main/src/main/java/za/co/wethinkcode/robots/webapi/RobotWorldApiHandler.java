package za.co.wethinkcode.robots.webapi;

import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import za.co.wethinkcode.robots.server.World;
import za.co.wethinkcode.robots.server.database.WorldRepository;
import za.co.wethinkcode.robots.server.protocol.Request;

/**
 * Does the work behind each Web API endpoint: reads the Javalin context,
 * talks to the domain (World) and the data access layer (WorldRepository),
 * and produces the JSON response body.
 *
 * This class holds no game rules of its own - everything about how the
 * world behaves lives in the domain layer below it.
 */
public class RobotWorldApiHandler {

    private RobotWorldApiHandler() {
    }

    /**
     * GET /world - returns all objects in the current active world.
     *
     * @param ctx   The Javalin context for the HTTP GET request.
     * @param world The current active world.
     */
    public static void getCurrentWorld(Context ctx, World world) {
        ctx.result(WorldJson.of(world).toString());
    }

    /**
     * GET /world/{name} - restores the named world from the database into
     * the current active world, then returns all of its objects.
     *
     * @param ctx        The Javalin context for the HTTP GET request.
     * @param world      The current active world that receives the restored objects.
     * @param repository The data access layer used to load the saved world.
     */
    public static void getNamedWorld(Context ctx, World world, WorldRepository repository) {
        String name = ctx.pathParam("name");
        World loaded = repository.loadWorld(name);
        if (loaded == null) {
            throw new NotFoundResponse("World not found: " + name);
        }
        world.restoreState(loaded);
        ctx.result(WorldJson.of(world).toString());
    }

    /**
     * POST /robot/{name} - executes a command using the same request and
     * response protocol as the socket server.  The request body deliberately
     * carries the robot name as well: that is the standard protocol envelope
     * shared by both transports.
     *
     * @param ctx   The Javalin context containing the JSON protocol request.
     * @param world The current active world.
     */
    public static void executeRobotCommand(Context ctx, World world) {
        ctx.result(new Request().handleRequest(ctx.body(), world));
    }
}
