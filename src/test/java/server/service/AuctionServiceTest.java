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
import server.model.Admin;
import server.model.OtherItem;
import server.model.RegularUser;
import server.model.User;
import server.observer.AuctionManager;
import server.repository.AuctionRepository;
import server.repository.BidRepository;
import server.repository.ItemRepository;
import server.repository.UserRepository;
import util.ItemValidationUtil;
import server.exception.PermissionDeniedException;
import common.ItemCategory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
            when(item.getCategory()).thenReturn(ItemCategory.OTHER);

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
    public void create_normalizesNegativeReserveAndIncrementAndUsesInjectedPublisher() throws Exception {
        try (MockedStatic<ItemValidationUtil> validation = mockStatic(ItemValidationUtil.class)) {
            validation.when(() -> ItemValidationUtil.getStartingPriceErrorMessage(any(), anyDouble()))
                    .thenReturn(null);
            validation.when(() -> ItemValidationUtil.getDurationErrorMessage(any(), anyInt()))
                    .thenReturn(null);

            AuctionEventPublisher publisher = mock(AuctionEventPublisher.class);
            ImageCleanupService cleanupService = mock(ImageCleanupService.class);
            AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                    bidRepository, transactionRepository, publisher, cleanupService);
            RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
            seller.setSeller(true);
            OtherItem item = new OtherItem(null, "Item", "Desc", 10.0, "seller-1", List.of());

            Auction auction = service.create(seller, item, 30, -100.0, -5.0);

            assertEquals(0.0, auction.getReservePrice(), 0.001);
            assertEquals(0.0, auction.getMinimumBidIncrement(), 0.001);
            verify(publisher).notifyAuctionCreated(auction);
            verifyNoInteractions(cleanupService);
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
            when(item.getCategory()).thenReturn(ItemCategory.OTHER);

            doThrow(new RuntimeException("DB error")).when(auctionRepository).createAuction(any(Auction.class));

            Exception ex = assertThrows(Exception.class, () -> service.create(seller, item, 5));
            assertTrue(ex.getMessage().contains("DB error"));

            verify(auctionRepository, atLeastOnce()).deleteAuction(anyString());
        }
    }

    @Test
    public void create_whenPersistAndCleanupBothFailRethrowsOriginalPersistError() throws Exception {
        try (MockedStatic<ItemValidationUtil> validation = mockStatic(ItemValidationUtil.class)) {
            validation.when(() -> ItemValidationUtil.getStartingPriceErrorMessage(any(), anyDouble()))
                    .thenReturn(null);
            validation.when(() -> ItemValidationUtil.getDurationErrorMessage(any(), anyInt()))
                    .thenReturn(null);
            AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                    bidRepository, transactionRepository);
            RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
            seller.setSeller(true);
            OtherItem item = new OtherItem(null, "Item", "Desc", 10.0, "seller-1", List.of());
            doThrow(new IOException("create failed")).when(auctionRepository).createAuction(any(Auction.class));
            doThrow(new IOException("cleanup failed")).when(auctionRepository).deleteAuction(anyString());

            Exception ex = assertThrows(IOException.class, () -> service.create(seller, item, 30));

            assertEquals("create failed", ex.getMessage());
            verify(auctionRepository).deleteAuction(anyString());
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
            when(item.getCategory()).thenReturn(ItemCategory.OTHER);

            long now = System.currentTimeMillis();
            long start = now - 120_000L; // more than allowed 60s in the past
            long end = now + 10_000L;

            assertThrows(PermissionDeniedException.class, () ->
                    service.create(seller, item, start, end, 0.0, 0.0, false));
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
    public void finish_creditsSellerWithWinningBidAmount() throws Exception {
        try (MockedStatic<ConcurrentBidManager> cbmStatic = mockStatic(ConcurrentBidManager.class)) {
            ConcurrentBidManager mockManager = mock(ConcurrentBidManager.class);
            cbmStatic.when(ConcurrentBidManager::getInstance).thenReturn(mockManager);
            when(mockManager.withAuctionWriteLock(anyString(), any()))
                    .thenAnswer(invocation -> {
                        java.util.concurrent.Callable<?> action = invocation.getArgument(1);
                        return action.call();
                    });

            AuctionEventPublisher publisher = mock(AuctionEventPublisher.class);
            AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                    bidRepository, transactionRepository, publisher, mock(ImageCleanupService.class));
            Auction auction = auction("A1", common.AuctionStatus.RUNNING);
            auction.setHighestBidderId("winner");
            auction.setCurrentPrice(250.0);
            RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
            seller.setSeller(true);

            when(auctionRepository.getAuctionById("A1")).thenReturn(auction);
            when(bidRepository.getBidsByAuctionId("A1")).thenReturn(List.of());
            when(userRepository.getUserById("seller-1")).thenReturn(seller);

            service.finish("A1");

            assertEquals(250.0, seller.getWallet(), 0.001);
            verify(userRepository).saveUser(seller);
            verify(transactionRepository).saveTransaction(argThat(tx ->
                    "seller-1".equals(tx.getUserId())
                            && "DEPOSIT".equals(tx.getType())
                            && tx.getAmount() == 250.0
                            && tx.getDescription().contains("Nh\u1eadn ti\u1ec1n")));
            verify(publisher).notifyAuctionFinished(auction);
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

    @Test
    public void cancel_refundsOnlyChargedBidDeltasAndSkipsInvalidBids() throws Exception {
        try (MockedStatic<ConcurrentBidManager> cbmStatic = mockStatic(ConcurrentBidManager.class)) {
            ConcurrentBidManager mockManager = mock(ConcurrentBidManager.class);
            cbmStatic.when(ConcurrentBidManager::getInstance).thenReturn(mockManager);
            when(mockManager.withAuctionWriteLock(anyString(), any()))
                    .thenAnswer(invocation -> {
                        java.util.concurrent.Callable<?> action = invocation.getArgument(1);
                        return action.call();
                    });

            AuctionEventPublisher publisher = mock(AuctionEventPublisher.class);
            AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                    bidRepository, transactionRepository, publisher, mock(ImageCleanupService.class));
            Auction auction = auction("A1", common.AuctionStatus.RUNNING);
            auction.setHighestBidderId("winner");
            when(auctionRepository.getAuctionById("A1")).thenReturn(auction);
            Bid first = bid("b1", "A1", "bidder", 100.0, 1L);
            Bid higherSameUser = bid("b2", "A1", "bidder", 130.0, 2L);
            Bid lowerSameUser = bid("b3", "A1", "bidder", 120.0, 3L);
            Bid blankBidder = bid("b4", "A1", " ", 200.0, 4L);
            when(bidRepository.getBidsByAuctionId("A1"))
                    .thenReturn(Arrays.asList(first, null, higherSameUser, lowerSameUser, blankBidder));
            RegularUser bidder = new RegularUser("bidder", "bidder", "password", "bidder@example.com");
            when(userRepository.getUserById("bidder")).thenReturn(bidder);

            service.cancel("A1", "seller-1");

            assertEquals(130.0, bidder.getWallet(), 0.001);
            verify(userRepository).saveUser(bidder);
            verify(transactionRepository).saveTransaction(argThat(tx -> tx.getAmount() == 130.0));
            verify(publisher).notifyAuctionCancelled(auction);
        }
    }

    @Test
    public void cancel_resolvesItemIdAndRejectsNullSeller() throws Exception {
        try (MockedStatic<ConcurrentBidManager> cbmStatic = mockStatic(ConcurrentBidManager.class)) {
            ConcurrentBidManager mockManager = mock(ConcurrentBidManager.class);
            cbmStatic.when(ConcurrentBidManager::getInstance).thenReturn(mockManager);
            when(mockManager.withAuctionWriteLock(anyString(), any()))
                    .thenAnswer(invocation -> {
                        java.util.concurrent.Callable<?> action = invocation.getArgument(1);
                        return action.call();
                    });
            AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                    bidRepository, transactionRepository);
            Auction auction = auction("A1", common.AuctionStatus.RUNNING);
            when(auctionRepository.getAuctionById("IT1")).thenReturn(null);
            when(auctionRepository.getAllAuctions()).thenReturn(List.of(auction));
            when(bidRepository.getBidsByAuctionId("A1")).thenReturn(null);

            assertThrows(PermissionDeniedException.class, () -> service.cancel("IT1", null));
            Auction cancelled = service.cancel("IT1", "seller-1");

            assertSame(auction, cancelled);
            assertEquals(common.AuctionStatus.CANCELLED, auction.getStatus());
        }
    }

    @Test
    public void finish_ignoresMissingFinishedAndCancelledAuctions() throws Exception {
        try (MockedStatic<ConcurrentBidManager> cbmStatic = mockStatic(ConcurrentBidManager.class)) {
            ConcurrentBidManager mockManager = mock(ConcurrentBidManager.class);
            cbmStatic.when(ConcurrentBidManager::getInstance).thenReturn(mockManager);
            when(mockManager.withAuctionWriteLock(anyString(), any()))
                    .thenAnswer(invocation -> {
                        java.util.concurrent.Callable<?> action = invocation.getArgument(1);
                        return action.call();
                    });

            AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                    bidRepository, transactionRepository);

            when(auctionRepository.getAuctionById("missing")).thenReturn(null);
            service.finish("missing");

            Auction finished = mock(Auction.class);
            when(finished.getStatus()).thenReturn(common.AuctionStatus.FINISHED);
            when(auctionRepository.getAuctionById("finished")).thenReturn(finished);
            service.finish("finished");

            Auction cancelled = mock(Auction.class);
            when(cancelled.getStatus()).thenReturn(common.AuctionStatus.CANCELLED);
            when(auctionRepository.getAuctionById("cancelled")).thenReturn(cancelled);
            service.finish("cancelled");

            verify(auctionRepository, never()).saveAuction(any());
            verifyNoInteractions(transactionRepository);
        }
    }

    @Test
    public void finish_withNoBidsMarksAuctionFinishedWithoutRefunds() throws Exception {
        try (MockedStatic<ConcurrentBidManager> cbmStatic = mockStatic(ConcurrentBidManager.class);
             MockedStatic<AuctionManager> amStatic = mockStatic(AuctionManager.class)) {
            ConcurrentBidManager mockManager = mock(ConcurrentBidManager.class);
            cbmStatic.when(ConcurrentBidManager::getInstance).thenReturn(mockManager);
            when(mockManager.withAuctionWriteLock(anyString(), any()))
                    .thenAnswer(invocation -> {
                        java.util.concurrent.Callable<?> action = invocation.getArgument(1);
                        return action.call();
                    });
            AuctionManager auctionManager = mock(AuctionManager.class);
            amStatic.when(AuctionManager::getInstance).thenReturn(auctionManager);

            AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                    bidRepository, transactionRepository);
            Auction auction = auction("A1", common.AuctionStatus.RUNNING);
            when(auctionRepository.getAuctionById("A1")).thenReturn(auction);
            when(bidRepository.getBidsByAuctionId("A1")).thenReturn(List.of());

            service.finish("A1");

            assertEquals(common.AuctionStatus.FINISHED, auction.getStatus());
            verify(auctionRepository).saveAuction(auction);
            verify(auctionManager).notifyAuctionFinished(auction);
            verifyNoInteractions(transactionRepository);
        }
    }

    @Test
    public void getListDelegatesToActiveOrAllAuctions() throws Exception {
        AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                bidRepository, transactionRepository);
        Auction active = mock(Auction.class);
        Auction closed = mock(Auction.class);
        when(auctionRepository.getActiveAuctions()).thenReturn(List.of(active));
        when(auctionRepository.getAllAuctions()).thenReturn(List.of(active, closed));

        assertEquals(List.of(active), service.getList(false));
        assertEquals(List.of(active, closed), service.getList(true));
    }

    @Test
    public void getById_resolvesAuctionIdOrItemId() throws Exception {
        AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                bidRepository, transactionRepository);
        Auction auction = mock(Auction.class);
        when(auction.getItemId()).thenReturn("IT1");
        when(auctionRepository.getAuctionById("A1")).thenReturn(auction);
        when(auctionRepository.getAuctionById("IT1")).thenReturn(null);
        when(auctionRepository.getAllAuctions()).thenReturn(List.of(auction));

        assertSame(auction, service.getById("A1"));
        assertSame(auction, service.getById("IT1"));
        assertThrows(IOException.class, () -> service.getById("missing"));
    }

    @Test
    public void getBySeller_requiresRegularSeller() throws Exception {
        AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                bidRepository, transactionRepository);
        RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
        seller.setSeller(true);
        Auction auction = mock(Auction.class);
        when(auctionRepository.getAuctionsBySellerId("seller-1")).thenReturn(List.of(auction));

        assertEquals(List.of(auction), service.getBySeller(seller));
        assertThrows(PermissionDeniedException.class, () -> service.getBySeller((User) null));
        seller.setSeller(false);
        assertThrows(PermissionDeniedException.class, () -> service.getBySeller(seller));
    }

    @Test
    public void deleteForAdmin_requiresAdminAndReturnsAuctionId() throws Exception {
        AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                bidRepository, transactionRepository);
        Admin admin = new Admin("admin-1", "admin", "password", "admin@example.com");
        RegularUser regular = new RegularUser("u1", "user", "password", "user@example.com");

        assertEquals("A1", service.deleteForAdmin(admin, Map.of("auctionId", "A1")));
        verify(auctionRepository).deleteAuction("A1");
        assertThrows(PermissionDeniedException.class,
                () -> service.deleteForAdmin(regular, Map.of("auctionId", "A2")));
    }

    @Test
    public void deleteAuctionWithImages_deletesImagesThenAuction() throws Exception {
        AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                bidRepository, transactionRepository);
        OtherItem item = new OtherItem("IT1", "Item", "Desc", 10.0, "seller-1", List.of("img"));
        Auction auction = new Auction("A1", "IT1", "seller-1", "seller", item, 10.0, 30);
        when(auctionRepository.getAuctionById("A1")).thenReturn(auction);

        try (MockedStatic<server.storage.ImageUploadManager> imageUploadManager =
                     mockStatic(server.storage.ImageUploadManager.class)) {
            service.deleteAuctionWithImages("A1");

            imageUploadManager.verify(() -> server.storage.ImageUploadManager.deleteItemImages("IT1"));
            verify(auctionRepository).deleteAuction("A1");
        }
    }

    @Test
    public void deleteAuctionWithImages_stillDeletesAuctionWhenImageCleanupFails() throws Exception {
        AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                bidRepository, transactionRepository);
        OtherItem item = new OtherItem("IT2", "Item", "Desc", 10.0, "seller-1", List.of("img"));
        Auction auction = new Auction("A2", "IT2", "seller-1", "seller", item, 10.0, 30);
        when(auctionRepository.getAuctionById("A2")).thenReturn(auction);

        try (MockedStatic<server.storage.ImageUploadManager> imageUploadManager =
                     mockStatic(server.storage.ImageUploadManager.class)) {
            imageUploadManager.when(() -> server.storage.ImageUploadManager.deleteItemImages("IT2"))
                    .thenThrow(new IOException("image cleanup failed"));

            service.deleteAuctionWithImages("A2");

            verify(auctionRepository).deleteAuction("A2");
        }
    }

    @Test
    public void deleteAuctionWithImages_withoutItemSkipsImageCleanup() throws Exception {
        AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                bidRepository, transactionRepository);
        Auction auction = new Auction("A3", "IT3", "seller-1", "seller", null, 10.0, 30);
        when(auctionRepository.getAuctionById("A3")).thenReturn(auction);

        try (MockedStatic<server.storage.ImageUploadManager> imageUploadManager =
                     mockStatic(server.storage.ImageUploadManager.class)) {
            service.deleteAuctionWithImages("A3");

            imageUploadManager.verifyNoInteractions();
            verify(auctionRepository).deleteAuction("A3");
        }
    }

    @Test
    public void deleteAuctionWithImages_usesInjectedCleanupAndRethrowsLookupFailure() throws Exception {
        ImageCleanupService cleanupService = mock(ImageCleanupService.class);
        AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                bidRepository, transactionRepository, mock(AuctionEventPublisher.class), cleanupService);
        OtherItem item = new OtherItem("IT9", "Item", "Desc", 10.0, "seller-1", List.of("img"));
        Auction auction = new Auction("A9", "IT9", "seller-1", "seller", item, 10.0, 30);
        when(auctionRepository.getAuctionById("A9")).thenReturn(auction);

        service.deleteAuctionWithImages("A9");

        verify(cleanupService).deleteItemImages("IT9");
        verify(auctionRepository).deleteAuction("A9");

        when(auctionRepository.getAuctionById("missing")).thenReturn(null);
        assertThrows(IOException.class, () -> service.deleteAuctionWithImages("missing"));
    }

    @Test
    public void cancel_rejectsMissingWrongSellerAndFinishedAuction() throws Exception {
        try (MockedStatic<ConcurrentBidManager> cbmStatic = mockStatic(ConcurrentBidManager.class)) {
            ConcurrentBidManager mockManager = mock(ConcurrentBidManager.class);
            cbmStatic.when(ConcurrentBidManager::getInstance).thenReturn(mockManager);
            when(mockManager.withAuctionWriteLock(anyString(), any()))
                    .thenAnswer(invocation -> {
                        java.util.concurrent.Callable<?> action = invocation.getArgument(1);
                        return action.call();
                    });

            AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                    bidRepository, transactionRepository);

            when(auctionRepository.getAuctionById("missing")).thenReturn(null);
            when(auctionRepository.getAllAuctions()).thenReturn(List.of());
            assertThrows(PermissionDeniedException.class, () -> service.cancel("missing", "seller-1"));

            Auction auction = mock(Auction.class);
            when(auction.getSellerId()).thenReturn("seller-1");
            when(auction.getStatus()).thenReturn(common.AuctionStatus.RUNNING);
            when(auctionRepository.getAuctionById("A1")).thenReturn(auction);
            assertThrows(PermissionDeniedException.class, () -> service.cancel("A1", "other"));

            when(auction.getStatus()).thenReturn(common.AuctionStatus.FINISHED);
            assertThrows(PermissionDeniedException.class, () -> service.cancel("A1", "seller-1"));
        }
    }

    @Test
    public void cancel_whenAlreadyCancelledReturnsAuctionWithoutSavingAgain() throws Exception {
        try (MockedStatic<ConcurrentBidManager> cbmStatic = mockStatic(ConcurrentBidManager.class)) {
            ConcurrentBidManager mockManager = mock(ConcurrentBidManager.class);
            cbmStatic.when(ConcurrentBidManager::getInstance).thenReturn(mockManager);
            when(mockManager.withAuctionWriteLock(anyString(), any()))
                    .thenAnswer(invocation -> {
                        java.util.concurrent.Callable<?> action = invocation.getArgument(1);
                        return action.call();
                    });

            AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                    bidRepository, transactionRepository);
            Auction auction = mock(Auction.class);
            when(auction.getSellerId()).thenReturn("seller-1");
            when(auction.getStatus()).thenReturn(common.AuctionStatus.CANCELLED);
            when(auctionRepository.getAuctionById("A1")).thenReturn(auction);

            assertSame(auction, service.cancel("A1", "seller-1"));
            verify(auctionRepository, never()).saveAuction(auction);
        }
    }

    @Test
    public void cancel_userWrapperRequiresSellerAndAcceptsRequestPayload() throws Exception {
        try (MockedStatic<ConcurrentBidManager> cbmStatic = mockStatic(ConcurrentBidManager.class);
             MockedStatic<AuctionManager> amStatic = mockStatic(AuctionManager.class)) {
            ConcurrentBidManager mockManager = mock(ConcurrentBidManager.class);
            cbmStatic.when(ConcurrentBidManager::getInstance).thenReturn(mockManager);
            when(mockManager.withAuctionWriteLock(anyString(), any()))
                    .thenAnswer(invocation -> {
                        java.util.concurrent.Callable<?> action = invocation.getArgument(1);
                        return action.call();
                    });
            AuctionManager auctionManager = mock(AuctionManager.class);
            amStatic.when(AuctionManager::getInstance).thenReturn(auctionManager);

            AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                    bidRepository, transactionRepository);
            RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
            seller.setSeller(true);
            RegularUser buyer = new RegularUser("buyer-1", "buyer", "password", "buyer@example.com");
            Auction auction = auction("A1", common.AuctionStatus.RUNNING);
            when(auctionRepository.getAuctionById("A1")).thenReturn(auction);
            when(bidRepository.getBidsByAuctionId("A1")).thenReturn(List.of());

            assertThrows(PermissionDeniedException.class,
                    () -> service.cancel(buyer, Map.of("auctionId", "A1")));
            assertSame(auction, service.cancel(seller, Map.of("auctionId", "A1")));

            assertEquals(common.AuctionStatus.CANCELLED, auction.getStatus());
            verify(auctionRepository).saveAuction(auction);
            verify(auctionManager).notifyAuctionCancelled(auction);
        }
    }

    @Test
    public void create_rejectsInactiveNonSellerNullItemAndInvalidTimeWindow() {
        AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                bidRepository, transactionRepository);
        RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
        seller.setSeller(true);
        OtherItem item = new OtherItem("IT1", "Item", "Desc", 10.0, "seller-1", List.of());
        long now = System.currentTimeMillis();

        seller.setActive(false);
        assertThrows(PermissionDeniedException.class,
                () -> service.create(seller, item, now + 1_000L, now + 2_000L, 0, 0, false));
        seller.setActive(true);
        seller.setSeller(false);
        assertThrows(PermissionDeniedException.class,
                () -> service.create(seller, item, now + 1_000L, now + 2_000L, 0, 0, false));
        seller.setSeller(true);
        assertThrows(PermissionDeniedException.class,
                () -> service.create(seller, null, now + 1_000L, now + 2_000L, 0, 0, false));
        assertThrows(PermissionDeniedException.class,
                () -> service.create(seller, item, now + 2_000L, now + 1_000L, 0, 0, false));
    }

    @Test
    public void create_rejectsNullSellerNullCategoryInvalidPriceAndDuration() {
        AuctionService service = new AuctionService(auctionRepository, itemRepository, userRepository,
                bidRepository, transactionRepository);
        RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
        seller.setSeller(true);
        OtherItem item = new OtherItem("IT1", "Item", "Desc", 10.0, "seller-1", List.of());

        assertThrows(PermissionDeniedException.class, () -> service.create(null, item, 30));

        server.model.Item noCategory = mock(server.model.Item.class);
        when(noCategory.getCategory()).thenReturn(null);
        assertThrows(PermissionDeniedException.class, () -> service.create(seller, noCategory, 30));

        try (MockedStatic<ItemValidationUtil> validation = mockStatic(ItemValidationUtil.class)) {
            validation.when(() -> ItemValidationUtil.getStartingPriceErrorMessage(any(), anyDouble()))
                    .thenReturn("bad price");
            assertThrows(PermissionDeniedException.class, () -> service.create(seller, item, 30));
        }

        try (MockedStatic<ItemValidationUtil> validation = mockStatic(ItemValidationUtil.class)) {
            validation.when(() -> ItemValidationUtil.getStartingPriceErrorMessage(any(), anyDouble()))
                    .thenReturn(null);
            validation.when(() -> ItemValidationUtil.getDurationErrorMessage(any(), anyInt()))
                    .thenReturn("bad duration");
            assertThrows(PermissionDeniedException.class, () -> service.create(seller, item, 30));
        }
    }

    private static Auction auction(String auctionId, common.AuctionStatus status) {
        long now = System.currentTimeMillis();
        OtherItem item = new OtherItem("IT1", "Item", "Desc", 10.0, "seller-1", List.of());
        Auction auction = new Auction(auctionId, "IT1", "seller-1", "seller", item,
                10.0, now - 1_000L, now + 60_000L);
        auction.setStatus(status);
        return auction;
    }

    private static Bid bid(String bidId, String auctionId, String bidderId, double amount, long bidTime) {
        Bid bid = new Bid(bidId, auctionId, bidderId, bidderId, amount);
        bid.setBidTime(bidTime);
        return bid;
    }

}
