package server.service;

import server.model.Auction;
import common.AuctionStatus;
import server.concurrency.ThreadPoolManager;
import server.exception.ServerExceptionHandler;
import util.LoggerUtil;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class SchedulerService {

    private static ScheduledFuture<?> schedulerTask;

    public static void startScheduler() {
        schedulerTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(() -> {
            try {
                checkExpiredAuctions();
            } catch (Exception e) {
                ServerExceptionHandler.handle("Scheduler check expired auctions", e);
            }
        }, 0, 1, TimeUnit.MINUTES);

        LoggerUtil.info("Scheduler started");
    }


    private static void checkExpiredAuctions() throws IOException, ClassNotFoundException {
        List<Auction> auctions = AuctionService.getAllAuctions();
        long now = System.currentTimeMillis();

        for (Auction auction : auctions) {
            auction.syncStatusWithTime();

            if ((auction.getStatus() == AuctionStatus.CLOSED ||
                    auction.getStatus() == AuctionStatus.OPEN ||
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
        if (schedulerTask != null && !schedulerTask.isCancelled()) {
            schedulerTask.cancel(true);
            LoggerUtil.info("Scheduler stopped");
        }
    }
}

