package client.controller;

import common.AuctionStatus;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.Bid;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SellerWinnerInfoControllerTest {
    @Test
    void winnerResolutionPrefersHighestBidderAndIgnoresCancelledAuction() throws Exception {
        SellerWinnerInfoController controller = new SellerWinnerInfoController();
        Auction auction = auction(AuctionStatus.FINISHED, "u2");

        Bid low = bid("u2", 100);
        Bid high = bid("u2", 200);
        Bid other = bid("u3", 300);

        assertSame(high, invoke(controller, "findWinningBid", auction, List.of(low, high, other)));
        assertEquals("u2", invoke(controller, "resolveWinnerId", auction, List.of(low, high, other)));

        auction.setStatus(AuctionStatus.CANCELLED);
        assertNull(invoke(controller, "findWinningBid", auction, List.of(low, high)));
        assertNull(invoke(controller, "resolveWinnerId", auction, List.of(low, high)));
    }

    @Test
    void winnerResolutionFallsBackToHighestBidWhenAuctionHasNoHighestBidder() throws Exception {
        SellerWinnerInfoController controller = new SellerWinnerInfoController();
        Auction auction = auction(AuctionStatus.FINISHED, null);

        Bid low = bid("u1", 100);
        Bid high = bid("u2", 200);

        assertSame(high, invoke(controller, "findWinningBid", auction, List.of(low, high)));
        assertEquals("u2", invoke(controller, "resolveWinnerId", auction, List.of(low, high)));
    }

    @Test
    void textHelpersUseFallbacksAndStoredAuctionId() throws Exception {
        SellerWinnerInfoController controller = new SellerWinnerInfoController();
        setField(controller, "auctionId", "A1");

        assertEquals("fallback", invoke(controller, "safe", "", "fallback"));
        assertEquals("value", invoke(controller, "safe", "value", "fallback"));
        assertEquals("fallback", invoke(controller, "contactValue", null, "phone", "fallback"));
        assertEquals("fallback", invoke(controller, "contactValue", Map.of("phone", ""), "phone", "fallback"));
        assertEquals("090", invoke(controller, "contactValue", Map.of("phone", "090"), "phone", "fallback"));
        assertEquals("A2", invoke(controller, "resolveAuctionId", "A2"));
        assertEquals("A1", invoke(controller, "resolveAuctionId", "--"));
        assertTrue(((String) invoke(controller, "formatVnd", 1000.0)).contains("1"));
    }

    private static Auction auction(AuctionStatus status, String highestBidderId) {
        Auction auction = new Auction();
        auction.setStatus(status);
        auction.setHighestBidderId(highestBidderId);
        return auction;
    }

    private static Bid bid(String bidderId, double amount) {
        Bid bid = new Bid();
        bid.setBidderId(bidderId);
        bid.setAmount(amount);
        return bid;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object invoke(Object target, String methodName, Object... args) throws Exception {
        Method method = findMethod(target.getClass(), methodName, args.length);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Method findMethod(Class<?> type, String methodName, int argCount) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == argCount) {
                return method;
            }
        }
        throw new IllegalArgumentException("Method not found: " + methodName);
    }
}
