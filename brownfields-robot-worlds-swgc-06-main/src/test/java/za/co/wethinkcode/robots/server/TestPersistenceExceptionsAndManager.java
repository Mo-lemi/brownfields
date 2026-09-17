package za.co.wethinkcode.robots.server;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import za.co.wethinkcode.robots.server.database.DatabaseManager;
import za.co.wethinkcode.robots.server.database.PersistenceException;

/**
 * Unit tests for PersistenceException and DatabaseManager: construction,
 * idempotent schema creation and a working connection.
 */
public class TestPersistenceExceptionsAndManager {

    @Test
    void persistenceExceptionCarriesItsMessage() {
        PersistenceException exception = new PersistenceException("boom");
        assertEquals("boom", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void persistenceExceptionCarriesItsCause() {
        IllegalStateException cause = new IllegalStateException("root");
        PersistenceException exception = new PersistenceException("wrapped", cause);
        assertEquals("wrapped", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void persistenceExceptionIsRuntime() {
        // callers are not forced to catch it - that is the design
        assertThrows(RuntimeException.class, () -> {
            throw new PersistenceException("unchecked by design");
        });
    }

    @Test
    void initializeDatabaseIsIdempotent() {
        assertDoesNotThrow(() -> {
            DatabaseManager.initializeDatabase();
            DatabaseManager.initializeDatabase();
        });
    }

    @Test
    void getConnectionOpensAWorkingConnection() throws SQLException {
        try (Connection connection = DatabaseManager.getConnection()) {
            assertNotNull(connection);
            assertTrue(connection.isValid(2), "the connection must be live");
        }
    }
}
