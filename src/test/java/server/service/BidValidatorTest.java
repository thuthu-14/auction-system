package server.service;

import common.ItemCategory;
import org.junit.jupiter.api.Test;
import server.exception.InvalidBidException;
import server.exception.PermissionDeniedException;
import server.model.Auction;
import server.model.OtherItem;
import server.model.RegularUser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BidValidatorTest {
    private final BidValidator validator = new BidValidator();

    @Test
    void validateBidderAndAuction_acceptsValidInput() {
        RegularUser bidder = bidder(100.0);

        assertDoesNotThrow(() -> validator.validateBidderAndAuction(bidder, auction(), 120.0));
    }

    @Test
    void validateBidderAndAuction_rejectsMissingBidderOrAuction() {
        RegularUser bidder = bidder(100.0);

        assertThrows(PermissionDeniedException.class,
                () -> validator.validateBidderAndAuction(null, auction(), 120.0));
        assertThrows(PermissionDeniedException.class,
                () -> validator.validateBidderAndAuction(bidder, null, 120.0));
    }

    @Test
    void validateBidderAndAuction_rejectsInvalidAmountAndEmptyWallet() {
        RegularUser bidder = bidder(100.0);

        assertThrows(InvalidBidException.class,
                () -> validator.validateBidderAndAuction(bidder, auction(), 0.0));

        bidder.setWallet(0.0);
        assertThrows(PermissionDeniedException.class,
                () -> validator.validateBidderAndAuction(bidder, auction(), 120.0));
    }

    @Test
    void validateBidderAndAuction_rejectsAuctionWithoutValidItemCategory() {
        RegularUser bidder = bidder(100.0);
        Auction auctionWithoutItem = new Auction("A2", "IT2", "seller", "seller", null, 100.0, 30);
        OtherItem item = new OtherItem("IT1", "Item", "Desc", 100.0, "seller", List.of());
        item.setCategory(null);
        Auction auctionWithoutCategory = new Auction("A3", "IT1", "seller", "seller", item, 100.0, 30);

        assertThrows(InvalidBidException.class,
                () -> validator.validateBidderAndAuction(bidder, auctionWithoutItem, 120.0));
        assertThrows(InvalidBidException.class,
                () -> validator.validateBidderAndAuction(bidder, auctionWithoutCategory, 120.0));
    }

    private static RegularUser bidder(double wallet) {
        RegularUser bidder = new RegularUser("u1", "bidder", "password", "bidder@example.com");
        bidder.setWallet(wallet);
        return bidder;
    }

    private static Auction auction() {
        OtherItem item = new OtherItem("IT1", "Item", "Desc", 100.0, "seller", List.of());
        assertEquals(ItemCategory.OTHER, item.getCategory());
        return new Auction("A1", "IT1", "seller", "seller", item, 100.0, 30);
    }
}
