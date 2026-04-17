// src/main/java/server/service/AuthService.java
package server.service;

import server.model.User;
import server.model.RegularUser;
import server.storage.UserDAO;
import server.exception.AuthenticationException;
import util.LoggerUtil;
import util.ValidationUtil;
import java.io.IOException;

/**
 * AuthService - Xử lý authentication
 */
public class AuthService {

    /**
     * Login user
     */
    public static User login(String username, String password)
            throws AuthenticationException, IOException, ClassNotFoundException {

        // Validate input
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            LoggerUtil.error("Login failed: Empty username or password");
            throw new AuthenticationException("❌ Username và password không được rỗng!");
        }

        // Tìm user
        User user = UserDAO.getUserByUsername(username);

        if (user == null) {
            LoggerUtil.error("Login failed: User not found - " + username);
            throw new AuthenticationException("❌ Tài khoản không tồn tại!");
        }

        // Kiểm tra password
        if (!user.getPassword().equals(password)) {
            LoggerUtil.error("Login failed: Wrong password for " + username);
            throw new AuthenticationException("❌ Mật khẩu không đúng!");
        }

        // Kiểm tra tài khoản có bị khóa không
        if (!user.isActive()) {
            LoggerUtil.error("Login failed: User account banned - " + username);
            throw new AuthenticationException("❌ Tài khoản của bạn bị khóa!");
        }

        LoggerUtil.info("✓ User logged in: " + username + " (Role: " + user.getRole() + ")");
        return user;
    }

    /**
     * Register user mới - CHỈ CÓ THỂ TẠO BIDDER
     * ← SỬA: Block tạo admin
     */
    public static User register(String username, String password, String email)
            throws IOException, ClassNotFoundException, AuthenticationException {

        // Validate input
        if (!ValidationUtil.isValidUsername(username)) {
            throw new AuthenticationException("❌ Username không hợp lệ (3-50 ký tự, chỉ a-z, 0-9, _)");
        }

        if (!ValidationUtil.isValidPassword(password)) {
            throw new AuthenticationException("❌ Password phải có ít nhất 6 ký tự");
        }

        if (!ValidationUtil.isValidEmail(email)) {
            throw new AuthenticationException("❌ Email không hợp lệ");
        }

        // ← THÊM: Block tạo admin account
        if (username.equalsIgnoreCase("admin")) {
            LoggerUtil.error("Registration blocked: Attempted to create admin account - " + username);
            throw new AuthenticationException("❌ Không thể tạo tài khoản admin!");
        }

        // Kiểm tra username đã tồn tại chưa
        User existingUser = UserDAO.getUserByUsername(username);
        if (existingUser != null) {
            LoggerUtil.error("Registration failed: Username already exists - " + username);
            throw new AuthenticationException("❌ Username đã tồn tại!");
        }

        // Tạo user mới - LUÔN LÀ BIDDER
        String userId = "U" + System.currentTimeMillis();
        RegularUser newUser = new RegularUser(userId, username, password, email);
        newUser.setWallet(0); // ← Mặc định wallet = 0, user phải nạp tiền
        newUser.setSeller(false); // ← Chỉ có thể upgrade sau

        UserDAO.registerUser(newUser);
        LoggerUtil.info("✓ User registered: " + username + " (Role: BIDDER)");

        return newUser;
    }
}