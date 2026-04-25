// src/main/java/server/service/UserService.java
package server.service;

import server.model.User;
import server.model.RegularUser;
import server.storage.UserDAO;
import util.LoggerUtil;
import java.io.IOException;
import java.util.*;

/**
 * UserService - Xử lý user operations
 */
public class UserService {

    /**
     * Lấy user theo ID
     */
    public static User getUserById(String userId)
            throws IOException, ClassNotFoundException {
        return UserDAO.getUserById(userId);
    }

    /**
     * Lấy user theo username
     */
    public static User getUserByUsername(String username)
            throws IOException, ClassNotFoundException {
        return UserDAO.getUserByUsername(username);
    }

    /**
     * Lấy tất cả users (chỉ Admin)
     */
    public static List<User> getAllUsers()
            throws IOException, ClassNotFoundException {
        return UserDAO.getAllUsers();
    }

    /**
     * Nạp tiền cho user
     */
    public static void addFunds(String userId, double amount)
            throws IOException, ClassNotFoundException {
        User user = UserDAO.getUserById(userId);
        if (user != null && amount > 0) {
            user.addFunds(amount);
            UserDAO.saveUser(user);
            LoggerUtil.info("✓ Funds added: $" + amount + " to user " + user.getUsername());
        }
    }

    /**
     * Khóa user (Ban)
     */
    public static void banUser(String userId)
            throws IOException, ClassNotFoundException {
        User user = UserDAO.getUserById(userId);
        if (user != null) {
            user.setActive(false);
            UserDAO.saveUser(user);
            LoggerUtil.info("✓ User banned: " + user.getUsername());
        }
    }

    /**
     * Mở khóa user (Unban)
     */
    public static void unbanUser(String userId)
            throws IOException, ClassNotFoundException {
        User user = UserDAO.getUserById(userId);
        if (user != null) {
            user.setActive(true);
            UserDAO.saveUser(user);
            LoggerUtil.info("✓ User unbanned: " + user.getUsername());
        }
    }

    /**
     * ← THÊM: Nâng cấp user lên seller
     */
    public static void upgradeSeller(String userId)
            throws IOException, ClassNotFoundException {
        User user = UserDAO.getUserById(userId);

        if (user == null) {
            throw new IOException("User not found");
        }

        if (!(user instanceof RegularUser)) {
            throw new IOException("Only RegularUser can be upgraded to Seller");
        }

        RegularUser regularUser = (RegularUser) user;

        if (regularUser.isSeller()) {
            throw new IOException("User is already a Seller");
        }

        regularUser.upgradeSeller();
        UserDAO.saveUser(regularUser);

        LoggerUtil.info("✓ User upgraded to Seller: " + user.getUsername());
    }

    /**
     * ← THÊM: Kiểm tra user có phải seller không
     */
    public static boolean isUserSeller(String userId)
            throws IOException, ClassNotFoundException {
        User user = UserDAO.getUserById(userId);

        if (user == null || !(user instanceof RegularUser)) {
            return false;
        }

        RegularUser regularUser = (RegularUser) user;
        return regularUser.isSeller();
    }
}
