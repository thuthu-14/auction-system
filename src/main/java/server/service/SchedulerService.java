package server.service;

import server.model.Auction;
import common.AuctionStatus;
import util.LoggerUtil;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class SchedulerService {

    private static ScheduledExecutorService scheduler;

    public static void startScheduler() {
        scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkExpiredAuctions();
            } catch (Exception e) {
                LoggerUtil.error("Scheduler error: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);

        LoggerUtil.info("Scheduler started");
    }


    private static void checkExpiredAuctions() throws IOException, ClassNotFoundException {
        List<Auction> auctions = AuctionService.getAllAuctions();
        long now = System.currentTimeMillis();

        for (Auction auction : auctions) {
            if ((auction.getStatus() == AuctionStatus.OPEN ||
                    auction.getStatus() == AuctionStatus.RUNNING) &&
                    now > auction.getEndTime()) {

                AuctionService.finishAuction(auction.getAuctionId());

                String winner = auction.getHighestBidderName() != null ?
                        auction.getHighestBidderName() : "Không có";

                LoggerUtil.info("Auction auto-finished: " + auction.getAuctionId() +
                        " - Winner: " + winner);
            }
        }
    }

    public static void stopScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            LoggerUtil.info("Scheduler stopped");
        }
    }
}

