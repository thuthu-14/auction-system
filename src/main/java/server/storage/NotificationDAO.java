package server.storage;

import server.model.Notification;
import util.JsonUtil;
import util.LoggerUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationDAO {
    private static final String FILE_PATH = "data/json/notifications.json";

    /**
     * Lấy danh sách thông báo cho một User
     */
    public static List<Notification> getNotificationsByUser(String username) {
        try {
            List<Notification> all = JsonUtil.loadListFromJson(FILE_PATH, Notification.class);
            if (all == null) return new ArrayList<>();

            return all.stream()
                    .filter(n -> n.getUserId().equals(username))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LoggerUtil.error("Lỗi NotificationDAO (get): " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Đánh dấu tất cả thông báo của User là đã đọc
     */
    public static void markAllAsRead(String username) {
        markAllAsRead(username, null);
    }

    public static void markAllAsRead(String username, java.util.Set<String> allowedTypes) {
        try {
            List<Notification> all = JsonUtil.loadListFromJson(FILE_PATH, Notification.class);
            if (all == null) return;

            boolean hasChanged = false;
            for (Notification n : all) {
                String type = n.getType() == null ? "" : n.getType().toUpperCase();
                boolean typeMatches = allowedTypes == null || allowedTypes.contains(type);
                if (n.getUserId().equals(username) && typeMatches && !n.isRead()) {
                    n.setRead(true);
                    hasChanged = true;
                }
            }

            if (hasChanged) {
                JsonUtil.saveToJson(FILE_PATH, all);
                LoggerUtil.info("✓ Đã cập nhật trạng thái 'Đã đọc' cho: " + username);
            }
        } catch (Exception e) {
            LoggerUtil.error("Lỗi NotificationDAO (markRead): " + e.getMessage());
        }
    }

    /**
     * Lưu một thông báo mới vào hệ thống
     */
    public static void addNotification(Notification notification) {
        try {
            // 1. Đọc danh sách hiện có
            List<Notification> all = JsonUtil.loadListFromJson(FILE_PATH, Notification.class);
            if (all == null) all = new ArrayList<>();

            // 2. Thêm thông báo mới vào đầu danh sách (để hiện lên trên cùng)
            all.add(0, notification);

            // 3. Ghi ngược lại vào file JSON
            JsonUtil.saveToJson(FILE_PATH, all);
            LoggerUtil.info("🔔 Đã lưu thông báo mới cho: " + notification.getUserId());
        } catch (Exception e) {
            LoggerUtil.error("Lỗi NotificationDAO (add): " + e.getMessage());
        }
    }
}
