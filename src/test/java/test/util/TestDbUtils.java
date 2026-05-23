package test.util;

import server.storage.SchemaInitializer;
import server.storage.DatabaseManager;

import java.sql.Connection;

public final class
TestDbUtils {
    public static void initSchema() {
        SchemaInitializer.init();
    }

    public static void clearAll() throws Exception {
        try (Connection c = DatabaseManager.getConnection();
             var st = c.createStatement()) {
            st.executeUpdate("DELETE FROM transactions");
            st.executeUpdate("DELETE FROM bids");
            st.executeUpdate("DELETE FROM auction_bids");
            st.executeUpdate("DELETE FROM user_bids");
            st.executeUpdate("DELETE FROM auctions");
            st.executeUpdate("DELETE FROM items");
            st.executeUpdate("DELETE FROM users");
        }
    }
}

