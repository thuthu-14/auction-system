package server;

import util.LoggerUtil;

public class ServerMain {

    public static void main(String[] args) {
        try {
            LoggerUtil.info("╔════════════════════════════════════════╗");
            LoggerUtil.info("║   AUCTION SYSTEM SERVER v1.0           ║");
            LoggerUtil.info("║   Online Bidding Platform              ║");
            LoggerUtil.info("╚════════════════════════════════════════╝");

            AuctionServer server = new AuctionServer();
            server.start();

        } catch (Exception e) {
            LoggerUtil.error("FATAL ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}