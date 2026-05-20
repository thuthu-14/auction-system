package server.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import server.concurrency.ConcurrentBidManager;
import server.model.Auction;
import server.model.Bid;
import server.model.RegularUser;
import server.model.User;
import server.observer.AuctionManager;
import server.repository.AuctionRepository;
import server.repository.BidRepository;
import server.repository.ItemRepository;
import server.repository.UserRepository;
import util.ItemValidationUtil;
import server.exception.PermissionDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuctionServiceTest {

    @Mock
    AuctionRepository auctionRepository;

    @Mock
    ItemRepository itemRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    BidRepository bidRepository;

    @Mock
    server.repository.SqlTransactionRepository transactionRepository;

    private AutoCloseable mocksClose;

    @BeforeEach
    void init() {
        mocksClose = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocksClose != null) {
            mocksClose.close();
        }
    }

    @Test
    public void create_success_persistsEntitiesAndNotifies() throws Exception {
        try (MockedStatic<ItemValidationUtil> ignored = mockStatic(ItemValidationUtil.class)) {
            ignored.when(() -> ItemValidationUtil.getStartingPriceErrorMessage(any(), anyDouble()))
                    .thenReturn(null);
            ignored.when(() -> ItemValidationUtil.getDurationErrorMessage(any(), anyInt()))
                    .thenReturn(null);

            AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                    bidRepository, transactionRepository);

            RegularUser seller = mock(RegularUser.class);
            when(seller.isActive()).thenReturn(true);
            when(seller.isSeller()).thenReturn(true);
            when(seller.getUserId()).thenReturn("seller-1");
            when(seller.getUsername()).thenReturn("seller1");

            server.model.Item item = mock(server.model.Item.class);
            when(item.getStartingPrice()).thenReturn(100.0);
            when(item.getCategory()).thenReturn(null);

            Auction created = service.create(seller, item, 10);

            verify(itemRepository, times(1)).saveItem(item);
            verify(auctionRepository, times(1)).createAuction(any(Auction.class));
            verify(seller, times(1)).addOwnedAuction(anyString());
            verify(userRepository, times(1)).saveUser(seller);
            assertNotNull(created);
            assertEquals(seller.getUserId(), created.getSellerId());
        }
    }

    @Test
    public void create_onRepositoryError_attemptsCleanupAndRethrows() throws Exception {
        try (MockedStatic<ItemValidationUtil> ignored = mockStatic(ItemValidationUtil.class)) {
            ignored.when(() -> ItemValidationUtil.getStartingPriceErrorMessage(any(), anyDouble()))
                    .thenReturn(null);
            ignored.when(() -> ItemValidationUtil.getDurationErrorMessage(any(), anyInt()))
                    .thenReturn(null);

            AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                    bidRepository, transactionRepository);

            RegularUser seller = mock(RegularUser.class);
            when(seller.isActive()).thenReturn(true);
            when(seller.isSeller()).thenReturn(true);
            when(seller.getUserId()).thenReturn("seller-2");
            when(seller.getUsername()).thenReturn("seller2");

            server.model.Item item = mock(server.model.Item.class);
            when(item.getStartingPrice()).thenReturn(50.0);
            when(item.getCategory()).thenReturn(null);

            doThrow(new RuntimeException("DB error")).when(auctionRepository).createAuction(any(Auction.class));

            Exception ex = assertThrows(Exception.class, () -> service.create(seller, item, 5));
            assertTrue(ex.getMessage().contains("DB error"));

            verify(auctionRepository, atLeastOnce()).deleteAuction(anyString());
        }
    }

    @Test
    public void create_withStartTimeInPast_throwsPermissionDenied() {
        try (MockedStatic<ItemValidationUtil> ignored = mockStatic(ItemValidationUtil.class)) {
            ignored.when(() -> ItemValidationUtil.getStartingPriceErrorMessage(any(), anyDouble()))
                    .thenReturn(null);
            ignored.when(() -> ItemValidationUtil.getDurationErrorMessage(any(), anyInt()))
                    .thenReturn(null);

            AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                    bidRepository, transactionRepository);

            RegularUser seller = mock(RegularUser.class);
            when(seller.isActive()).thenReturn(true);
            when(seller.isSeller()).thenReturn(true);

            server.model.Item item = mock(server.model.Item.class);
            when(item.getStartingPrice()).thenReturn(10.0);
            when(item.getCategory()).thenReturn(null);

            long now = System.currentTimeMillis();
            long start = now - 120_000L; // more than allowed 60s in the past
            long end = now + 10_000L;

            assertThrows(PermissionDeniedException.class, () ->
                    service.create(seller, item, start, end, 0.0, 0.0));
        }
    }

    @Test
    public void finish_callsRefundsForLosingBid() throws Exception {
        try (MockedStatic<ItemValidationUtil> ignored = mockStatic(ItemValidationUtil.class);
             MockedStatic<ConcurrentBidManager> cbmStatic = mockStatic(ConcurrentBidManager.class);
             MockedStatic<AuctionManager> amStatic = mockStatic(AuctionManager.class)) {

            ignored.when(() -> ItemValidationUtil.getStartingPriceErrorMessage(any(), anyDouble()))
                    .thenReturn(null);
            ignored.when(() -> ItemValidationUtil.getDurationErrorMessage(any(), anyInt()))
                    .thenReturn(null);

            // prepare mocked manager to directly invoke the callable without real locking
            ConcurrentBidManager mockManager = mock(ConcurrentBidManager.class);
            cbmStatic.when(ConcurrentBidManager::getInstance).thenReturn(mockManager);
            when(mockManager.withAuctionWriteLock(anyString(), any()))
                    .thenAnswer(invocation -> {
                        java.util.concurrent.Callable<?> action = invocation.getArgument(1);
                        return action.call();
                    });

            AuctionManager mockAuctionManager = mock(AuctionManager.class);
            amStatic.when(AuctionManager::getInstance).thenReturn(mockAuctionManager);

            AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                    bidRepository, transactionRepository);

            Auction auction = mock(Auction.class);
            when(auction.getAuctionId()).thenReturn("A1");
            when(auction.getStatus()).thenReturn(common.AuctionStatus.RUNNING);
            when(auction.getHighestBidderId()).thenReturn("u2");

            Bid b1 = mock(Bid.class);
            when(b1.getBidderId()).thenReturn("u1");
            when(b1.getAmount()).thenReturn(100.0);
            when(b1.getBidTime()).thenReturn(1L);

            Bid b2 = mock(Bid.class);
            when(b2.getBidderId()).thenReturn("u2");
            when(b2.getAmount()).thenReturn(150.0);
            when(b2.getBidTime()).thenReturn(2L);

            when(auctionRepository.getAuctionById("A1")).thenReturn(auction);
            when(bidRepository.getBidsByAuctionId("A1")).thenReturn(java.util.List.of(b1, b2));

            User user1 = mock(User.class);
            when(userRepository.getUserById("u1")).thenReturn(user1);

            service.finish("A1");

            verify(userRepository, atLeastOnce()).saveUser(user1);
            verify(transactionRepository, atLeastOnce()).saveTransaction(any());
        }
    }

    @Test
    public void cancel_refundsAllBids() throws Exception {
        try (MockedStatic<ItemValidationUtil> ignored = mockStatic(ItemValidationUtil.class);
             MockedStatic<ConcurrentBidManager> cbmStatic = mockStatic(ConcurrentBidManager.class);
             MockedStatic<AuctionManager> amStatic = mockStatic(AuctionManager.class)) {

            ignored.when(() -> ItemValidationUtil.getStartingPriceErrorMessage(any(), anyDouble()))
                    .thenReturn(null);
            ignored.when(() -> ItemValidationUtil.getDurationErrorMessage(any(), anyInt()))
                    .thenReturn(null);

            ConcurrentBidManager mockManager = mock(ConcurrentBidManager.class);
            cbmStatic.when(ConcurrentBidManager::getInstance).thenReturn(mockManager);
            when(mockManager.withAuctionWriteLock(anyString(), any()))
                    .thenAnswer(invocation -> {
                        java.util.concurrent.Callable<?> action = invocation.getArgument(1);
                        return action.call();
                    });

            AuctionManager mockAuctionManager = mock(AuctionManager.class);
            amStatic.when(AuctionManager::getInstance).thenReturn(mockAuctionManager);

            AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                    bidRepository, transactionRepository);

            Auction auction = mock(Auction.class);
            when(auction.getAuctionId()).thenReturn("A2");
            when(auction.getStatus()).thenReturn(common.AuctionStatus.RUNNING);
            when(auction.getHighestBidderId()).thenReturn("u2");
            when(auction.getSellerId()).thenReturn("seller1");

            Bid b1 = mock(Bid.class);
            when(b1.getBidderId()).thenReturn("u1");
            when(b1.getAmount()).thenReturn(100.0);
            when(b1.getBidTime()).thenReturn(1L);

            Bid b2 = mock(Bid.class);
            when(b2.getBidderId()).thenReturn("u2");
            when(b2.getAmount()).thenReturn(150.0);
            when(b2.getBidTime()).thenReturn(2L);

            when(auctionRepository.getAuctionById("A2")).thenReturn(auction);
            when(bidRepository.getBidsByAuctionId("A2")).thenReturn(java.util.List.of(b1, b2));

            User user1 = mock(User.class);
            User user2 = mock(User.class);
            when(userRepository.getUserById("u1")).thenReturn(user1);
            when(userRepository.getUserById("u2")).thenReturn(user2);

            Auction result = service.cancel("A2", "seller1");

            verify(userRepository, atLeastOnce()).saveUser(user1);
            verify(userRepository, atLeastOnce()).saveUser(user2);
            verify(transactionRepository, atLeastOnce()).saveTransaction(any());
            assertNotNull(result);
        }
    }

}