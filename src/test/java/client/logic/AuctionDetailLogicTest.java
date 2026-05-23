package client.logic;

import common.AuctionStatus;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.Bid;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionDetailLogicTest {

    private final AuctionDetailLogic logic = new AuctionDetailLogic();

    @Test
    void resolvesCurrentPriceAndMinimumIncrement() {
        assertEquals(150.0, logic.resolveCurrentPrice(true, 120.0, 130.0, 150.0));
        assertEquals(130.0, logic.resolveCurrentPrice(false, 999.0, 130.0, 90.0));
        assertEquals(25.0, logic.resolveMinimumBidIncrement(25.0, 10.0));
        assertEquals(10.0, logic.resolveMinimumBidIncrement(0.0, 10.0));
        assertEquals(150.0, logic.minimumAllowedBid(100.0, 50.0));
    }

    @Test
    void parsesAndStepsBidAmounts() {
        assertEquals(123456.0, logic.parseBidAmount("123,456 VND", 100.0, 50.0));
        assertEquals(150.0, logic.parseBidAmount("bad", 100.0, 50.0));
        assertEquals(250.0, logic.nextIncreaseBid("200", 100.0, 50.0));
        assertEquals(150.0, logic.nextIncreaseBid("bad", 100.0, 50.0));
        assertEquals(150.0, logic.nextDecreaseBid("200", 100.0, 50.0));
        assertEquals(150.0, logic.nextDecreaseBid("200", 100.0, 50.0));
        assertEquals(150.0, logic.nextDecreaseBid("150", 100.0, 50.0));
        assertEquals(200.0, logic.nextDecreaseBid("200", 100.0, 100.0));
        assertEquals(150.0, logic.nextDecreaseBid("bad", 100.0, 50.0));
    }

    @Test
    void latestPriceAndApplyDecisionUseCurrentPriceFloor() {
        Bid bid100 = bid("B1", 100.0);
        Bid bid250 = bid("B2", 250.0);

        assertEquals(250.0, logic.latestPriceFromHistory(120.0, List.of(bid100, bid250)));
        assertEquals(300.0, logic.latestPriceFromHistory(300.0, List.of(bid100, bid250)));
        assertEquals(300.0, logic.latestPriceFromHistory(300.0, List.of()));
        assertTrue(logic.shouldApplyLatestBidPrice(301.0, 300.0));
        assertFalse(logic.shouldApplyLatestBidPrice(300.0, 300.0));
    }

    @Test
    void helpersHandleAuctionIdentityTextAndFormatting() {
        Auction auction = new Auction();
        auction.setAuctionId("A1");
        auction.setStatus(AuctionStatus.OPEN);

        assertTrue(logic.isCurrentAuction(auction, "A1"));
        assertFalse(logic.isCurrentAuction(auction, "A2"));
        assertFalse(logic.isCurrentAuction(null, "A1"));
        assertEquals("", logic.safeRealtimeText(null));
        assertEquals("", logic.safeRealtimeText(" "));
        assertEquals("ok", logic.safeRealtimeText("ok"));
        assertEquals("1.235 VND", logic.formatVnd(1234.56));
        assertEquals("1.235", logic.formatPlainNumber(1234.56));
    }

    private Bid bid(String id, double amount) {
        Bid bid = new Bid();
        bid.setBidId(id);
        bid.setAmount(amount);
        return bid;
    }
}
