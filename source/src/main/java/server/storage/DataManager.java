package server.storage;

import common.Constants;
import util.JsonUtil;
import util.SerializationUtil;
import util.LoggerUtil;
import java.io.IOException;

public class DataManager {

    private static DataManager instance;

    public static final String JSON_USERS = Constants.JSON_DIR + "/users.json";
    public static final String JSON_AUCTIONS = Constants.JSON_DIR + "/auctions.json";
    public static final String JSON_BIDS = Constants.JSON_DIR + "/bids.json";
    public static final String JSON_ITEMS = Constants.JSON_DIR + "/items.json";

    public static final String DAT_USERS = Constants.SERIALIZED_DIR + "/users.dat";
    public static final String DAT_AUCTIONS = Constants.SERIALIZED_DIR + "/auctions.dat";
    public static final String DAT_BIDS = Constants.SERIALIZED_DIR + "/bids.dat";
    public static final String DAT_ITEMS = Constants.SERIALIZED_DIR + "/items.dat";

    private DataManager() {}

    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    public static void initializeDataFiles() throws IOException {
        LoggerUtil.info("Initializing data files...");

        JsonUtil.createFileIfNotExists(JSON_USERS);
        JsonUtil.createFileIfNotExists(JSON_AUCTIONS);
        JsonUtil.createFileIfNotExists(JSON_BIDS);
        JsonUtil.createFileIfNotExists(JSON_ITEMS);

        SerializationUtil.createFileIfNotExists(DAT_USERS);
        SerializationUtil.createFileIfNotExists(DAT_AUCTIONS);
        SerializationUtil.createFileIfNotExists(DAT_BIDS);
        SerializationUtil.createFileIfNotExists(DAT_ITEMS);

        LoggerUtil.info("All data files initialized");
    }
}
