package server.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequestDtoTest {
    @Test
    void auctionIdRequest_extractsAuctionIdFromStringOrMap() {
        assertEquals("A1", AuctionIdRequest.from(" A1 ").auctionId());
        assertEquals("A2", AuctionIdRequest.from(Map.of("auctionId", " A2 ")).auctionId());
        assertThrows(IllegalArgumentException.class, () -> AuctionIdRequest.from("--"));
    }

    @Test
    void placeBidRequest_extractsAuctionIdAndAmount() {
        PlaceBidRequest request = PlaceBidRequest.from(Map.of("auctionId", "A1", "amount", "125.5"));

        assertEquals("A1", request.auctionId());
        assertEquals(125.5, request.amount(), 0.001);
        assertThrows(IllegalArgumentException.class, () -> PlaceBidRequest.from("not-map"));
    }

    @Test
    void setUserStatusRequest_requiresUserIdAndParsesActive() throws Exception {
        SetUserStatusRequest request = SetUserStatusRequest.from(Map.of("userId", "u1", "active", "true"));

        assertEquals("u1", request.userId());
        assertTrue(request.active());
        assertThrows(IOException.class, () -> SetUserStatusRequest.from(Map.of("active", false)));
    }

    @Test
    void upgradeSellerRequest_extractsSellerProfileFields() {
        UpgradeSellerRequest request = UpgradeSellerRequest.from(Map.of(
                "password", "secret",
                "shopName", "Shop",
                "phone", "123",
                "address", "Addr",
                "email", "shop@example.com"
        ));

        assertEquals("secret", request.password());
        assertEquals("Shop", request.shopName());
        assertEquals("123", request.phone());
        assertEquals("Addr", request.address());
        assertEquals("shop@example.com", request.email());
    }
}
