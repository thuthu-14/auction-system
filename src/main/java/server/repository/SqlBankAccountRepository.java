package server.repository;

import server.storage.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class SqlBankAccountRepository {

    public void saveBankAccount(String userId, String username, String email,
                                String bankName, String accountNumber, double initialBalance) throws Exception {
        String sql = """
            INSERT OR REPLACE INTO bank_accounts
            (user_id, username, email, bank_name, account_number, initial_balance, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, username);
            ps.setString(3, email);
            ps.setString(4, bankName);
            ps.setString(5, accountNumber);
            ps.setDouble(6, initialBalance);
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public boolean hasBankAccount(String userId) throws Exception {
        String sql = "SELECT user_id FROM bank_accounts WHERE user_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    public Map<String, String> getBankAccount(String userId) throws Exception {
        String sql = "SELECT bank_name, account_number FROM bank_accounts WHERE user_id = ?";
        Map<String, String> result = new HashMap<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result.put("bankName", rs.getString("bank_name"));
                result.put("accountNumber", rs.getString("account_number"));
                return result;
            }
        }
        return null;
    }
}