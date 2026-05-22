package server.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:file:auction-test?mode=memory&cache=shared";
    private static Connection keepAliveConnection;

    static {
        try {
            keepAliveConnection = DriverManager.getConnection(URL);
        } catch (SQLException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}

