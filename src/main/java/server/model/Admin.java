package server.model;

import common.UserRole;
import java.util.*;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    private String adminLevel;
    private List<String> permissions;

    public Admin(String userId, String username, String password, String email) {
        super(userId, username, password, email, UserRole.ADMIN);
        this.adminLevel = "SUPER_ADMIN";
        this.permissions = new ArrayList<>();
        initializePermissions();
    }

    public Admin() {
        super();
        this.permissions = new ArrayList<>();
    }

    private void initializePermissions() {
        permissions.add("VIEW_ALL_USERS");
        permissions.add("BAN_USER");
        permissions.add("DELETE_AUCTION");
        permissions.add("VIEW_LOGS");
    }

    @Override
    public String getDashboardTitle() {
        return "⚙️ BẢNG ĐIỀU KHIỂN ADMIN - " + username;
    }

    @Override
    public List<String> getAvailableActions() {
        List<String> actions = new ArrayList<>();
        actions.add("Quản lý người dùng");
        actions.add("Khóa tài khoản");
        actions.add("Xem tất cả đấu giá");
        actions.add("Xóa đấu giá");
        return actions;
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public String getAdminLevel() {
        return adminLevel;
    }

    public List<String> getPermissions() {
        return new ArrayList<>(permissions);
    }

    @Override
    public String toString() {
        return "Admin{" + "userId='" + userId + '\'' + ", username='" + username + '\'' + '}';
    }
}
