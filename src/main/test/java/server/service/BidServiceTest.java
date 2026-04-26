package server.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;

import server.model.*;
import server.exception.*;
import common.AuctionStatus;
import java.io.IOException;

public class BidServiceTest {

    private RegularUser bidder;
    private RegularUser seller;
    private Item item;
    private Auction auction;

    @Before
    public void setUp() {
        seller = new RegularUser("U001", "seller", "pass123", "seller@test.com");
        seller.setWallet(10000);

        bidder = new RegularUser("U002", "bidder", "pass123", "bidder@test.com");
        bidder.setWallet(5000);

        item = new Electronics("IT001", "Laptop", "Good laptop",
                500, seller.getUserId(), "Dell", "24 months");

        // Tạo auction
        auction = new Auction("AU001", "IT001", seller.getUserId(), "seller",
                item, 500, 60);
    }

    @Test
    public void testPlaceBidValid() {
        try {
            Bid bid = BidService.placeBid(bidder, auction, 600);

            assertNotNull(bid);
            assertEquals(600, auction.getCurrentPrice(), 0.01);
            assertEquals("U002", auction.getHighestBidderId());
            assertEquals("bidder", auction.getHighestBidderName());
            assertEquals(AuctionStatus.RUNNING, auction.getStatus());

            System.out.println("Test placeBidValid PASSED");
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    @Test
    public void testPlaceBidTooLow() {
        try {
            BidService.placeBid(bidder, auction, 400);
            fail("Should throw InvalidBidException");
        } catch (InvalidBidException e) {
            assertTrue(e.getMessage().contains("cao hơn"));
            System.out.println("Test placeBidTooLow PASSED");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testSellerCannotBidOnOwnAuction() {
        try {
            BidService.placeBid(seller, auction, 600);
            fail("Should throw PermissionDeniedException");
        } catch (PermissionDeniedException e) {
            assertTrue(e.getMessage().contains("không thể"));
            System.out.println("Test sellerCannotBidOnOwnAuction PASSED");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testCannotBidOnFinishedAuction() {
        auction.setStatus(AuctionStatus.FINISHED);

        try {
            BidService.placeBid(bidder, auction, 600);
            fail("Should throw AuctionClosedException");
        } catch (AuctionClosedException e) {
            assertTrue(e.getMessage().contains("kết thúc"));
            System.out.println("Test cannotBidOnFinishedAuction PASSED");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testPlaceBidEqualToCurrentPrice() {
        try {
            BidService.placeBid(bidder, auction, 500);
            fail("Should throw InvalidBidException");
        } catch (InvalidBidException e) {
            assertTrue(e.getMessage().contains("cao hơn"));
            System.out.println("Test placeBidEqualToCurrentPrice PASSED");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testMultipleBidsSequential() {
        try {
            Bid bid1 = BidService.placeBid(bidder, auction, 600);
            assertEquals(600, auction.getCurrentPrice(), 0.01);
            assertEquals("bidder", auction.getHighestBidderName());

            RegularUser bidder2 = new RegularUser("U003", "bidder2", "pass123", "bidder2@test.com");
            bidder2.setWallet(5000);

            Bid bid2 = BidService.placeBid(bidder2, auction, 700);
            assertEquals(700, auction.getCurrentPrice(), 0.01);
            assertEquals("bidder2", auction.getHighestBidderName());

            System.out.println("Test multipleBidsSequential PASSED");
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    @After
    public void tearDown() {
        bidder = null;
        seller = null;
        item = null;
        auction = null;
    }
}