package server.service;

import server.model.*;
import server.repository.SqlUserRepository;
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
    public static void initializeDefaultData() {
        LoggerUtil.info("Initializing default data (SQLite)...");
        initializeAdminAccount();
        LoggerUtil.info("✓ Default data initialized successfully");
    }

    private static void initializeAdminAccount() {
        try {
            SqlUserRepository repo = new SqlUserRepository();
            User admin = repo.getUserByUsername("admin");
            if (admin != null) {
                LoggerUtil.info("ℹ Admin account already exists");
                return;
            }

            Admin newAdmin = new Admin("ADMIN001", "admin", "admin123", "admin@auction.com");
            newAdmin.setWallet(Constants.ADMIN_WALLET);
            newAdmin.setActive(true);

            repo.registerUser(newAdmin);
            LoggerUtil.info("✓ Admin account created in SQLite");

        } catch (Exception e) {
            throw new RuntimeException(e);
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