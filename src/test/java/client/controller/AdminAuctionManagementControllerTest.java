package client.controller;

import common.AuctionStatus;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.OtherItem;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdminAuctionManagementControllerTest {
    @Test
    void adminAuctionRowMapsAuctionFieldsAndFormatsValues() throws Exception {
        long now = System.currentTimeMillis();
        OtherItem item = new OtherItem("IT1", "Vintage Clock", "desc", 100_000.0, "seller-1", List.of());
        Auction auction = new Auction("A1", "IT1", "seller-1", "Seller Name", item,
                100_000.0, now - 1_000L, now + 60_000L);
        auction.setStatus(AuctionStatus.RUNNING);
        auction.setCurrentPrice(150_000.0);

        Object row = rowFromAuction(auction);

        assertEquals("A1", property(row, "auctionIdProperty"));
        assertEquals("IT1", property(row, "itemIdProperty"));
        assertEquals("seller-1", property(row, "sellerIdProperty"));
        assertEquals("Vintage Clock", property(row, "itemNameProperty"));
        assertTrue(property(row, "currentPriceProperty").contains("150"));
        assertEquals("RUNNING", property(row, "statusProperty"));
        assertEquals("RUNNING", invoke(row, "getRawStatus"));
        assertEquals(150_000.0, (double) invoke(row, "getRawCurrentPrice"), 0.001);
        assertTrue((long) invoke(row, "getCreatedAt") > 0);
        assertEquals(auction.getEndTime(), invoke(row, "getEndTimeMillis"));
        assertNotNull(invoke(row, "getCreatedDate"));
    }

    @Test
    void adminAuctionRowUsesDashFallbacksAndSearchesFields() throws Exception {
        Auction auction = new Auction();
        auction.setCurrentPrice(0.0);
        Object row = rowFromAuction(auction);

        assertEquals("-", property(row, "auctionIdProperty"));
        assertEquals("-", property(row, "itemIdProperty"));
        assertEquals("-", property(row, "sellerIdProperty"));
        assertEquals("-", property(row, "itemNameProperty"));
        assertEquals("-", property(row, "statusProperty"));

        Method matches = row.getClass().getDeclaredMethod("matches", String.class);
        matches.setAccessible(true);
        assertTrue((boolean) matches.invoke(row, "-"));
        assertFalse((boolean) matches.invoke(row, "not-present"));
    }

    @Test
    void dateFormatterReturnsDashForInvalidTime() throws Exception {
        Class<?> rowClass = Class.forName("client.controller.AdminAuctionManagementController$AdminAuctionRow");
        Method formatDateTime = rowClass.getDeclaredMethod("formatDateTime", long.class);
        formatDateTime.setAccessible(true);

        assertEquals("-", formatDateTime.invoke(null, 0L));
        assertNotEquals("-", formatDateTime.invoke(null, System.currentTimeMillis()));
    }

    private static Object rowFromAuction(Auction auction) throws Exception {
        Class<?> rowClass = Class.forName("client.controller.AdminAuctionManagementController$AdminAuctionRow");
        Method fromAuction = rowClass.getDeclaredMethod("fromAuction", Auction.class);
        fromAuction.setAccessible(true);
        return fromAuction.invoke(null, auction);
    }

    private static String property(Object row, String methodName) throws Exception {
        Object property = invoke(row, methodName);
        Method get = property.getClass().getMethod("get");
        return (String) get.invoke(property);
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }
}
