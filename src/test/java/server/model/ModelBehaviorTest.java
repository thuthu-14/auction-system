package server.model;

import common.AuctionStatus;
import common.ItemCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelBehaviorTest {

    @Test
    void auctionPlaceBidUpdatesHighestBidderAndRejectsInvalidStates() throws Exception {
        long now = System.currentTimeMillis();
        Auction auction = new Auction("A1", "IT1", "seller-1", "seller",
                item(), 100.0, now - 1_000L, now + 60_000L);

        assertTrue(auction.placeBid("u1", "bidder", 110.0));
        assertEquals(110.0, auction.getCurrentPrice(), 0.001);
        assertEquals("u1", auction.getHighestBidderId());
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
        assertThrows(Exception.class, () -> auction.placeBid("u2", "other", 110.0));

        long nowTest = System.currentTimeMillis();
        Auction finishedAuction = new Auction("A2", "IT2", "seller-1", "seller",
                item(), 100.0, nowTest - 60_000L, nowTest - 10_000L);
        assertThrows(Exception.class, () -> finishedAuction.placeBid("u2", "other", 120.0));
    }

    @Test
    void auctionRejectsBidsBeforeStartAndAfterEnd() {
        long now = System.currentTimeMillis();
        Auction future = new Auction("A1", "IT1", "seller-1", "seller",
                item(), 100.0, now + 60_000L, now + 120_000L);
        Auction expired = new Auction("A2", "IT1", "seller-1", "seller",
                item(), 100.0, now - 120_000L, now - 60_000L);

        assertThrows(Exception.class, () -> future.placeBid("u1", "bidder", 110.0));
        assertThrows(Exception.class, () -> expired.placeBid("u1", "bidder", 110.0));
        assertEquals(0, expired.getTimeRemainingSeconds());
    }

    @Test
    void auctionBidIdsAreDeduplicatedAndDefensivelyCopied() {
        Auction auction = new Auction("A1", "IT1", "seller-1", "seller", item(), 100.0, 10);

        auction.addBidId("B1");
        auction.addBidId("B1");
        List<String> ids = auction.getBidIds();
        ids.add("B2");

        assertEquals(List.of("B1"), auction.getBidIds());
        auction.setBidIds(List.of("B3"));
        assertEquals(List.of("B3"), auction.getBidIds());
    }

    @Test
    void userFundsAndRegularUserCollectionsAreSafe() {
        RegularUser user = new RegularUser("u1", "user", "password", "user@example.com");

        assertFalse(user.deductFunds(1.0));
        user.addFunds(100.0);
        assertTrue(user.deductFunds(40.0));
        assertEquals(60.0, user.getWallet(), 0.001);
        user.addOwnedAuction("A1");
        user.addOwnedAuction("A1");
        user.addBidId("B1");
        user.addBidId("B1");

        assertEquals(List.of("A1"), user.getOwnedAuctionIds());
        assertEquals(List.of("B1"), user.getBidIds());
        assertTrue(user.getDashboardTitle().contains("user"));
        assertFalse(user.getAvailableActions().isEmpty());

        user.upgradeSeller("Shop", "123", "Addr", null);
        assertTrue(user.isSeller());
        assertEquals("user@example.com", user.getShopEmail());
        assertTrue(user.getAvailableActions().size() > 4);
    }

    @Test
    void itemSubclassesExposeDetailedInfoAndToString() {
        List<Item> items = List.of(
                new Electronics("E1", "Phone", "Desc", 50_000.0, "seller", "Brand", "12 months", List.of()),
                new Art("A1", "Art", "Desc", 100_000.0, "seller", "Artist", "Canvas", List.of()),
                new Vehicle("V1", "Bike", "Desc", 500_000.0, "seller", "Model", 12, List.of()),
                new Fashion("F1", "Shirt", "Desc", 10_000.0, "seller", "Brand", "Cotton", List.of()),
                new Jewelry("J1", "Ring", "Desc", 25_000.0, "seller", "Gold", 2.5, List.of()),
                new OtherItem("O1", "Other", "Desc", 10.0, "seller", List.of())
        );

        for (Item item : items) {
            assertNotNull(item.getDetailedInfo());
            assertTrue(item.getDetailedInfo().contains(item.getName()));
            assertTrue(item.toString().contains(item.getName()));
            assertNotNull(item.getCategory());
        }
    }

    @Test
    void itemSettersUpdateBaseFields() {
        OtherItem item = new OtherItem();

        item.setItemId("IT1");
        item.setName("Name");
        item.setDescription("Description");
        item.setStartingPrice(10.0);
        item.setCategory(ItemCategory.OTHER);
        item.setSellerId("seller");
        item.setImages(List.of("a.jpg"));

        assertEquals("IT1", item.getItemId());
        assertEquals("Name", item.getName());
        assertEquals("Description", item.getDescription());
        assertEquals(10.0, item.getStartingPrice(), 0.001);
        assertEquals(ItemCategory.OTHER, item.getCategory());
        assertEquals("seller", item.getSellerId());
        assertEquals(List.of("a.jpg"), item.getImages());
        assertEquals(0, item.getCreatedAt());
    }

    private static OtherItem item() {
        return new OtherItem("IT1", "Item", "Description", 100.0, "seller-1", List.of());
    }
}
