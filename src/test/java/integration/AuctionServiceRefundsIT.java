package integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.Bid;
import server.model.RegularUser;
import server.model.User;
import server.repository.*;
import server.service.AuctionService;
import server.service.BidService;
import server.storage.SchemaInitializer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuctionServiceRefundsIT {

    @BeforeEach
    void setup() {
        SchemaInitializer.init();
    }

    @AfterEach
    void teardown() throws Exception {
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
    public void finish_refund_all_losing_bids_atomicity_onTransactionFailure() throws Exception {
        // Set up real repos
        SqlUserRepository userRepo = new SqlUserRepository();
        SqlItemRepository itemRepo = new SqlItemRepository();
        SqlAuctionRepository auctionRepo = new SqlAuctionRepository();
        SqlBidRepository bidRepo = new SqlBidRepository();

        // Create seller
        RegularUser seller = new RegularUser("seller-2", "seller2", "p", "s2@x.com");
        seller.setSeller(true);
        seller.setWallet(0);
        userRepo.registerUser(seller);

        // Create item and auction
        server.model.OtherItem item = new server.model.OtherItem("IT2", "Refund Item", "", 50.0, seller.getUserId(), null);
        itemRepo.saveItem(item);

        long now = System.currentTimeMillis();
        Auction auction = new Auction("A2", item.getItemId(), seller.getUserId(), seller.getUsername(), item, 50.0, now, now + 10000);
        auctionRepo.createAuction(auction);

        // create two bidders and bids
        RegularUser u1 = new RegularUser("ru1", "ru1", "p", "ru1@x.com");
        u1.setWallet(1000);
        userRepo.registerUser(u1);
        RegularUser u2 = new RegularUser("ru2", "ru2", "p", "ru2@x.com");
        u2.setWallet(1000);
        userRepo.registerUser(u2);

        // place two bids sequentially
        BidService bidService = new BidService(bidRepo, auctionRepo, userRepo);
        Auction a1 = auctionRepo.getAuctionById("A2");
        bidService.place(u1, a1, 60.0);
        // refresh auction
        Auction a2 = auctionRepo.getAuctionById("A2");
        bidService.place(u2, a2, 80.0);

        // verify there are two bids
        List<Bid> bids = bidRepo.getBidsByAuctionId("A2");
        assertEquals(2, bids.size());

        // Now test finish refunds with a transaction repo that throws after first save
        SqlTransactionRepository realTxRepo = new SqlTransactionRepository();

        // wrapper that throws on second call
        class FailingTransactionRepo extends SqlTransactionRepository {
            private int count = 0;
            @Override
            public void saveTransaction(common.Transaction t) throws Exception {
                count++;
                if (count >= 2) throw new RuntimeException("Simulated TX failure");
                super.saveTransaction(t);
            }
        }

        SqlTransactionRepository txFailing = new FailingTransactionRepo();

        // Construct AuctionService with our failing tx repo to observe behavior
        AuctionService service = new AuctionService(auctionRepo, itemRepo, userRepo, bidRepo, txFailing);

        // Execute finish -> should attempt refunds. We expect atomic behavior; if not implemented, this test will detect partial commits.
        try {
            service.finish("A2");
            // If no exception, ensure transactions persisted for all refunds (ideally none if atomic rollback occurred)
        } catch (Exception e) {
            // Expected due to simulated failure; proceed to assertions
        }

        // After finish attempt, check transactions for each user
        var t1 = realTxRepo.getByUser("ru1");
        var t2 = realTxRepo.getByUser("ru2");

        // Assert: either both refunds present (atomic commit) OR partial (bug). We assert atomic: both or none.
        boolean both = !t1.isEmpty() && !t2.isEmpty();
        boolean none = t1.isEmpty() && t2.isEmpty();
        assertTrue(both || none, "Refunds must be atomic: either all or none. Found partial commit.");
    }
}


