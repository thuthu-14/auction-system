package server;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;

import server.concurrency.ConcurrentBidManager;

import java.util.concurrent.*;

public class ConcurrentBidTest {

    private RegularUser seller;
    private RegularUser bidder1;
    private RegularUser bidder2;
    private RegularUser bidder3;
    private Item item;
    private Auction auction;
    private ConcurrentBidManager bidManager;

    @Before
    public void setUp() {
        seller = new RegularUser("U001", "seller", "pass123", "seller@test.com");
        seller.setWallet(10000);

        bidder1 = new RegularUser("U002", "bidder1", "pass123", "bidder1@test.com");
        bidder1.setWallet(10000);

        bidder2 = new RegularUser("U003", "bidder2", "pass123", "bidder2@test.com");
        bidder2.setWallet(10000);

        bidder3 = new RegularUser("U004", "bidder3", "pass123", "bidder3@test.com");
        bidder3.setWallet(10000);

        item = new Electronics("IT001", "Laptop", "Good laptop",
                500, seller.getUserId(), "Dell", "24 months");

        auction = new Auction("AU001", "IT001", seller.getUserId(), "seller",
                item, 500, 60);

        bidManager = ConcurrentBidManager.getInstance();
    }

    @Test
    public void testMultipleConcurrentBids() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);

        executor.submit(() -> {
            try {
                bidManager.placeBidSafely(bidder1, auction, 600);
                latch.countDown();
            } catch (Exception e) {
                fail("Bidder 1 failed: " + e.getMessage());
            }
        });

        executor.submit(() -> {
            try {
                bidManager.placeBidSafely(bidder2, auction, 700);
                latch.countDown();
            } catch (Exception e) {
                fail("Bidder 2 failed: " + e.getMessage());
            }
        });

        executor.submit(() -> {
            try {
                bidManager.placeBidSafely(bidder3, auction, 800);
                latch.countDown();
            } catch (Exception e) {
                fail("Bidder 3 failed: " + e.getMessage());
            }
        });

        latch.await(5, TimeUnit.SECONDS);

        assertEquals(800, auction.getCurrentPrice(), 0.01);
        assertEquals("bidder3", auction.getHighestBidderName());
        assertEquals(3, auction.getBidIds().size());

        executor.shutdown();
        System.out.println("Test multipleConcurrentBids PASSED");
    }

    @Test
    public void testRaceConditionBidding() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        executor.submit(() -> {
            try {
                bidManager.placeBidSafely(bidder1, auction, 600);
                latch.countDown();
            } catch (Exception e) {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                bidManager.placeBidSafely(bidder2, auction, 600);
                latch.countDown();
            } catch (InvalidBidException e) {
                assertTrue(e.getMessage().contains("cao hơn"));
                latch.countDown();
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        });

        latch.await(5, TimeUnit.SECONDS);

        assertEquals(600, auction.getCurrentPrice(), 0.01);

        executor.shutdown();
        System.out.println("Test raceConditionBidding PASSED");
    }

    @After
    public void tearDown() {
        seller = null;
        bidder1 = null;
        bidder2 = null;
        bidder3 = null;
        item = null;
        auction = null;
    }
}