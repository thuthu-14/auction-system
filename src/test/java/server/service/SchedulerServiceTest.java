package server.service;

import common.AuctionStatus;
import org.junit.jupiter.api.Test;
import server.model.Auction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SchedulerServiceTest {

    @Test
    public void startAndStopScheduler_usesThreadPoolManager() {
        try (var mocked = org.mockito.Mockito.mockStatic(server.concurrency.ThreadPoolManager.class)) {
            server.concurrency.ThreadPoolManager mockMgr = org.mockito.Mockito.mock(server.concurrency.ThreadPoolManager.class);
            mocked.when(server.concurrency.ThreadPoolManager::getInstance).thenReturn(mockMgr);
            var dummyFuture = org.mockito.Mockito.mock(java.util.concurrent.ScheduledFuture.class);
            org.mockito.Mockito.when(
                    mockMgr.scheduleAtFixedRate(
                            org.mockito.Mockito.any(),
                            org.mockito.Mockito.anyLong(),
                            org.mockito.Mockito.anyLong(),
                            org.mockito.Mockito.any()))
                    .thenReturn(dummyFuture);

            // start scheduler should call scheduleAtFixedRate
            SchedulerService.startScheduler();
            // stop scheduler should attempt to cancel
            SchedulerService.stopScheduler();

            org.mockito.Mockito.verify(mockMgr).scheduleAtFixedRate(
                    org.mockito.Mockito.any(),
                    org.mockito.Mockito.anyLong(),
                    org.mockito.Mockito.anyLong(),
                    org.mockito.Mockito.any());
        }
    }

    @Test
    public void checkExpiredAuctions_finishesOnlyOpenOrRunningExpiredAuctions() throws Exception {
        long now = System.currentTimeMillis();
        Auction expiredOpen = auction("A1", AuctionStatus.OPEN, now - 1_000L, null);
        Auction expiredRunning = auction("A2", AuctionStatus.RUNNING, now - 1_000L, "winner");
        Auction futureOpen = auction("A3", AuctionStatus.OPEN, now + 60_000L, null);
        Auction expiredFinished = auction("A4", AuctionStatus.FINISHED, now - 1_000L, null);

        try (var mockedAuctionService = mockStatic(AuctionService.class)) {
            mockedAuctionService.when(AuctionService::getAllAuctions)
                    .thenReturn(java.util.List.of(expiredOpen, expiredRunning, futureOpen, expiredFinished));

            var method = SchedulerService.class.getDeclaredMethod("checkExpiredAuctions");
            method.setAccessible(true);
            method.invoke(null);

            mockedAuctionService.verify(() -> AuctionService.finishAuction("A1"));
            mockedAuctionService.verify(() -> AuctionService.finishAuction("A2"));
            mockedAuctionService.verify(() -> AuctionService.finishAuction("A3"), never());
            mockedAuctionService.verify(() -> AuctionService.finishAuction("A4"), never());
        }
    }

    private static Auction auction(String id, AuctionStatus status, long endTime, String highestBidderName) {
        Auction auction = new Auction();
        auction.setAuctionId(id);
        auction.setStatus(status);
        auction.setEndTime(endTime);
        auction.setHighestBidderName(highestBidderName);
        return auction;
    }
}
