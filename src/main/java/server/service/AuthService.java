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
    public static User login(String email, String password)
            throws AuthenticationException, IOException, ClassNotFoundException {

        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            throw new AuthenticationException("❌ email và password không được rỗng!");
        }

        User user = UserDAO.getUserByEmail(email);

        if (user == null) {
            throw new AuthenticationException("❌ Tài khoản không tồn tại!");
        }

        if (!user.getPassword().equals(password)) {
            throw new AuthenticationException("❌ Mật khẩu không đúng!");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("❌ Tài khoản bị khóa!");
        }

        // 🔥 FIX QUAN TRỌNG NHẤT
        if (user.getRole() == common.UserRole.ADMIN) {
            return new server.model.Admin(
                    user.getUserId(),
                    user.getUsername(),
                    user.getPassword(),
                    user.getEmail()
            );
        }

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

// Kiểm tra email đã tồn tại chưa
        User existingByEmail = UserDAO.getUserByEmail(email);
        if (existingByEmail != null) {
            LoggerUtil.error("Registration failed: Email already exists - " + email);
            throw new AuthenticationException("❌ Email đã tồn tại!");
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