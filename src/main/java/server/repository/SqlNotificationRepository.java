package server.repository;

import server.model.Notification;
import server.storage.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SqlNotificationRepository {

    public List<Notification> getNotificationsByUser(String userId) throws Exception {
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public void addNotification(Notification n) throws Exception {
        String sql = """
            INSERT INTO notifications
            (notification_id, user_id, type, title, description, created_at, time_ago, button_text, reference_id, is_read)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, n.getId());
            ps.setString(2, n.getUserId());
            ps.setString(3, n.getType());
            ps.setString(4, n.getTitle());
            ps.setString(5, n.getDescription());
            ps.setLong(6, n.getCreatedAt());
            ps.setString(7, n.getTimeAgo());
            ps.setString(8, n.getButtonText());
            ps.setString(9, n.getReferenceId());
            ps.setInt(10, n.isRead() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    public void markAllAsRead(String userId, Set<String> allowedTypes) throws Exception {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0";
        if (allowedTypes != null && !allowedTypes.isEmpty()) {
            String in = String.join(",", allowedTypes.stream().map(t -> "'" + t + "'").toList());
            sql += " AND UPPER(type) IN (" + in + ")";
        }
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        }
    }

    private Notification map(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getString("notification_id"));
        n.setUserId(rs.getString("user_id"));
        n.setType(rs.getString("type"));
        n.setTitle(rs.getString("title"));
        n.setDescription(rs.getString("description"));
        n.setCreatedAt(rs.getLong("created_at"));
        n.setTimeAgo(rs.getString("time_ago"));
        n.setButtonText(rs.getString("button_text"));
        n.setReferenceId(rs.getString("reference_id"));
        n.setRead(rs.getInt("is_read") == 1);
        return n;
    }
}