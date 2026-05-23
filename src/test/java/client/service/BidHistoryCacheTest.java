package client.service;

import org.junit.jupiter.api.Test;
import server.model.Bid;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BidHistoryCacheTest {
    @Test
    void putStoresSortedCopyAndIgnoresBlankInputs() {
        Bid later = bid("B2", "A1", "U2", 200, 20);
        Bid earlier = bid("B1", "A1", "U1", 100, 10);
        BidHistoryCache cache = new BidHistoryCache();

        cache.put("A1", List.of(later, earlier));
        cache.put("", List.of(bid("B3", "A2", "U3", 300, 30)));

        List<Bid> cached = cache.get("A1");
        assertEquals(List.of(earlier, later), cached);
        cached.clear();
        assertEquals(2, cache.get("A1").size());
        assertTrue(cache.get("").isEmpty());
    }

    @Test
    void mergeReplacesSameBidAndSortsByTime() {
        Bid original = bid("B1", "A1", "U1", 100, 20);
        Bid replacement = bid("B1", "A1", "U1", 150, 30);
        Bid earlier = bid("B0", "A1", "U0", 90, 10);
        BidHistoryCache cache = new BidHistoryCache();
        cache.put("A1", List.of(original));

        List<Bid> merged = cache.merge("A1", List.of(replacement, earlier));

        assertEquals(List.of(earlier, replacement), merged);
        assertEquals(150.0, cache.getHighestPrice("A1"));
    }

    @Test
    void buildSignatureHandlesNullsAndRoundsMoney() {
        Bid bid = bid(null, "A1", "U1", 100.6, 10);
        BidHistoryCache cache = new BidHistoryCache();

        String signature = cache.buildSignature(Arrays.asList(null, bid), 12.4);

        assertEquals("12|:A1:U1:10:101|", signature);
    }

    @Test
    void mergeHandlesNullsAndDuplicateByFallbackFields() {
        Bid existing = bid(null, "A1", "U1", 100.0, 10);
        Bid replacement = bid(null, "A1", "U1", 100.0, 10);
        Bid nullBidder = bid(null, "A1", null, 200.0, 20);
        BidHistoryCache cache = new BidHistoryCache();
        cache.put("A1", List.of(existing));

        List<Bid> merged = cache.merge("A1", List.of(replacement, nullBidder));

        assertEquals(2, merged.size());
        assertTrue(merged.contains(replacement));
        assertTrue(merged.contains(nullBidder));
        assertEquals(merged, cache.merge("A1", (Bid) null));
        assertThrows(UnsupportedOperationException.class, () -> cache.merge(null, List.of(replacement)));
    }

    @Test
    void highestPriceAndSignatureHandleEmptyAndBlankText() {
        Bid blankText = bid("", "", "", 42.2, 5);
        BidHistoryCache cache = new BidHistoryCache();
        cache.put("A1", List.of(blankText));

        assertEquals(42.2, cache.getHighestPrice("A1"), 0.001);
        assertEquals(0.0, cache.getHighestPrice("missing"), 0.001);
        assertEquals("0|:::5:42|", cache.buildSignature(List.of(blankText), 0.0));
    }

    private Bid bid(String id, String auctionId, String bidderId, double amount, long bidTime) {
        Bid bid = new Bid(id, auctionId, bidderId, bidderId, amount);
        bid.setBidTime(bidTime);
        return bid;
    }
}
