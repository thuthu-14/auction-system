package server.service;

import server.model.*;
import server.storage.*;
import common.Constants;
import util.LoggerUtil;
import util.JsonUtil; // Import thêm JsonUtil
import java.io.IOException;
import java.util.*;

/**
 * InitializeDataService - Tạo dữ liệu mặc định khi server khởi động
 * Đã lược bỏ hoàn toàn Serialization để dùng thuần JSON.
 */
public class InitializeDataService {

    /**
     * Khởi tạo tất cả dữ liệu mặc định
     */
    public static void initializeDefaultData() throws IOException, ClassNotFoundException {
        LoggerUtil.info("📊 Initializing default data (JSON only)...");

        // 1. Đảm bảo các file JSON tồn tại
        initializeEmptyDataFiles();

        // 2. Tạo admin account nếu chưa tồn tại
        initializeAdminAccount();

        LoggerUtil.info("✓ Default data initialized successfully");
    }

    /**
     * Tạo admin account mặc định
     */
    private static void initializeAdminAccount() throws IOException, ClassNotFoundException {
        List<User> users = UserDAO.getAllUsers();

        // Kiểm tra admin account đã tồn tại
        boolean adminExists = false;
        for (User user : users) {
            // Dùng .equals an toàn hơn
            if ("admin".equals(user.getUsername()) && user instanceof Admin) {
                adminExists = true;
                LoggerUtil.info("ℹ Admin account already exists");
                break;
            }
        }

        if (!adminExists) {
            // Tạo admin account mới
            Admin admin = new Admin("ADMIN001", "admin", "admin123", "admin@auction.com");
            admin.setWallet(Constants.ADMIN_WALLET); // $1,000,000
            admin.setActive(true);

            // Lưu vào users.json (Hàm này giờ chỉ lưu JSON theo fix trước đó)
            UserDAO.registerUser(admin);

            // Hiển thị thông tin trực quan
            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║   ✅ ADMIN ACCOUNT CREATED (JSON)      ║");
            System.out.println("╠════════════════════════════════════════╣");
            System.out.println("║ 👤 Username: admin                     ║");
            System.out.println("║ 🔐 Password: admin123                  ║");
            System.out.println("║ 💼 Role: ADMIN                         ║");
            System.out.println("║ 💳 Wallet: $1,000,000.00               ║");
            System.out.println("╚════════════════════════════════════════╝\n");

            LoggerUtil.info("✓ Admin account created and saved to JSON");
        }
    }

    /**
     * Kiểm tra + tạo empty JSON files nếu chưa tồn tại
     * Đã xóa bỏ phần tạo file .dat
     */
    private static void initializeEmptyDataFiles() throws IOException {
        String[] jsonFiles = {
                DataManager.JSON_ITEMS,
                DataManager.JSON_AUCTIONS,
                DataManager.JSON_BIDS,
                DataManager.JSON_USERS,
                DataManager.JSON_BANK_ACCOUNTS
        };

        for (String filePath : jsonFiles) {
            if (!JsonUtil.fileExists(filePath)) {
                JsonUtil.createFileIfNotExists(filePath);
                LoggerUtil.info("✓ Created empty JSON file: " + filePath);
            }
        }
    }
}