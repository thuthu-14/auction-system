package io.auctionsystem.classes;

import java.util.List;

public class User {
    private String userId;
    private String username;
    private String password;
    private String email;
    private double wallet;
    private String role;
    private long createdAt;
    private boolean active;
    private String adminLevel;
    private List<String> permissions;

    // Getters
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public List<String> getPermissions() { return permissions; }

    // Bạn có thể thêm các getters khác nếu cần dùng
}