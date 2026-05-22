package server.repository;

import common.Transaction;
import server.storage.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqlTransactionRepository implements TransactionRepository {

    @Override
    public void saveTransaction(Transaction t) throws Exception {
        String sql = """
            INSERT INTO transactions
            (id, user_id, type, amount, balance_after, description, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.id);
            ps.setString(2, t.userId);
            ps.setString(3, t.type);
            ps.setDouble(4, t.amount);
            ps.setDouble(5, t.balanceAfter);
            ps.setString(6, t.description);
            ps.setLong(7, t.timestamp);
            ps.executeUpdate();
        }
    }

    public List<Transaction> getByUser(String userId) throws Exception {
        String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY timestamp DESC";
        List<Transaction> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Transaction t = new Transaction();
                t.id = rs.getString("id");
                t.userId = rs.getString("user_id");
                t.type = rs.getString("type");
                t.amount = rs.getDouble("amount");
                t.balanceAfter = rs.getDouble("balance_after");
                t.description = rs.getString("description");
                t.timestamp = rs.getLong("timestamp");
                list.add(t);
            }
        }
        return list;
    }
}
