package model;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;

import common.UserRole;
import server.model.Admin;
import server.model.RegularUser;

public class UserTest {

    private RegularUser regularUser;
    private Admin admin;

    @Before
    public void setUp() {
        regularUser = new RegularUser("U001", "john", "pass123", "john@test.com");
        admin = new Admin("ADMIN001", "admin", "admin123", "admin@test.com");
    }

    @Test
    public void testRegularUserCreation() {
        assertNotNull(regularUser);
        assertEquals("U001", regularUser.getUserId());
        assertEquals("john", regularUser.getUsername());
        assertEquals("john@test.com", regularUser.getEmail());
        assertEquals(UserRole.BIDDER, regularUser.getRole());
        assertTrue(regularUser.isActive());
        assertFalse(regularUser.isSeller());

        System.out.println("Test regularUserCreation PASSED");
    }

    @Test
    public void testAddFunds() {
        regularUser.addFunds(100);
        assertEquals(100, regularUser.getWallet(), 0.01);

        regularUser.addFunds(50);
        assertEquals(150, regularUser.getWallet(), 0.01);

        System.out.println("Test addFunds PASSED");
    }

    @Test
    public void testDeductFunds() {
        regularUser.setWallet(100);

        boolean result = regularUser.deductFunds(30);
        assertTrue(result);
        assertEquals(70, regularUser.getWallet(), 0.01);
    }

    @Test
    public void testDeductFundsInsufficient() {
        regularUser.setWallet(50);

        boolean result = regularUser.deductFunds(100);
        assertFalse(result);
        assertEquals(50, regularUser.getWallet(), 0.01);

        System.out.println("Test deductFundsInsufficient PASSED");
    }

    @Test
    public void testUpgradeToSeller() {
        assertFalse(regularUser.isSeller());

        regularUser.upgradeSeller(null,null,null,null);

        assertTrue(regularUser.isSeller());

        System.out.println("Test upgradeToSeller PASSED");
    }

    @Test
    public void testAdminCreation() {
        assertNotNull(admin);
        assertEquals("ADMIN001", admin.getUserId());
        assertEquals("admin", admin.getUsername());
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertTrue(admin.hasPermission("BAN_USER"));
        assertTrue(admin.hasPermission("DELETE_AUCTION"));

        System.out.println("Test adminCreation PASSED");
    }

    @Test
    public void testUserActiveStatus() {
        assertTrue(regularUser.isActive());

        regularUser.setActive(false);
        assertFalse(regularUser.isActive());

        regularUser.setActive(true);
        assertTrue(regularUser.isActive());

        System.out.println("Test userActiveStatus PASSED");
    }

    @Test
    public void testAddOwnedAuction() {
        regularUser.addOwnedAuction("AU001");
        regularUser.addOwnedAuction("AU002");

        assertEquals(2, regularUser.getOwnedAuctionIds().size());
        assertTrue(regularUser.getOwnedAuctionIds().contains("AU001"));

        System.out.println("Test addOwnedAuction PASSED");
    }

    @After
    public void tearDown() {
        regularUser = null;
        admin = null;
    }
}