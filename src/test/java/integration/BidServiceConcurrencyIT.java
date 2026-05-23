package integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.OtherItem;
import server.model.RegularUser;
import server.model.User;
import server.repository.*;
import server.service.BidService;
import server.storage.SchemaInitializer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class BidServiceConcurrencyIT {

    @BeforeEach
    void setup() {
        SchemaInitializer.init();
    }

    @AfterEach
    void teardown() throws Exception {
        // nothing - H2 in-memory will be cleared between JVM runs; but clear tables for isolation
        try (var c = server.storage.DatabaseManager.getConnection();
             var st = c.createStatement()) {
            st.executeUpdate("DELETE FROM bids");
            st.executeUpdate("DELETE FROM auctions");
            st.executeUpdate("DELETE FROM items");
            st.executeUpdate("DELETE FROM users");
            st.executeUpdate("DELETE FROM transactions");
            st.executeUpdate("DELETE FROM auction_bids");
            st.executeUpdate("DELETE FROM user_bids");
        }
    }

    @Test
    public void concurrent_bids_highestBidPersistent_noLostUpdate() throws Exception {
        // prepare repositories and service
        SqlUserRepository userRepo = new SqlUserRepository();
        SqlItemRepository itemRepo = new SqlItemRepository();
        SqlAuctionRepository auctionRepo = new SqlAuctionRepository();
        SqlBidRepository bidRepo = new SqlBidRepository();
        BidService bidService = new BidService(bidRepo, auctionRepo, userRepo);

        // create seller
        RegularUser seller = new RegularUser("seller-1", "seller1", "x", "s@d.com");
        seller.setSeller(true);
        seller.setWallet(0);
        userRepo.registerUser(seller);

        // create item
        OtherItem item = new OtherItem("IT1", "Test Item", "", 100.0, seller.getUserId(), null);
        itemRepo.saveItem(item);

        // create auction
        long now = System.currentTimeMillis();
        Auction auction = new Auction("A1", item.getItemId(), seller.getUserId(), seller.getUsername(), item, 100.0, now, now + 60_000L);
        auctionRepo.createAuction(auction);

        // create bidders
        int bidders = 10;
        List<RegularUser> users = new ArrayList<>();
        for (int i = 0; i < bidders; i++) {
            RegularUser u = new RegularUser("u" + i, "user" + i, "p", "u" + i + "@x.com");
            u.setWallet(10_000);
            userRepo.registerUser(u);
            users.add(u);
        }

        // run concurrent bids
        ExecutorService ex = Executors.newFixedThreadPool(bidders);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < bidders; i++) {
            final int idx = i;
            futures.add(ex.submit(() -> {
                try {
                    start.await();
                    RegularUser bidder = users.get(idx);
                    // fetch fresh auction instance from DB
                    Auction a = auctionRepo.getAuctionById("A1");
                    double amount = 100.0 + (idx + 1) * 10.0; // increasing bids
                    bidService.place(bidder, a, amount);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        start.countDown();
        for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
        ex.shutdown();
        assertTrue(ex.awaitTermination(5, TimeUnit.SECONDS));

        // validate highest bid
        Auction finalAuction = auctionRepo.getAuctionById("A1");
        assertNotNull(finalAuction);
        assertEquals(100.0 + bidders * 10.0, finalAuction.getCurrentPrice(), 0.001);

        // validate bids persisted
        var allBids = bidRepo.getBidsByAuctionId("A1");
        assertEquals(bidders, allBids.size());

        // validate wallets decreased for each bidder
        for (int i = 0; i < bidders; i++) {
            RegularUser u = (RegularUser) userRepo.getUserById("u" + i);
            double expectedWallet = 10_000 - (100.0 + (i + 1) * 10.0);
            assertEquals(expectedWallet, u.getWallet(), 0.001);
        }
    }
}


