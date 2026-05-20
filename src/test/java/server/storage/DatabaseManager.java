package server.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    // Use H2 in-memory with SQLite compatibility mode to accept SQLite syntax used in code
    private static final String URL = "jdbc:h2:mem:auction;DB_CLOSE_DELAY=-1;MODE=SQLite";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}

