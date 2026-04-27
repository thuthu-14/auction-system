// src/main/java/server/service/InitializeDataService.java
package server.service;

import server.model.*;
import server.storage.*;
import common.Constants;
import util.LoggerUtil;
import java.io.IOException;
import java.util.*;

/**
 * InitializeDataService - Tạo dữ liệu mặc định khi server khởi động
 *
 * Logic:
 * 1. Kiểm tra users.json tồn tại + có admin không
 * 2. Nếu không → Tạo admin account
 * 3. Nếu có → Skip
 * 4. items.json, auctions.json, bids.json sẽ trống []
 * 5. User tạo data sau (tự tạo auctions, items, bids)
 */
public class InitializeDataService {

    /**
     * Khởi tạo tất cả dữ liệu mặc định
     */
    public static void initializeDefaultData() throws IOException, ClassNotFoundException {
        LoggerUtil.info("📊 Initializing default data...");

        // 1. Tạo admin account nếu chưa tồn tại
        initializeAdminAccount();

        // 2. Kiểm tra items.json, auctions.json, bids.json tồn tại không
        // Nếu không → Tạo files trống
        initializeEmptyDataFiles();

        LoggerUtil.info("✓ Default data initialized successfully");
    }

    /**
     * Tạo admin account mặc định
     *
     * Logic:
     * 1. Load users từ JSON
     * 2. Kiểm tra "admin" username tồn tại không
     * 3. Nếu không → Tạo admin, lưu JSON + Serialized
     * 4. Nếu có → Skip, display info
     */
    private static void initializeAdminAccount() throws IOException, ClassNotFoundException {
        List<User> users = UserDAO.getAllUsers();

        // Kiểm tra admin account đã tồn tại
        boolean adminExists = false;
        for (User user : users) {
            if (user.getUsername().equals("admin") && user instanceof Admin) {
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

            // Lưu vào users.json + users.dat
            UserDAO.registerUser(admin);

            // Hiển thị thông tin
            System.out.println();
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║   ✅ ADMIN ACCOUNT CREATED              ║");
            System.out.println("╠════════════════════════════════════════╣");
            System.out.println("║ 👤 Username: admin                     ║");
            System.out.println("║ 🔐 Password: admin123                  ║");
            System.out.println("║ 💼 Role: ADMIN                         ║");
            System.out.println("║ 💳 Wallet: $1,000,000.00               ║");
            System.out.println("╠════════════════════════════════════════╣");
            System.out.println("║ 📝 Note:                               ║");
            System.out.println("║ - Only this admin account is created   ║");
            System.out.println("║ - Other users must register via client ║");
            System.out.println("║ - Items, Auctions, Bids created by UI  ║");
            System.out.println("╚════════════════════════════════════════╝");
            System.out.println();

            LoggerUtil.info("✓ Admin account created and saved to JSON + Serialized");
        }
    }

    /**
     * ← THÊM: Kiểm tra + tạo empty data files nếu chưa tồn tại
     */
    private static void initializeEmptyDataFiles() throws IOException {
        // items.json - trống
        if (!util.JsonUtil.fileExists(server.storage.DataManager.JSON_ITEMS)) {
            util.JsonUtil.createFileIfNotExists(server.storage.DataManager.JSON_ITEMS);
            LoggerUtil.info("✓ Created empty items.json");
        }

        // auctions.json - trống
        if (!util.JsonUtil.fileExists(server.storage.DataManager.JSON_AUCTIONS)) {
            util.JsonUtil.createFileIfNotExists(server.storage.DataManager.JSON_AUCTIONS);
            LoggerUtil.info("✓ Created empty auctions.json");
        }

        // bids.json - trống
        if (!util.JsonUtil.fileExists(server.storage.DataManager.JSON_BIDS)) {
            util.JsonUtil.createFileIfNotExists(server.storage.DataManager.JSON_BIDS);
            LoggerUtil.info("✓ Created empty bids.json");
        }

        // items.dat - trống
        if (!util.SerializationUtil.fileExists(server.storage.DataManager.DAT_ITEMS)) {
            util.SerializationUtil.createFileIfNotExists(server.storage.DataManager.DAT_ITEMS);
            LoggerUtil.info("✓ Created empty items.dat");
        }

        // auctions.dat - trống
        if (!util.SerializationUtil.fileExists(server.storage.DataManager.DAT_AUCTIONS)) {
            util.SerializationUtil.createFileIfNotExists(server.storage.DataManager.DAT_AUCTIONS);
            LoggerUtil.info("✓ Created empty auctions.dat");
        }

        // bids.dat - trống
        if (!util.SerializationUtil.fileExists(server.storage.DataManager.DAT_BIDS)) {
            util.SerializationUtil.createFileIfNotExists(server.storage.DataManager.DAT_BIDS);
            LoggerUtil.info("✓ Created empty bids.dat");
        }
    }
}
