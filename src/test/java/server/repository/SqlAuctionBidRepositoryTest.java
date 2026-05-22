package server.repository;

import common.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.Bid;
import server.model.OtherItem;
import server.storage.DatabaseManager;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlAuctionBidRepositoryTest {

    private final SqlItemRepository itemRepository = new SqlItemRepository();
    private final SqlAuctionRepository auctionRepository = new SqlAuctionRepository();
    private final SqlBidRepository bidRepository = new SqlBidRepository();

    @BeforeEach
    void resetTables() throws Exception {
        try (Connection c = DatabaseManager.getConnection();
             Statement st = c.createStatement()) {
            st.executeUpdate("DROP TABLE IF EXISTS auction_bids");
            st.executeUpdate("DROP TABLE IF EXISTS user_bids");
            st.executeUpdate("DROP TABLE IF EXISTS bids");
            st.executeUpdate("DROP TABLE IF EXISTS auctions");
            st.executeUpdate("DROP TABLE IF EXISTS item_images");
            st.executeUpdate("DROP TABLE IF EXISTS items");

            st.executeUpdate("""
                    CREATE TABLE items (
                        item_id TEXT PRIMARY KEY,
                        type TEXT,
                        name TEXT,
                        description TEXT,
                        starting_price REAL,
                        category TEXT,
                        seller_id TEXT,
                        created_at INTEGER,
                        brand TEXT,
                        warranty_period TEXT,
                        model TEXT,
                        odometer INTEGER,
                        material TEXT,
                        weight REAL,
                        creator TEXT
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE item_images (
                        image_id TEXT PRIMARY KEY,
                        item_id TEXT,
                        image_data BLOB,
                        image_size INTEGER,
                        image_type TEXT,
                        created_at INTEGER,
                        sort_index INTEGER DEFAULT 0
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE auctions (
                        auction_id TEXT PRIMARY KEY,
                        item_id TEXT,
                        seller_id TEXT,
                        seller_name TEXT,
                        current_price REAL,
                        highest_bidder_id TEXT,
                        highest_bidder_name TEXT,
                        status TEXT,
                        start_time INTEGER,
                        end_time INTEGER,
                        created_at INTEGER,
                        reserve_price REAL,
                        minimum_bid_increment REAL,
                        view_count INTEGER
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE bids (
                        bid_id TEXT PRIMARY KEY,
                        auction_id TEXT,
                        bidder_id TEXT,
                        bidder_name TEXT,
                        amount REAL,
                        bid_time INTEGER,
                        status TEXT
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE auction_bids (
                        auction_id TEXT,
                        bid_id TEXT,
                        PRIMARY KEY (auction_id, bid_id)
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE user_bids (
                        user_id TEXT,
                        bid_id TEXT,
                        PRIMARY KEY (user_id, bid_id)
                    )
                    """);
        }
    }

    @Test
    void auctionRepositoryCreatesUpdatesQueriesAndDeletesAuctionWithBidIds() throws Exception {
        OtherItem item = item("ITEM-A1", "Phone");
        itemRepository.saveItem(item);
        Auction auction = auction("A1", item.getItemId(), "seller-1", AuctionStatus.OPEN, 100.0);
        auction.setHighestBidderId("bidder-1");
        auction.setHighestBidderName("Bidder One");
        auction.setReservePrice(150.0);
        auction.setMinimumBidIncrement(5.0);
        auction.setViewCount(7);
        auction.setBidIds(List.of("B1", "B2"));

        auctionRepository.createAuction(auction);

        Auction loaded = auctionRepository.getAuctionById("A1");
        assertNotNull(loaded);
        assertEquals("A1", loaded.getAuctionId());
        assertEquals("Phone", loaded.getItem().getName());
        assertEquals(AuctionStatus.OPEN, loaded.getStatus());
        assertEquals(List.of("B1", "B2"), loaded.getBidIds());
        assertEquals(1, auctionRepository.getActiveAuctions().size());
        assertEquals(List.of("A1"), auctionRepository.getAuctionsBySellerId("seller-1")
                .stream().map(Auction::getAuctionId).toList());

        auction.setCurrentPrice(175.0);
        auction.setStatus(AuctionStatus.RUNNING);
        auction.setBidIds(List.of("B3"));
        auctionRepository.saveAuction(auction);

        Auction updated = auctionRepository.getAuctionById("A1");
        assertEquals(175.0, updated.getCurrentPrice());
        assertEquals(AuctionStatus.RUNNING, updated.getStatus());
        assertEquals(List.of("B3"), updated.getBidIds());

        auctionRepository.deleteAuction("A1");

        assertNull(auctionRepository.getAuctionById("A1"));
        assertTrue(auctionRepository.getAllAuctions().isEmpty());
    }

    @Test
    void cancelledAuctionClearsHighestBidderWhenLoaded() throws Exception {
        OtherItem item = item("ITEM-A2", "Camera");
        itemRepository.saveItem(item);
        Auction auction = auction("A2", item.getItemId(), "seller-1", AuctionStatus.CANCELLED, 100.0);
        auction.setHighestBidderId("bidder-1");
        auction.setHighestBidderName("Bidder One");

        auctionRepository.createAuction(auction);

        Auction loaded = auctionRepository.getAuctionById("A2");

        assertEquals(AuctionStatus.CANCELLED, loaded.getStatus());
        assertNull(loaded.getHighestBidderId());
        assertNull(loaded.getHighestBidderName());
        assertTrue(auctionRepository.getActiveAuctions().isEmpty());
    }

    @Test
    void bidRepositorySavesReplacesAndQueriesByAuctionBidderAndLinkedUser() throws Exception {
        Bid first = bid("B1", "A1", "U1", "Alice", 100.0, 10, "ACTIVE");
        Bid second = bid("B2", "A1", "U2", "Bob", 125.0, 20, "ACTIVE");
        Bid otherAuction = bid("B3", "A2", "U1", "Alice", 90.0, 30, "CANCELLED");

        bidRepository.saveBid(first);
        bidRepository.saveBid(second);
        bidRepository.saveBid(otherAuction);

        assertEquals(3, bidRepository.getAllBids().size());
        assertEquals("Alice", bidRepository.getBidById("B1").getBidderName());
        assertEquals(List.of("B1", "B2"), bidRepository.getBidsByAuctionId("A1")
                .stream().map(Bid::getBidId).toList());
        assertEquals(List.of("B1", "B3"), bidRepository.getBidsByBidderId("U1")
                .stream().map(Bid::getBidId).toList());
        assertEquals(List.of("B1", "B3"), bidRepository.getBidsLinkedToUser("U1")
                .stream().map(Bid::getBidId).toList());

        Bid replacement = bid("B1", "A1", "U1", "Alice Updated", 150.0, 40, "WINNING");
        bidRepository.saveBid(replacement);

        Bid updated = bidRepository.getBidById("B1");
        assertEquals("Alice Updated", updated.getBidderName());
        assertEquals(150.0, updated.getAmount());
        assertEquals("WINNING", updated.getStatus());
    }

    @Test
    void bidRepositoryReturnsNullOrEmptyForMissingRowsAndSkipsNullLinks() throws Exception {
        Bid unlinked = bid("B-NULL", null, null, "No Links", 50.0, 10, "ACTIVE");

        bidRepository.saveBid(unlinked);

        assertNull(bidRepository.getBidById("missing"));
        assertTrue(bidRepository.getBidsByAuctionId("missing").isEmpty());
        assertTrue(bidRepository.getBidsByBidderId("missing").isEmpty());
        assertTrue(bidRepository.getBidsLinkedToUser("missing").isEmpty());
        assertEquals("B-NULL", bidRepository.getBidById("B-NULL").getBidId());
    }

    private OtherItem item(String itemId, String name) {
        return new OtherItem(itemId, name, "desc", 100.0, "seller-1", List.of());
    }

    private Auction auction(String auctionId, String itemId, String sellerId,
                            AuctionStatus status, double currentPrice) {
        Auction auction = new Auction();
        auction.setAuctionId(auctionId);
        auction.setItemId(itemId);
        auction.setSellerId(sellerId);
        auction.setSellerName("Seller One");
        auction.setCurrentPrice(currentPrice);
        auction.setStatus(status);
        auction.setStartTime(100);
        auction.setEndTime(200);
        auction.setReservePrice(0);
        auction.setMinimumBidIncrement(1);
        auction.setViewCount(0);
        return auction;
    }

    private Bid bid(String bidId, String auctionId, String bidderId, String bidderName,
                    double amount, long bidTime, String status) {
        Bid bid = new Bid();
        bid.setBidId(bidId);
        bid.setAuctionId(auctionId);
        bid.setBidderId(bidderId);
        bid.setBidderName(bidderName);
        bid.setAmount(amount);
        bid.setBidTime(bidTime);
        bid.setStatus(status);
        return bid;
    }
}
