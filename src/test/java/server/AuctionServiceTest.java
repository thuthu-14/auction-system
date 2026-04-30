package server;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import server.exception.PermissionDeniedException;
import common.AuctionStatus;
import server.model.Electronics;
import server.model.RegularUser;
import server.service.AuctionService;

public class AuctionServiceTest {

    private RegularUser seller;
    private Item item;

    @Before
    public void setUp() {
        seller = new RegularUser("U001", "seller", "pass123", "seller@test.com");
        seller.setWallet(10000);
        item = new Electronics("IT001", "Laptop", "Good laptop",
                500, seller.getUserId(), "Dell", "24 months");
    }

    @Test
    public void testCreateAuctionValid() {
        try {
            Auction auction = AuctionService.createAuction(seller, item, 60);

            assertNotNull(auction);
            assertEquals("Laptop", auction.getItem().getName());
            assertEquals(500, auction.getCurrentPrice(), 0.01);
            assertEquals(AuctionStatus.OPEN, auction.getStatus());
            assertEquals(seller.getUserId(), auction.getSellerId());

            System.out.println("Test createAuctionValid PASSED");
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    @Test
    public void testCreateAuctionNullSeller() {
        try {
            AuctionService.createAuction(null, item, 60);
            fail("Should throw PermissionDeniedException");
        } catch (PermissionDeniedException e) {
            assertTrue(e.getMessage().contains("Seller"));
            System.out.println("Test createAuctionNullSeller PASSED");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testCreateAuctionNullItem() {
        try {
            AuctionService.createAuction(seller, null, 60);
            fail("Should throw PermissionDeniedException");
        } catch (PermissionDeniedException e) {
            assertTrue(e.getMessage().contains("Item"));
            System.out.println("✓ Test createAuctionNullItem PASSED");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testCreateAuctionDurationTooShort() {
        try {
            AuctionService.createAuction(seller, item, 0);
            fail("Should throw PermissionDeniedException");
        } catch (PermissionDeniedException e) {
            assertTrue(e.getMessage().contains("không hợp lệ"));
            System.out.println("Test createAuctionDurationTooShort PASSED");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testCreateAuctionZeroPrice() {
        try {
            Item zeroItem = new Electronics("IT002", "Free Item", "Desc",
                    0, seller.getUserId(), "Brand", "12 months");
            AuctionService.createAuction(seller, zeroItem, 60);
            fail("Should throw PermissionDeniedException");
        } catch (PermissionDeniedException e) {
            assertTrue(e.getMessage().contains("không hợp lệ"));
            System.out.println("Test createAuctionZeroPrice PASSED");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @After
    public void tearDown() {
        seller = null;
        item = null;
    }
}
