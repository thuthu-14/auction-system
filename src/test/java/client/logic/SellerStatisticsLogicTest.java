package client.logic;

import common.AuctionStatus;
import org.junit.jupiter.api.Test;
import server.model.Auction;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SellerStatisticsLogicTest {

    private final SellerStatisticsLogic logic = new SellerStatisticsLogic();

    @Test
    void summarizeCountsRevenueAverageBidSuccessRateAndRevenueTimeline() {
        long now = 1_000_000L;
        Auction successful = auction("A1", AuctionStatus.FINISHED, 500_000, 900_000, 2);
        Auction failed = auction("A2", AuctionStatus.CLOSED, 200_000, 800_000, 0);
        Auction running = auction("A3", AuctionStatus.RUNNING, 700_000, now + 100_000, 3);
        Auction cancelled = auction("A4", AuctionStatus.CANCELLED, 999_000, 700_000, 10);

        SellerStatisticsLogic.SellerStatisticsSummary summary =
                logic.summarize(List.of(successful, failed, running, cancelled), now);

        assertEquals(500_000, summary.totalRevenue());
        assertEquals(5.0 / 3.0, summary.averageBid(), 0.0001);
        assertEquals(50.0, summary.successRate());
        assertEquals(1, summary.revenueByEndTime().size());
        assertEquals(500_000, summary.revenueByEndTime().get(900_000L));
    }

    @Test
    void endedCancelledAndBidCountHelpersHandleEdgeCases() {
        long now = 1_000_000L;
        assertTrue(logic.isEnded(auction("A1", AuctionStatus.OPEN, 1, now - 1, 1), now));
        assertTrue(logic.isEnded(auction("A2", AuctionStatus.FINISHED, 1, now + 1, 1), now));
        assertFalse(logic.isEnded(auction("A3", AuctionStatus.CANCELLED, 1, now - 1, 1), now));
        assertTrue(logic.isCancelled(auction("A4", AuctionStatus.CANCELLED, 1, now, 1)));
        assertFalse(logic.isCancelled(null));
        assertEquals(0, logic.bidCount(null));
    }

    @Test
    void formattersAndTickUnitsMatchControllerDisplayRules() {
        assertEquals("0 đ", logic.formatVnd(0));
        assertEquals("999.999 đ", logic.formatVnd(999_999));
        assertEquals("1.5M đ", logic.formatVnd(1_500_000));
        assertEquals("1.7", logic.formatAverageBid(1.666));
        assertEquals("66.7%", logic.formatSuccessRate(66.666));
        assertEquals(10.0, logic.calculateTickUnit(0));
        assertEquals(20.0, logic.calculateTickUnit(120));
        assertEquals(200_000.0, logic.calculateTickUnit(1_200_000));
    }

    private Auction auction(String id, AuctionStatus status, double price, long endTime, int bidCount) {
        Auction auction = new Auction();
        auction.setAuctionId(id);
        auction.setStatus(status);
        auction.setCurrentPrice(price);
        auction.setEndTime(endTime);
        auction.setBidIds(java.util.stream.IntStream.range(0, bidCount)
                .mapToObj(i -> id + "-B" + i)
                .toList());
        return auction;
    }
}
