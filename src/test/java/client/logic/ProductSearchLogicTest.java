package client.logic;

import common.AuctionStatus;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.OtherItem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductSearchLogicTest {
    @Test
    void matchesKeywordChecksAuctionAndItemFields() throws Exception {
        Auction auction = auction();

        assertTrue(ProductSearchLogic.matchesKeyword(auction, ""));
        assertTrue(ProductSearchLogic.matchesKeyword(auction, "clock"));
        assertTrue(ProductSearchLogic.matchesKeyword(auction, "seller"));
        assertTrue(ProductSearchLogic.matchesKeyword(auction, "other"));
        assertFalse(ProductSearchLogic.matchesKeyword(auction, "missing"));
    }

    @Test
    void visibleAuctionAllowsOnlyOpenAndRunning() throws Exception {
        Auction auction = auction();

        auction.setStatus(AuctionStatus.OPEN);
        assertTrue(ProductSearchLogic.isVisibleAuction(auction));
        auction.setStatus(AuctionStatus.RUNNING);
        assertTrue(ProductSearchLogic.isVisibleAuction(auction));
        auction.setStatus(AuctionStatus.FINISHED);
        assertFalse(ProductSearchLogic.isVisibleAuction(auction));
    }

    @Test
    void filterVisibleMatchesSortsByEndTimeThenName() {
        Auction later = auction();
        later.setAuctionId("later");
        later.setEndTime(System.currentTimeMillis() + 120_000L);
        Auction sooner = auction();
        sooner.setAuctionId("sooner");
        sooner.setEndTime(System.currentTimeMillis() + 60_000L);
        Auction finished = auction();
        finished.setStatus(AuctionStatus.FINISHED);

        List<Auction> results = ProductSearchLogic.filterVisibleMatches(List.of(later, finished, sooner), "clock");

        assertEquals(List.of("sooner", "later"), results.stream().map(Auction::getAuctionId).toList());
    }

    private static Auction auction() {
        long now = System.currentTimeMillis();
        OtherItem item = new OtherItem("IT1", "Vintage Clock", "description", 100.0, "seller-1", List.of());
        return new Auction("A1", "IT1", "seller-1", "Seller Name", item,
                100.0, now - 1_000L, now + 60_000L);
    }
}
