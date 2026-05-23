package client.logic;

import common.AuctionStatus;
import common.ItemCategory;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.OtherItem;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DashboardLogicTest {

    private final DashboardLogic logic = new DashboardLogic();

    @Test
    void endingSoonAuctionsKeepsOnlyVisibleFutureAuctionsSortedByRemainingTime() {
        Auction later = auction("A1", AuctionStatus.OPEN, ItemCategory.ELECTRONICS, 100, 3, 120_000);
        Auction sooner = auction("A2", AuctionStatus.RUNNING, ItemCategory.ART, 100, 1, 30_000);
        Auction cancelled = auction("A3", AuctionStatus.CANCELLED, ItemCategory.ART, 100, 10, 10_000);
        Auction noItem = auction("A4", AuctionStatus.OPEN, ItemCategory.ART, 100, 10, 10_000);
        noItem.setItem(null);
        Auction ended = auction("A5", AuctionStatus.OPEN, ItemCategory.ART, 100, 10, -10_000);

        List<Auction> result = logic.endingSoonAuctions(List.of(later, sooner, cancelled, noItem, ended), 10);

        assertEquals(List.of("A2", "A1"), result.stream().map(Auction::getAuctionId).toList());
    }

    @Test
    void suggestedAuctionsUsesSignalsAndExcludesRecentlyViewedAuctions() {
        Auction recent = auction("RECENT", AuctionStatus.OPEN, ItemCategory.ART, 500, 9, 90_000);
        Auction categoryMatch = auction("CAT", AuctionStatus.OPEN, ItemCategory.ELECTRONICS, 100, 1, 90_000);
        Auction popular = auction("POPULAR", AuctionStatus.OPEN, ItemCategory.FASHION, 300, 8, 90_000);

        List<Auction> result = logic.suggestedAuctions(
                List.of(recent, categoryMatch, popular),
                List.of("RECENT"),
                Map.of(ItemCategory.ELECTRONICS, 5),
                true,
                8);

        assertFalse(result.contains(recent));
        assertEquals("CAT", result.get(0).getAuctionId());
    }

    @Test
    void suggestedAuctionsFallsBackToPopularWhenNoSignals() {
        Auction cheapLowBid = auction("LOW", AuctionStatus.OPEN, ItemCategory.ART, 100, 1, 90_000);
        Auction expensiveHighBid = auction("HIGH", AuctionStatus.OPEN, ItemCategory.ART, 500, 7, 100_000);
        Auction midBid = auction("MID", AuctionStatus.OPEN, ItemCategory.ART, 300, 3, 80_000);

        List<Auction> result = logic.suggestedAuctions(
                List.of(cheapLowBid, expensiveHighBid, midBid),
                List.of("HIGH"),
                Map.of(ItemCategory.ART, 10),
                false,
                2);

        assertEquals(List.of("LOW", "MID"), result.stream().map(Auction::getAuctionId).toList());
    }

    @Test
    void scoreVisibilityBidCountAndRankHandleNulls() {
        Auction auction = auction("A1", AuctionStatus.OPEN, ItemCategory.JEWELRY, 100, 12, 90_000);

        assertTrue(logic.isVisibleAuction(auction));
        assertFalse(logic.isVisibleAuction(null));
        assertEquals(12, logic.bidCount(auction));
        assertEquals(0, logic.bidCount(null));
        assertEquals(Map.of("A", 3, "B", 2, "C", 1), logic.auctionRank(List.of("A", "B", "C")));
        assertEquals(20 * 2 + 12 * 3 + 10,
                logic.recommendationScore(auction,
                        Map.of(ItemCategory.JEWELRY, 3),
                        Map.of("A1", 2)));
        assertEquals(0, logic.recommendationScore(null, null, null));
    }

    private Auction auction(String id,
                            AuctionStatus status,
                            ItemCategory category,
                            double price,
                            int bidCount,
                            long endOffsetMillis) {
        OtherItem item = new OtherItem("ITEM-" + id, id, "desc", price, "seller", List.of());
        item.setCategory(category);
        Auction auction = new Auction();
        auction.setAuctionId(id);
        auction.setItemId(item.getItemId());
        auction.setItem(item);
        auction.setStatus(status);
        auction.setCurrentPrice(price);
        auction.setEndTime(System.currentTimeMillis() + endOffsetMillis);
        auction.setBidIds(java.util.stream.IntStream.range(0, bidCount)
                .mapToObj(i -> id + "-B" + i)
                .toList());
        return auction;
    }
}
