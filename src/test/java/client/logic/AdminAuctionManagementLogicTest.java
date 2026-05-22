package client.logic;

import common.AuctionStatus;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.OtherItem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdminAuctionManagementLogicTest {
    @Test
    void mapsAuctionToDataAndMatchesFields() {
        long now = System.currentTimeMillis();
        OtherItem item = new OtherItem("IT1", "Clock", "desc", 100.0, "seller-1", List.of());
        Auction auction = new Auction("A1", "IT1", "seller-1", "Seller", item, 100.0, now, now + 60_000L);
        auction.setStatus(AuctionStatus.RUNNING);

        AdminAuctionManagementLogic.AdminAuctionData data = AdminAuctionManagementLogic.map(auction);

        assertEquals("A1", data.auctionId());
        assertEquals("Clock", data.itemName());
        assertEquals("RUNNING", data.rawStatus());
        assertTrue(AdminAuctionManagementLogic.matches(data, "seller"));
        assertFalse(AdminAuctionManagementLogic.matches(data, "missing"));
    }

    @Test
    void formattingHelpersUseFallbacks() {
        assertEquals("-", AdminAuctionManagementLogic.valueOrDash(null));
        assertEquals("-", AdminAuctionManagementLogic.valueOrDash(" "));
        assertEquals("A1", AdminAuctionManagementLogic.valueOrDash("A1"));
        assertEquals("-", AdminAuctionManagementLogic.formatDateTime(0));
        assertNotEquals("-", AdminAuctionManagementLogic.formatDateTime(System.currentTimeMillis()));
        assertNotNull(AdminAuctionManagementLogic.createdDate(System.currentTimeMillis()));
    }
}
