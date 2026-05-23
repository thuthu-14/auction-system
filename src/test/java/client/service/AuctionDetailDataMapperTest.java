package client.service;

import com.google.gson.JsonObject;
import common.AuctionStatus;
import common.ItemCategory;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.Art;
import server.model.Electronics;
import server.model.Fashion;
import server.model.Jewelry;
import server.model.Vehicle;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionDetailDataMapperTest {
    private final AuctionDetailDataMapper mapper = new AuctionDetailDataMapper();

    @Test
    void toDetailDataMapsAuctionAndElectronicsItemFields() {
        Electronics item = new Electronics(
                "IT1", "Laptop", "Gaming laptop", 12_000_000, "S1",
                "Asus", "24 months", List.of("img1")
        );
        long now = System.currentTimeMillis();
        Auction auction = new Auction("A1", "IT1", "S1", "seller", item, 13_000_000, now, now + 60_000);
        auction.setCurrentPrice(14_500_000);
        auction.setReservePrice(15_000_000);
        auction.setMinimumBidIncrement(500_000);

        JsonObject data = mapper.toDetailData(auction);
        JsonObject product = data.getAsJsonObject("item");

        assertEquals("A1", data.get("auctionId").getAsString());
        assertEquals(14_500_000, data.get("currentPrice").getAsDouble());
        assertEquals(15_000_000, data.get("reservePrice").getAsDouble());
        assertEquals(500_000, data.get("minimumBidIncrement").getAsDouble());
        assertEquals("seller", product.get("sellerName").getAsString());
        assertEquals("Laptop", product.get("name").getAsString());
        assertEquals("ELECTRONICS", product.get("type").getAsString());
        assertEquals("Asus", product.get("brand").getAsString());
        assertEquals("24 months", product.get("warrantyPeriod").getAsString());
    }

    @Test
    void toDetailDataHandlesNullAuctionAndNullItem() {
        assertEquals(0, mapper.toDetailData(null).size());

        Auction auction = new Auction("A2", "IT2", "S1", "seller", null, 1_000, 30);

        JsonObject data = mapper.toDetailData(auction);

        assertEquals("A2", data.get("auctionId").getAsString());
        JsonObject product = data.getAsJsonObject("item");
        assertEquals("A2", product.get("auctionId").getAsString());
        assertEquals("seller", product.get("sellerName").getAsString());
        assertFalse(product.has("name"));
    }

    @Test
    void isAuctionEndedUsesNullStatusAndRemainingTime() {
        assertTrue(mapper.isAuctionEnded(null));

        Auction closed = new Auction("A3", "IT3", "S1", "seller", null, 1_000, 30);
        closed.setStatus(AuctionStatus.CLOSED);
        assertTrue(mapper.isAuctionEnded(closed));

        long now = System.currentTimeMillis();
        Auction expired = new Auction("A4", "IT4", "S1", "seller", null, 1_000, now - 120_000, now - 60_000);
        assertTrue(mapper.isAuctionEnded(expired));

        Auction open = new Auction("A5", "IT5", "S1", "seller", null, 1_000, now, now + 60_000);
        open.setStatus(AuctionStatus.OPEN);
        assertFalse(mapper.isAuctionEnded(open));
    }

    @Test
    void resolveBidIncrementUsesCategoryOrDefault() {
        assertEquals(ItemCategory.ELECTRONICS.getMinimumBidIncrement(), mapper.resolveBidIncrement("ELECTRONICS"));
        assertEquals(500_000, mapper.resolveBidIncrement("NOT_A_CATEGORY"));
        assertEquals(500_000, mapper.resolveBidIncrement(null));
    }

    @Test
    void toDetailDataMapsAllSpecializedItemFields() {
        assertEquals("Model S", itemJson(new Vehicle("V1", "Car", "desc", 1, "S1", "Model S", 1200, List.of()))
                .get("model").getAsString());
        assertEquals(1200, itemJson(new Vehicle("V1", "Car", "desc", 1, "S1", "Model S", 1200, List.of()))
                .get("odometer").getAsInt());

        JsonObject fashion = itemJson(new Fashion("F1", "Coat", "desc", 1, "S1", "Uniqlo", "Cotton", List.of()));
        assertEquals("Uniqlo", fashion.get("brand").getAsString());
        assertEquals("Cotton", fashion.get("material").getAsString());

        JsonObject jewelry = itemJson(new Jewelry("J1", "Ring", "desc", 1, "S1", "Gold", 3.5, List.of()));
        assertEquals("Gold", jewelry.get("material").getAsString());
        assertEquals(3.5, jewelry.get("weight").getAsDouble());

        JsonObject art = itemJson(new Art("AR1", "Painting", "desc", 1, "S1", "Monet", "Oil", List.of()));
        assertEquals("Monet", art.get("creator").getAsString());
        assertEquals("Oil", art.get("material").getAsString());
    }

    @Test
    void isAuctionEndedRecognizesFinishedAndCancelledStatus() {
        Auction finished = new Auction("A6", "IT6", "S1", "seller", null, 1_000, 30);
        finished.setStatus(AuctionStatus.FINISHED);
        assertTrue(mapper.isAuctionEnded(finished));

        Auction cancelled = new Auction("A7", "IT7", "S1", "seller", null, 1_000, 30);
        cancelled.setStatus(AuctionStatus.CANCELLED);
        assertTrue(mapper.isAuctionEnded(cancelled));
    }

    private JsonObject itemJson(server.model.Item item) {
        long now = System.currentTimeMillis();
        Auction auction = new Auction("A-" + item.getItemId(), item.getItemId(), "S1", "seller", item, 2, now, now + 60_000);
        return mapper.toDetailData(auction).getAsJsonObject("item");
    }
}
