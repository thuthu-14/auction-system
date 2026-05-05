package server.storage;

import common.Constants;
import util.JsonUtil;
import util.LoggerUtil;
import java.io.IOException;

public class DataManager {

    private static DataManager instance;

    public static final String JSON_USERS = Constants.JSON_DIR + "/users.json";
    public static final String JSON_AUCTIONS = Constants.JSON_DIR + "/auctions.json";
    public static final String JSON_BIDS = Constants.JSON_DIR + "/bids.json";
    public static final String JSON_ITEMS = Constants.JSON_DIR + "/items.json";
    public static final String JSON_BANK_ACCOUNTS = Constants.JSON_DIR + "/bank_accounts.json";

    private DataManager() {}

    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    public static void initializeDataFiles() throws IOException {
        LoggerUtil.info("Initializing JSON data files...");

        // Chỉ khởi tạo các file JSON
        JsonUtil.createFileIfNotExists(JSON_USERS);
        JsonUtil.createFileIfNotExists(JSON_AUCTIONS);
        JsonUtil.createFileIfNotExists(JSON_BIDS);
        JsonUtil.createFileIfNotExists(JSON_ITEMS);
        JsonUtil.createFileIfNotExists(JSON_BANK_ACCOUNTS);

        LoggerUtil.info("All JSON data files initialized successfully");
    }
}