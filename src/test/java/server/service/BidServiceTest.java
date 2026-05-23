package server.service;

import common.ItemCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;
import server.exception.PermissionDeniedException;
import server.exception.AuctionClosedException;
import server.exception.InsufficientFundsException;
import server.exception.InvalidBidException;
import server.model.Auction;
import server.model.Bid;
import server.model.OtherItem;
import server.model.RegularUser;
import server.repository.AuctionRepository;
import server.repository.BidRepository;
import server.repository.SqlTransactionRepository;
import server.repository.TransactionRepository;
import server.repository.UserRepository;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BidServiceTest {

    @Mock
    BidRepository bidRepository;

    @Mock
    AuctionRepository auctionRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    BidValidator bidValidator;

    @Mock
    BidChargeCalculator chargeCalculator;

    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void place_whenBidderRaisesOwnBid_chargesOnlyIncrementalAmount() throws Exception {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository, transactionRepository);
        RegularUser bidder = new RegularUser("u1", "bidder", "password", "u1@example.com");
        bidder.setWallet(100.0);
        Auction auction = auction("A1", 120.0);

        Bid previousUserBid = bid("B1", "A1", "u1", "bidder", 120.0, 1L);
        when(auctionRepository.getAuctionById("A1")).thenReturn(auction);
        when(bidRepository.getBidsByAuctionId("A1")).thenReturn(List.of(previousUserBid));

        try (MockedConstruction<SqlTransactionRepository> ignored =
                     mockConstruction(SqlTransactionRepository.class)) {
            Bid placed = service.place(bidder, auction, 150.0);

            assertEquals(70.0, bidder.getWallet(), 0.001);
            assertEquals(150.0, placed.getAmount(), 0.001);
            assertEquals(150.0, auction.getCurrentPrice(), 0.001);
            assertEquals("u1", auction.getHighestBidderId());
            verify(bidRepository).saveBid(placed);
            verify(auctionRepository).saveAuction(auction);
            verify(userRepository).saveUser(bidder);
        }
    }

    @Test
    void getHistory_resolvesItemId_mergesAuctionBidIds_deduplicatesAndSortsAscending() throws Exception {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository, transactionRepository);
        Auction auction = auction("A1", 100.0);
        auction.setItemId("IT1");
        auction.setBidIds(List.of("B2", "B3"));
        Bid first = bid("B1", "A1", "u1", "one", 110.0, 20L);
        Bid duplicate = bid("B2", "A1", "u2", "two", 120.0, 10L);
        Bid linkedOnly = bid("B3", "A1", "u3", "three", 130.0, 30L);

        when(auctionRepository.getAuctionById("IT1")).thenReturn(null);
        when(auctionRepository.getAllAuctions()).thenReturn(List.of(auction));
        when(auctionRepository.getAuctionById("A1")).thenReturn(auction);
        when(bidRepository.getBidsByAuctionId("A1")).thenReturn(List.of(first, duplicate));
        when(bidRepository.getBidById("B2")).thenReturn(duplicate);
        when(bidRepository.getBidById("B3")).thenReturn(linkedOnly);

        List<Bid> history = service.getHistory("IT1");

        assertEquals(List.of("B2", "B1", "B3"),
                history.stream().map(Bid::getBidId).toList());
    }

    @Test
    void getHistory_deduplicatesBidsWithoutIdsByAuctionBidderTimeAndAmount() throws Exception {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository, transactionRepository);
        Auction auction = auction("A1", 100.0);
        auction.setBidIds(List.of("linked"));
        Bid repositoryBid = bid(null, "A1", "u1", "one", 110.0, 20L);
        Bid sameLinkedBid = bid(null, "A1", "u1", "one", 110.0, 20L);
        Bid distinctLinkedBid = bid(null, "A1", "u1", "one", 115.0, 21L);

        when(auctionRepository.getAuctionById("A1")).thenReturn(auction);
        when(bidRepository.getBidsByAuctionId("A1")).thenReturn(List.of(repositoryBid));
        when(bidRepository.getBidById("linked")).thenReturn(sameLinkedBid, distinctLinkedBid);

        assertEquals(List.of(repositoryBid), service.getHistory("A1"));

        auction.setBidIds(List.of("linked", "linked2"));
        when(bidRepository.getBidById("linked2")).thenReturn(distinctLinkedBid);
        List<Bid> history = service.getHistory("A1");

        assertEquals(2, history.size());
        assertSame(repositoryBid, history.get(0));
        assertSame(distinctLinkedBid, history.get(1));
    }

    @Test
    void getUserBidList_mergesLinkedBids_deduplicatesAndSortsNewestFirst() throws Exception {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository, transactionRepository);
        Bid oldest = bid("B1", "A1", "u1", "one", 110.0, 10L);
        Bid duplicate = bid("B2", "A1", "u1", "one", 120.0, 20L);
        Bid newest = bid("B3", "A2", "u1", "one", 130.0, 30L);

        when(bidRepository.getBidsByBidderId("u1")).thenReturn(List.of(oldest, duplicate));
        when(bidRepository.getBidsLinkedToUser("u1")).thenReturn(List.of(duplicate, newest));

        List<Bid> bids = service.getUserBidList("u1");

        assertEquals(List.of("B3", "B2", "B1"),
                bids.stream().map(Bid::getBidId).toList());
    }

    @Test
    void getUserBidList_deduplicatesNullIdBidsByValueAndIgnoresNullLinkedBid() throws Exception {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository, transactionRepository);
        Bid repositoryBid = bid(null, "A1", "u1", "one", 110.0, 10L);
        Bid duplicateLinkedBid = bid(null, "A1", "u1", "one", 110.0, 10L);
        Bid newest = bid(null, "A1", "u1", "one", 120.0, 20L);
        when(bidRepository.getBidsByBidderId("u1")).thenReturn(List.of(repositoryBid));
        when(bidRepository.getBidsLinkedToUser("u1")).thenReturn(java.util.Arrays.asList(null, duplicateLinkedBid, newest));

        List<Bid> bids = service.getUserBidList("u1");

        assertEquals(List.of(newest, repositoryBid), bids);
    }

    @Test
    void placeForCurrentUser_usesLatestRegularUserAndReturnsPreviousHighestBidder() throws Exception {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository, transactionRepository);
        RegularUser staleUser = new RegularUser("u1", "stale", "password", "stale@example.com");
        staleUser.setWallet(1.0);
        RegularUser latestUser = new RegularUser("u1", "latest", "password", "latest@example.com");
        latestUser.setWallet(500.0);
        completeProfile(latestUser);
        Auction auction = auction("A1", 100.0);
        auction.setHighestBidderId("old-winner");

        when(auctionRepository.getAuctionById("A1")).thenReturn(auction);
        when(userRepository.getUserById("u1")).thenReturn(latestUser);
        when(bidRepository.getBidsByAuctionId("A1")).thenReturn(List.of());

        try (MockedConstruction<SqlTransactionRepository> ignored =
                     mockConstruction(SqlTransactionRepository.class)) {
            BidPlacementResult result = service.placeForCurrentUser(staleUser, "A1", 130.0);

            assertSame(latestUser, result.getBidder());
            assertEquals("old-winner", result.getPreviousHighestBidderId());
            assertEquals(370.0, latestUser.getWallet(), 0.001);
            assertEquals(1.0, staleUser.getWallet(), 0.001);
        }
    }

    @Test
    void placeForCurrentUser_rejectsNonRegularUser() {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository, transactionRepository);

        assertThrows(PermissionDeniedException.class,
                () -> service.placeForCurrentUser(null, "A1", 100.0));
    }

    @Test
    void place_rejectsClosedAuctionBeforePersistingAnything() throws Exception {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository, transactionRepository);
        RegularUser bidder = new RegularUser("u1", "bidder", "password", "u1@example.com");
        bidder.setWallet(500.0);
        completeProfile(bidder);
        Auction auction = auction("A1", 100.0);
        auction.setStatus(common.AuctionStatus.FINISHED);
        when(auctionRepository.getAuctionById("A1")).thenReturn(auction);

        assertThrows(AuctionClosedException.class, () -> service.place(bidder, auction, 120.0));

        verify(bidRepository, never()).saveBid(any());
        verify(auctionRepository, never()).saveAuction(any());
        verify(userRepository, never()).saveUser(any());
    }

    @Test
    void place_rejectsSellerBiddingOnOwnAuction() throws Exception {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository, transactionRepository);
        RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
        seller.setWallet(500.0);
        Auction auction = auction("A1", 100.0);
        when(auctionRepository.getAuctionById("A1")).thenReturn(auction);

        assertThrows(PermissionDeniedException.class, () -> service.place(seller, auction, 120.0));

        verify(bidRepository, never()).saveBid(any());
    }

    @Test
    void place_rejectsBidBelowCustomMinimumIncrement() throws Exception {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository, transactionRepository);
        RegularUser bidder = new RegularUser("u1", "bidder", "password", "u1@example.com");
        bidder.setWallet(500.0);
        completeProfile(bidder);
        Auction auction = auction("A1", 100.0);
        auction.setMinimumBidIncrement(25.0);
        when(auctionRepository.getAuctionById("A1")).thenReturn(auction);

        assertThrows(InvalidBidException.class, () -> service.place(bidder, auction, 120.0));

        verify(bidRepository, never()).saveBid(any());
    }

    @Test
    void place_rejectsInsufficientFundsForIncrementalCharge() throws Exception {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository, transactionRepository);
        RegularUser bidder = new RegularUser("u1", "bidder", "password", "u1@example.com");
        bidder.setWallet(10.0);
        Auction auction = auction("A1", 120.0);
        when(auctionRepository.getAuctionById("A1")).thenReturn(auction);
        when(bidRepository.getBidsByAuctionId("A1"))
                .thenReturn(List.of(bid("B1", "A1", "u1", "bidder", 120.0, 1L)));

        assertThrows(InsufficientFundsException.class, () -> service.place(bidder, auction, 150.0));

        assertEquals(10.0, bidder.getWallet(), 0.001);
        verify(bidRepository, never()).saveBid(any());
    }

    @Test
    void place_rejectsInvalidInputsBeforeTakingAuctionLock() {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository, transactionRepository);
        RegularUser bidder = new RegularUser("u1", "bidder", "password", "u1@example.com");
        bidder.setWallet(500.0);
        Auction auction = auction("A1", 100.0);

        assertThrows(PermissionDeniedException.class, () -> service.place(null, auction, 120.0));
        assertThrows(PermissionDeniedException.class, () -> service.place(bidder, null, 120.0));
        assertThrows(InvalidBidException.class, () -> service.place(bidder, auction, 0.0));

        bidder.setWallet(0.0);
        assertThrows(PermissionDeniedException.class, () -> service.place(bidder, auction, 120.0));

        bidder.setWallet(500.0);
        Auction noItemAuction = new Auction("A2", "IT2", "seller-1", "seller",
                null, 100.0, 30);
        assertThrows(InvalidBidException.class, () -> service.place(bidder, noItemAuction, 120.0));

        verifyNoInteractions(bidRepository, auctionRepository, userRepository);
    }

    @Test
    void place_usesInjectedValidatorBeforeRepositoryWork() throws Exception {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository,
                transactionRepository, bidValidator, chargeCalculator);
        RegularUser bidder = new RegularUser("u1", "bidder", "password", "u1@example.com");
        Auction auction = auction("A1", 100.0);
        doThrow(new InvalidBidException("invalid"))
                .when(bidValidator).validateBidderAndAuction(bidder, auction, 120.0);

        assertThrows(InvalidBidException.class, () -> service.place(bidder, auction, 120.0));

        verify(bidValidator).validateBidderAndAuction(bidder, auction, 120.0);
        verifyNoInteractions(bidRepository, auctionRepository, userRepository, transactionRepository, chargeCalculator);
    }

    @Test
    void place_rejectsNonPositiveChargeFromInjectedCalculator() throws Exception {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository,
                transactionRepository, bidValidator, chargeCalculator);
        RegularUser bidder = new RegularUser("u1", "bidder", "password", "u1@example.com");
        bidder.setWallet(500.0);
        Auction auction = auction("A1", 100.0);
        when(auctionRepository.getAuctionById("A1")).thenReturn(auction);
        when(bidRepository.getBidsByAuctionId("A1")).thenReturn(List.of());
        when(chargeCalculator.calculateChargeAmount(120.0, 0.0)).thenReturn(0.0);

        assertThrows(InvalidBidException.class, () -> service.place(bidder, auction, 120.0));

        verify(bidRepository, never()).saveBid(any());
        verify(auctionRepository, never()).saveAuction(any());
        verify(userRepository, never()).saveUser(any());
    }

    @Test
    void placeForCurrentUser_objectPayloadDelegatesAndMissingAuctionThrows() throws Exception {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository, transactionRepository);
        RegularUser bidder = new RegularUser("u1", "bidder", "password", "u1@example.com");
        bidder.setWallet(500.0);
        completeProfile(bidder);
        Auction auction = auction("A1", 100.0);
        when(auctionRepository.getAuctionById("missing")).thenReturn(null);
        when(auctionRepository.getAuctionById("A1")).thenReturn(auction);
        when(userRepository.getUserById("u1")).thenReturn(bidder);
        when(bidRepository.getBidsByAuctionId("A1")).thenReturn(List.of());

        assertThrows(PermissionDeniedException.class,
                () -> service.placeForCurrentUser(bidder, "missing", 120.0));

        try (MockedConstruction<SqlTransactionRepository> ignored =
                     mockConstruction(SqlTransactionRepository.class)) {
            BidPlacementResult result = service.placeForCurrentUser(
                    bidder, Map.of("auctionId", "A1", "amount", 130.0));

            assertEquals("A1", result.getAuction().getAuctionId());
            assertEquals(130.0, result.getBid().getAmount(), 0.001);
        }
    }

    @Test
    void placeForCurrentUser_rejectsUserWithoutContactProfile() throws Exception {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository, transactionRepository);
        RegularUser bidder = new RegularUser("u1", "bidder", "password", "u1@example.com");
        bidder.setWallet(500.0);
        Auction auction = auction("A1", 100.0);
        when(auctionRepository.getAuctionById("A1")).thenReturn(auction);
        when(userRepository.getUserById("u1")).thenReturn(bidder);

        assertThrows(PermissionDeniedException.class,
                () -> service.placeForCurrentUser(bidder, "A1", 130.0));
    }

    @Test
    void historyAndUserBidList_requestPayloadsDelegateToStringMethods() throws Exception {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository, transactionRepository);
        Auction auction = auction("A1", 100.0);
        Bid historyBid = bid("B1", "A1", "u1", "bidder", 120.0, 1L);
        Bid userBid = bid("B2", "A2", "u2", "other", 140.0, 2L);
        when(auctionRepository.getAuctionById("A1")).thenReturn(auction);
        when(bidRepository.getBidsByAuctionId("A1")).thenReturn(List.of(historyBid));
        when(bidRepository.getBidsByBidderId("u2")).thenReturn(List.of(userBid));
        when(bidRepository.getBidsLinkedToUser("u2")).thenReturn(List.of());

        assertEquals(List.of(historyBid), service.getHistory(Map.of("auctionId", "A1")));
        assertEquals(List.of(userBid), service.getUserBidList(null, "u2"));
    }

    @Test
    void getUserBidList_withNullDataUsesCurrentUser() throws Exception {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository, transactionRepository);
        RegularUser currentUser = new RegularUser("u1", "user", "password", "u1@example.com");
        Bid bid = bid("B1", "A1", "u1", "user", 110.0, 1L);
        when(bidRepository.getBidsByBidderId("u1")).thenReturn(List.of(bid));
        when(bidRepository.getBidsLinkedToUser("u1")).thenReturn(List.of());

        assertEquals(List.of(bid), service.getUserBidList(currentUser, null));
    }

    @Test
    void getUserBidList_withoutCurrentUserOrExplicitUser_throws() {
        BidService service = new BidService(bidRepository, auctionRepository, userRepository, transactionRepository);

        assertThrows(IllegalStateException.class, () -> service.getUserBidList(null, null));
    }

    private static Auction auction(String auctionId, double currentPrice) {
        long now = System.currentTimeMillis();
        OtherItem item = new OtherItem("IT1", "Test item", "description", 100.0, "seller-1", List.of());
        assertEquals(ItemCategory.OTHER, item.getCategory());
        Auction auction = new Auction(auctionId, "IT1", "seller-1", "seller", item,
                currentPrice, now - 1_000L, now + 60_000L);
        auction.setMinimumBidIncrement(1.0);
        return auction;
    }

    private static Bid bid(String bidId, String auctionId, String bidderId, String bidderName,
                           double amount, long bidTime) {
        Bid bid = new Bid(bidId, auctionId, bidderId, bidderName, amount);
        bid.setBidTime(bidTime);
        return bid;
    }

    private static void completeProfile(RegularUser user) {
        user.setShopPhone("0912345678");
        user.setShopAddress("Ha Noi");
    }
}
