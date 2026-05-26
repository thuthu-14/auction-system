package server.service;

import server.model.*;
import server.repository.SqlUserRepository;
import server.storage.*;
import common.Constants;
import util.LoggerUtil;
import util.JsonUtil; // Import thêm JsonUtil
import java.io.IOException;
import java.util.*;


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


}