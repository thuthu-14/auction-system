package client.service;

import org.junit.jupiter.api.Test;
import server.model.Bid;
import server.model.RegularUser;

import static org.junit.jupiter.api.Assertions.*;

class LocalAuctionDataServiceTest {
    private final LocalAuctionDataService service = new LocalAuctionDataService();

    @Test
    void placeholderLoadersReturnEmptyCollectionsOrNullContact() {
        assertTrue(service.loadBidsForAuction("A1").isEmpty());
        assertTrue(service.loadBidsForUser(new RegularUser("U1", "user", "pw", "u@example.com"), true).isEmpty());
        assertTrue(service.loadAuctionsById().isEmpty());
        assertNull(service.findSellerContact(null));
    }

    @Test
    void isBidFromUserIsNullSafeAndMatchesUserId() {
        RegularUser user = new RegularUser("U1", "user", "pw", "u@example.com");
        Bid bid = new Bid("B1", "A1", "U1", "user", 100.0);
        Bid otherBid = new Bid("B2", "A1", "U2", "other", 100.0);

        assertTrue(service.isBidFromUser(bid, user));
        assertFalse(service.isBidFromUser(otherBid, user));
        assertFalse(service.isBidFromUser(null, user));
        assertFalse(service.isBidFromUser(bid, null));

        user.setUserId(null);
        assertFalse(service.isBidFromUser(bid, user));
    }
}
