// a class to handle connecting to the robotworlds database and creating the worlds and obstacle tables if they don't exist.
//a class that connects to SQLite and builds our tables if they don't already exist.
package za.co.wethinkcode.robots.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Handles the SQLite database connection and schema initialization.
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:robotworlds.db";

    /**
     * Establishes a connection to the SQLite database.
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
        }

        return connection;
    }

    /**
     * Creates the required tables (worlds, obstacles) if they do not exist.
     */
    public static void initializeDatabase() {
        String createWorldsTable = """
                CREATE TABLE IF NOT EXISTS worlds (
                    name TEXT PRIMARY KEY,
                    width INTEGER NOT NULL,
                    height INTEGER NOT NULL
                );
                """;

        String createObstaclesTable = """
                CREATE TABLE IF NOT EXISTS obstacles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    world_name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    size INTEGER DEFAULT 1,
                    FOREIGN KEY (world_name) REFERENCES worlds(name) ON DELETE CASCADE
                );
                """;

        String createPitsTable = """
                CREATE TABLE IF NOT EXISTS pits (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    world_name TEXT NOT NULL,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    FOREIGN KEY (world_name) REFERENCES worlds(name) ON DELETE CASCADE
                );
                """;
        String createMinesTable = """
                CREATE TABLE IF NOT EXISTS mines (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    world_name TEXT NOT NULL,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    FOREIGN KEY (world_name) REFERENCES worlds(name) ON DELETE CASCADE
                );
                """;                

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Enable foreign keys for SQLite
            stmt.execute("PRAGMA foreign_keys = ON;");

            stmt.execute(createWorldsTable);
            stmt.execute(createObstaclesTable);
            stmt.execute(createPitsTable);
            stmt.execute(createMinesTable);
            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            throw new PersistenceException("Error initializing database", e);
        }
    }
}