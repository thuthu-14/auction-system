package model;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;

import common.AuctionStatus;
import server.model.Auction;
import server.model.Electronics;
import server.model.Item;
import server.model.RegularUser;
import util.DateTimeUtil;

public class AuctionTest {

    private Auction auction;
    private Item item;
    private RegularUser seller;

    @Before
    public void setUp() {
        seller = new RegularUser("U001", "seller", "pass123", "seller@test.com");
        item = new Electronics("IT001", "Laptop", "Good laptop",
                500, seller.getUserId(), "Dell", "24 months");

        auction = new Auction("AU001", "IT001", seller.getUserId(), "seller",
                item, 500, 60);
    }


    @Test
    public void testAuctionCreation() {
        assertNotNull(auction);
        assertEquals("AU001", auction.getAuctionId());
        assertEquals(500, auction.getCurrentPrice(), 0.01);
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertNull(auction.getHighestBidderId());

        System.out.println("Test auctionCreation PASSED");
    }

    @Test
    public void testPlaceBid() throws Exception {
        RegularUser bidder = new RegularUser("U002", "bidder", "pass123", "bidder@test.com");

        boolean result = auction.placeBid("U002", "bidder", 600);

        assertTrue(result);
        assertEquals(600, auction.getCurrentPrice(), 0.01);
        assertEquals("U002", auction.getHighestBidderId());
        assertEquals("bidder", auction.getHighestBidderName());
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());

        System.out.println("Test placeBid PASSED");
    }

    @Test
    public void testPlaceBidTooLow() throws Exception {
        RegularUser bidder = new RegularUser("U002", "bidder", "pass123", "bidder@test.com");
        auction.placeBid("U002", "bidder", 600);

        try {
            auction.placeBid("U003", "bidder2", 600);
            fail("Should throw Exception");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("cao hơn"));
            System.out.println("Test placeBidTooLow PASSED");
        }
    }

    @Test
    public void testTimeRemainingCalculation() {
        long timeRemaining = auction.getTimeRemainingSeconds();

        assertTrue(timeRemaining > 0);
        assertTrue(timeRemaining <= 3600);

        System.out.println("Test timeRemainingCalculation PASSED");
    }

    @Test
    public void testAuctionExpiration() {
        auction.setEndTime(System.currentTimeMillis() - 1000);

        long timeRemaining = auction.getTimeRemainingSeconds();
        assertEquals(0, timeRemaining);

        System.out.println("Test auctionExpiration PASSED");
    }

    @Test
    public void testAddBidIds() {
        auction.addBidId("BID001");
        auction.addBidId("BID002");
        auction.addBidId("BID001");

        assertEquals(2, auction.getBidIds().size());
        assertTrue(auction.getBidIds().contains("BID001"));
        assertTrue(auction.getBidIds().contains("BID002"));

        System.out.println("Test addBidIds PASSED");
    }


    @Test
    public void testViewCountIncrement() {
        assertEquals(0, auction.getViewCount());

        auction.incrementViewCount();
        assertEquals(1, auction.getViewCount());

        auction.incrementViewCount();
        assertEquals(2, auction.getViewCount());

        System.out.println("Test viewCountIncrement PASSED");
    }

    @Test
    public void testAuctionStatusChange() {
        assertEquals(AuctionStatus.OPEN, auction.getStatus());

        auction.setStatus(AuctionStatus.RUNNING);
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());

        auction.setStatus(AuctionStatus.FINISHED);
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());

        System.out.println("Test auctionStatusChange PASSED");
    }

    @After
    public void tearDown() {
        auction = null;
        item = null;
        seller = null;
    }
}