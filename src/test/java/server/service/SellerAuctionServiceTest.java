package server.service;

import common.ItemCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import server.exception.PermissionDeniedException;
import server.model.Art;
import server.model.Auction;
import server.model.Electronics;
import server.model.Fashion;
import server.model.Item;
import server.model.Jewelry;
import server.model.OtherItem;
import server.model.RegularUser;
import server.model.User;
import server.model.Vehicle;
import server.repository.AuctionRepository;
import server.repository.ItemRepository;
import server.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class SellerAuctionServiceTest {

    @Mock
    AuctionRepository auctionRepository;

    @Mock
    ItemRepository itemRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    AuctionService auctionService;

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
    public void createSellerAuction_permissionDeniedForNonSeller() throws Exception {
        SellerAuctionService svc = new SellerAuctionService();
        Exception ex = assertThrows(Exception.class, () -> svc.createSellerAuction((RegularUser) null, java.util.Map.of()));
        assertNotNull(ex);
    }

    @Test
    public void createSellerAuction_rejectsRegularUserWhoIsNotSeller() {
        SellerAuctionService service = new SellerAuctionService(auctionService);
        RegularUser user = new RegularUser("u1", "user", "password", "user@example.com");

        assertThrows(PermissionDeniedException.class,
                () -> service.createSellerAuction(user, validOtherAuctionData()));
    }

    @Test
    public void createAuction_rejectsMissingItemTypeAndInvalidCommonFields() {
        SellerAuctionService service = new SellerAuctionService(auctionService);
        RegularUser user = new RegularUser("u1", "user", "password", "user@example.com");

        assertThrows(IllegalArgumentException.class, () -> service.createAuction(user, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> service.createAuction(user, Map.of(
                        "itemType", "OTHER",
                        "name", "ab",
                        "description", "description",
                        "price", 10.0,
                        "duration", ItemCategory.OTHER.getMinDurationMinutes()
                )));
        assertThrows(IllegalArgumentException.class,
                () -> service.createAuction(user, Map.of(
                        "itemType", "OTHER",
                        "name", "Valid name",
                        "description", "description",
                        "price", 10.0,
                        "duration", 1
                )));
    }

    @Test
    public void createAuction_rejectsInvalidTimeWindow() {
        SellerAuctionService service = new SellerAuctionService(auctionService);
        RegularUser user = new RegularUser("u1", "user", "password", "user@example.com");
        long now = System.currentTimeMillis();
        Map<String, Object> data = new java.util.HashMap<>(validOtherAuctionData());
        data.put("startTime", now + 60_000L);
        data.put("endTime", now + 30_000L);

        assertThrows(IllegalArgumentException.class, () -> service.createAuction(user, data));
    }

    @Test
    public void createAuction_validOtherPayloadDelegatesToAuctionService() throws Exception {
        SellerAuctionService service = new SellerAuctionService(auctionService);
        RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
        seller.setSeller(true);
        Auction expected = mock(Auction.class);

        when(auctionService.create(
                same(seller), any(OtherItem.class), anyLong(), anyLong(), eq(25.0), eq(5.0)))
                .thenReturn(expected);

        Map<String, Object> data = new java.util.HashMap<>(validOtherAuctionData());
        data.put("reservePrice", 25.0);
        data.put("minimumBidIncrement", 5.0);

        Auction created = service.createSellerAuction(seller, data);

        assertSame(expected, created);
    }

    @Test
    public void createAuction_userWrappersRequireRegularUserAndAcceptFallbackKeys() throws Exception {
        SellerAuctionService service = new SellerAuctionService(auctionService);
        RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
        seller.setSeller(true);
        User anonymous = mock(User.class);
        Auction expected = mock(Auction.class);
        long now = System.currentTimeMillis();
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("Type", "ELECTRONICS");
        payload.put("name", "Valid item");
        payload.put("description", "Valid description");
        payload.put("startingPrice", 50_000.0);
        payload.put("duration", ItemCategory.ELECTRONICS.getMinDurationMinutes());
        payload.put("startTime", String.valueOf(now + 60_000L));
        payload.put("endTime", String.valueOf(now + (ItemCategory.ELECTRONICS.getMinDurationMinutes() + 1L) * 60_000L));
        payload.put("brand", "Brand");
        payload.put("warranty", "12 months");
        payload.put("images", "not-a-list");

        when(auctionService.create(
                same(seller), any(Electronics.class), anyLong(), anyLong(), anyDouble(), anyDouble()))
                .thenReturn(expected);

        assertThrows(PermissionDeniedException.class, () -> service.createAuction(anonymous, payload));
        assertSame(expected, service.createAuction((User) seller, payload));
        assertSame(expected, service.createSellerAuction((User) seller, payload));
    }

    @Test
    public void createItemFromData_buildsExpectedItemSubtypeAndImages() throws Exception {
        SellerAuctionService service = new SellerAuctionService(auctionService);

        Item electronics = service.createItemFromData("seller-1", "ELECTRONICS", Map.of(
                "name", "Phone",
                "description", "Nice phone",
                "price", 50_000.0,
                "brand", "Brand",
                "warrantyPeriod", "12 months",
                "images", List.of("a.jpg", 10)
        ));
        assertInstanceOf(Electronics.class, electronics);
        assertEquals(List.of("a.jpg", "10"), electronics.getImages());

        assertInstanceOf(Art.class, service.createItemFromData("seller-1", "ART", Map.of(
                "name", "Painting", "description", "Oil", "price", 100_000.0,
                "creator", "Artist", "material", "Canvas"
        )));
        assertInstanceOf(Vehicle.class, service.createItemFromData("seller-1", "VEHICLE", Map.of(
                "name", "Motorbike", "description", "Fast", "price", 500_000.0,
                "model", "Model X", "odometer", "123"
        )));
        assertInstanceOf(Fashion.class, service.createItemFromData("seller-1", "FASHION", Map.of(
                "name", "Jacket", "description", "Warm", "price", 10_000.0,
                "brand", "Brand", "material", "Cotton"
        )));
        assertInstanceOf(Jewelry.class, service.createItemFromData("seller-1", "JEWELRY", Map.of(
                "name", "Ring", "description", "Gold", "price", 25_000.0,
                "material", "Gold", "weight", "2.5"
        )));
    }

    @Test
    public void createAuction_validatesCategorySpecificRequiredFields() {
        SellerAuctionService service = new SellerAuctionService(auctionService);
        RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");

        assertThrows(IllegalArgumentException.class, () -> service.createAuction(seller, Map.of(
                "itemType", "ELECTRONICS",
                "name", "Valid name",
                "description", "description",
                "price", 50_000.0,
                "duration", ItemCategory.ELECTRONICS.getMinDurationMinutes(),
                "brand", "Brand"
        )));
        assertThrows(IllegalArgumentException.class, () -> service.createAuction(seller, Map.of(
                "itemType", "VEHICLE",
                "name", "Valid name",
                "description", "description",
                "price", 500_000.0,
                "duration", ItemCategory.VEHICLE.getMinDurationMinutes(),
                "model", "Model X",
                "odometer", -1
        )));
        assertThrows(IllegalArgumentException.class, () -> service.createAuction(seller, Map.of(
                "itemType", "JEWELRY",
                "name", "Valid name",
                "description", "description",
                "price", 25_000.0,
                "duration", ItemCategory.JEWELRY.getMinDurationMinutes(),
                "material", "Gold",
                "weight", 0.0
        )));
        assertThrows(IllegalArgumentException.class, () -> service.createAuction(seller, Map.of(
                "itemType", "ART",
                "name", "Valid name",
                "description", "description",
                "price", 100_000.0,
                "duration", ItemCategory.ART.getMinDurationMinutes(),
                "creator", "Artist",
                "material", ""
        )));
        assertThrows(IllegalArgumentException.class, () -> service.createAuction(seller, Map.of(
                "itemType", "FASHION",
                "name", "Valid name",
                "description", "description",
                "price", 10_000.0,
                "duration", ItemCategory.FASHION.getMinDurationMinutes(),
                "brand", "",
                "material", "Cotton"
        )));
    }

    @ParameterizedTest
    @MethodSource("validCategoryPayloads")
    public void createAuction_acceptsValidPayloadForEachCategory(Map<String, Object> payload,
                                                                 Class<? extends Item> expectedItemType)
            throws Exception {
        SellerAuctionService service = new SellerAuctionService(auctionService);
        RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
        Auction expected = mock(Auction.class);

        when(auctionService.create(
                same(seller), any(expectedItemType), anyLong(), anyLong(), anyDouble(), anyDouble()))
                .thenReturn(expected);

        assertSame(expected, service.createAuction(seller, payload));
    }

    @Test
    public void createAuction_rejectsUnknownItemType() {
        SellerAuctionService service = new SellerAuctionService(auctionService);
        RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
        Map<String, Object> data = new java.util.HashMap<>(validOtherAuctionData());
        data.put("itemType", "UNKNOWN");

        assertThrows(IllegalArgumentException.class, () -> service.createAuction(seller, data));
    }

    @Test
    public void deleteSellerAuction_requiresOwnerAndDelegatesDelete() throws Exception {
        SellerAuctionService service = new SellerAuctionService(auctionService);
        RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
        seller.setSeller(true);
        Auction auction = mock(Auction.class);
        when(auction.getSellerId()).thenReturn("seller-1");

        when(auctionService.getById("A1")).thenReturn(auction);

        service.deleteSellerAuction(seller, "A1");

        verify(auctionService).delete("A1");
    }

    @Test
    public void deleteSellerAuction_rejectsNonOwnerMissingAuctionAndNonSeller() throws Exception {
        SellerAuctionService service = new SellerAuctionService(auctionService);
        RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
        seller.setSeller(true);
        Auction auction = mock(Auction.class);
        when(auction.getSellerId()).thenReturn("other");

        when(auctionService.getById("A1")).thenReturn(auction);
        assertThrows(PermissionDeniedException.class, () -> service.deleteSellerAuction(seller, "A1"));

        when(auctionService.getById("missing")).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> service.deleteSellerAuction(seller, "missing"));

        seller.setSeller(false);
        assertThrows(PermissionDeniedException.class, () -> service.deleteSellerAuction(seller, "A1"));
    }

    @Test
    public void deleteSellerAuction_userWrapperReturnsAuctionId() throws Exception {
        SellerAuctionService service = new SellerAuctionService(auctionService);
        RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
        seller.setSeller(true);
        Auction auction = mock(Auction.class);
        when(auction.getSellerId()).thenReturn("seller-1");

        when(auctionService.getById("A1")).thenReturn(auction);

        assertEquals("A1", service.deleteSellerAuction((User) seller, Map.of("auctionId", "A1")));

        verify(auctionService).delete("A1");
    }

    private static Map<String, Object> validOtherAuctionData() {
        long now = System.currentTimeMillis();
        return Map.of(
                "itemType", "OTHER",
                "name", "Valid item",
                "description", "Valid description",
                "price", 10.0,
                "duration", ItemCategory.OTHER.getMinDurationMinutes(),
        "startTime", now + 60_000L,
                "endTime", now + (ItemCategory.OTHER.getMinDurationMinutes() + 1L) * 60_000L
        );
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> validCategoryPayloads() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(payload("ELECTRONICS", 50_000.0,
                        Map.of("brand", "Brand", "warrantyPeriod", "12 months")), Electronics.class),
                org.junit.jupiter.params.provider.Arguments.of(payload("ART", 100_000.0,
                        Map.of("creator", "Artist", "material", "Canvas")), Art.class),
                org.junit.jupiter.params.provider.Arguments.of(payload("VEHICLE", 500_000.0,
                        Map.of("model", "Model X", "odometer", 123)), Vehicle.class),
                org.junit.jupiter.params.provider.Arguments.of(payload("FASHION", 10_000.0,
                        Map.of("brand", "Brand", "material", "Cotton")), Fashion.class),
                org.junit.jupiter.params.provider.Arguments.of(payload("JEWELRY", 25_000.0,
                        Map.of("material", "Gold", "weight", 2.5)), Jewelry.class),
                org.junit.jupiter.params.provider.Arguments.of(validOtherAuctionData(), OtherItem.class)
        );
    }

    private static Map<String, Object> payload(String itemType, double price, Map<String, Object> extra) {
        long now = System.currentTimeMillis();
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("itemType", itemType);
        payload.put("name", "Valid item");
        payload.put("description", "Valid description");
        payload.put("price", price);
        payload.put("duration", ItemCategory.valueOf(itemType).getMinDurationMinutes());
        payload.put("startTime", now + 60_000L);
        payload.put("endTime", now + (ItemCategory.valueOf(itemType).getMinDurationMinutes() + 1L) * 60_000L);
        payload.putAll(extra);
        return payload;
    }
}


