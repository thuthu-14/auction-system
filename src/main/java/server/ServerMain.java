package server;

import server.service.InitializeDataService;
import server.exception.ServerExceptionHandler;
import server.storage.SchemaInitializer;

import util.LoggerUtil;

public class ServerMain {

    public static void main(String[] args) {
        ServerExceptionHandler.installGlobalHandlers();
        try {
            LoggerUtil.info("╔════════════════════════════════════════╗");
            LoggerUtil.info("║   AUCTION SYSTEM SERVER v1.0           ║");
            LoggerUtil.info("║   Online Bidding Platform              ║");
            LoggerUtil.info("╚════════════════════════════════════════╝");

            // [1] Khởi tạo database schema
            SchemaInitializer.init();

            // [2] Khởi tạo dữ liệu mặc định
            InitializeDataService.initializeDefaultData();

            // [3] Khởi động server
            AuctionServer server = new AuctionServer();
            server.start();

        } catch (Exception e) {
            ServerExceptionHandler.handle("Fatal server startup error", e);
            System.exit(1);
        }
    }
}