package client.logic;

import common.UserRole;
import server.model.User;

public final class UserManagementLogic {
    private UserManagementLogic() {
    }

    public static boolean matches(User user, String keyword) {
        if (user == null) {
            return false;
        }
        String normalizedKeyword = value(keyword).toLowerCase();
        return value(user.getUserId()).toLowerCase().contains(normalizedKeyword)
                || value(user.getUsername()).toLowerCase().contains(normalizedKeyword)
                || value(user.getEmail()).toLowerCase().contains(normalizedKeyword)
                || roleText(user.getRole()).toLowerCase().contains(normalizedKeyword)
                || statusText(user.isActive()).toLowerCase().contains(normalizedKeyword);
    }

    public static String displayUsername(User user) {
        String username = value(user != null ? user.getUsername() : null);
        String email = value(user != null ? user.getEmail() : null);
        return email.isBlank() ? username : username + " (" + email + ")";
    }

    public static String roleText(UserRole role) {
        if (role == null) {
            return "-";
        }
        return switch (role) {
            case ADMIN -> "Quản trị viên";
            case SELLER -> "Người bán";
            case BIDDER -> "Người đấu giá";
        };
    }

    public static String statusText(boolean active) {
        return active ? "Đang hoạt động" : "Đã khóa";
    }

    public static String value(String text) {
        return text == null ? "" : text;
    }
}
