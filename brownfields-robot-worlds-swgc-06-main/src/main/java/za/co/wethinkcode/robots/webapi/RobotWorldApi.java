package za.co.wethinkcode.robots.webapi;

import io.javalin.Javalin;
import za.co.wethinkcode.robots.server.World;
import za.co.wethinkcode.robots.server.database.DatabaseManager;
import za.co.wethinkcode.robots.server.database.SqliteWorldRepository;
import za.co.wethinkcode.robots.server.database.WorldRepository;

/**
 * The Web API layer's server: defines the HTTP interface of RobotWorlds
 * and runs it on an embedded Javalin (servlet container).
 *
 * The server class only maps routes to handlers - all the work of reading
 * the request, talking to the domain (World) and the data access layer,
 * and building the response body lives in RobotWorldApiHandler.
 *
 * The World is handed in from above; the World never knows it is being
 * served over HTTP.
 */
public class RobotWorldApi {

    private final Javalin server;
    private final World world;
    private final WorldRepository repository;

    public RobotWorldApi(World world, WorldRepository repository) {
        this.world = world;
        this.repository = repository;
        this.server = Javalin.create(config -> config.http.defaultContentType = "application/json");

        this.server.get("/world", ctx -> RobotWorldApiHandler.getCurrentWorld(ctx, world));
        this.server.get("/world/{name}", ctx -> RobotWorldApiHandler.getNamedWorld(ctx, world, repository));
        this.server.post("/robot/{name}", ctx -> RobotWorldApiHandler.executeRobotCommand(ctx, world));

    }

    /** Starts the API on the given port. Port 0 lets the OS pick a free one. */
    public Javalin start(int port) {
        return server.start(port);
    }

    /** The port the running API is listening on. */
    public int port() {
        return server.port();
    }

    public void stop() {
        server.stop();
    }

    /**
     * Runs the Web API as its own program.
     * Port resolution: -p argument beats ROBOT_WORLD_API_PORT, which beats
     * the default 5050.
     */
//    public static void main(String[] args) {
//        DatabaseManager.initializeDatabase();
//
//        int port = Integer.parseInt(
//                System.getenv().getOrDefault("ROBOT_WORLD_API_PORT", "5050"));
//        for (int i = 0; i < args.length - 1; i++) {
//            if ("-p".equals(args[i])) {
//                port = Integer.parseInt(args[i + 1]);
//                break;
//            }
//        }
//
//        World world = new World();
//        new RobotWorldApi(world, new SqliteWorldRepository()).start(port);
//        System.out.println("RobotWorld Web API listening on port " + port);
//    }
}
