package server.observer;

import common.AuctionStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.Bid;
import server.model.Notification;
import server.model.OtherItem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuctionNotificationObserverTest {
    private final CaptureNotificationObserver capture = new CaptureNotificationObserver();

    @AfterEach
    void restoreNotificationManager() throws Exception {
        setNotificationObservers(List.of(new SqlNotificationObserver()));
    }

    @Test
    void bidPlacedCreatesSellerAndOutbidNotifications() throws Exception {
        setNotificationObservers(List.of(capture));
        Auction auction = auction("A1", "seller-1", "Clock", 100.0);
        Bid bid = bid("B1", "A1", "bidder-2", 175.0, 2L);

        new AuctionNotificationObserver().update(auction, new AuctionEvent(AuctionEvent.BID_PLACED, bid, "bidder-1"));

        assertEquals(2, capture.notifications.size());
        assertEquals("seller-1", capture.notifications.get(0).getUserId());
        assertEquals("SELLER_BID", capture.notifications.get(0).getType());
        assertEquals("bidder-1", capture.notifications.get(1).getUserId());
        assertEquals("OUTBID", capture.notifications.get(1).getType());
    }

    @Test
    void bidPlacedSkipsNullBidSellerSelfBidAndSamePreviousBidder() throws Exception {
        setNotificationObservers(List.of(capture));
        Auction auction = auction("A1", "seller-1", "Clock", 100.0);

        new AuctionNotificationObserver().update(auction, new AuctionEvent(AuctionEvent.BID_PLACED));
        new AuctionNotificationObserver().update(auction,
                new AuctionEvent(AuctionEvent.BID_PLACED, bid("B1", "A1", "seller-1", 150.0, 1L), "seller-1"));

        assertTrue(capture.notifications.isEmpty());
    }

    @Test
    void helperMethodsCalculateRefundsAndFallbackText() throws Exception {
        AuctionNotificationObserver observer = new AuctionNotificationObserver();
        Auction auction = auction("A1", "seller-1", "", 100.0);
        auction.setHighestBidderId("u2");
        List<Bid> bids = new ArrayList<>(List.of(
                bid("B1", "A1", "u1", 130.0, 2L),
                bid("B2", "A1", "u2", 180.0, 3L),
                bid("B0", "A1", "u0", 90.0, 1L),
                bid("B3", "A1", "", 250.0, 4L)
        ));

        @SuppressWarnings("unchecked")
        Map<String, Double> includeWinner = (Map<String, Double>) invoke(observer, "calculateRefundsByUser", auction, bids, true);
        assertEquals(30.0, includeWinner.get("u1"), 0.001);
        assertEquals(50.0, includeWinner.get("u2"), 0.001);

        @SuppressWarnings("unchecked")
        Map<String, Double> excludeWinner = (Map<String, Double>) invoke(observer, "calculateRefundsByUser", auction, bids, false);
        assertEquals(30.0, excludeWinner.get("u1"), 0.001);
        assertFalse(excludeWinner.containsKey("u2"));

        assertEquals(100.0, (double) invoke(observer, "startingPrice", auction), 0.001);
        assertEquals("A1", invoke(observer, "productName", auction));
        assertEquals("1.200đ", invoke(observer, "formatVnd", 1200.0));
    }

    @Test
    void updateIgnoresUnknownEventsAndSwallowsNotificationFailures() throws Exception {
        setNotificationObservers(List.of(event -> {
            throw new RuntimeException("boom");
        }));
        Auction auction = auction("A1", "seller-1", "Clock", 100.0);

        assertDoesNotThrow(() -> new AuctionNotificationObserver().update(auction,
                new AuctionEvent(AuctionEvent.BID_PLACED, bid("B1", "A1", "u1", 150.0, 1L), "u0")));
        assertDoesNotThrow(() -> new AuctionNotificationObserver().update(auction, new AuctionEvent("UNKNOWN")));
    }

    private static Auction auction(String id, String sellerId, String name, double startPrice) {
        long now = System.currentTimeMillis();
        OtherItem item = new OtherItem("IT-" + id, name, "desc", startPrice, sellerId, List.of());
        Auction auction = new Auction(id, item.getItemId(), sellerId, "Seller", item, startPrice, now - 1_000L, now + 60_000L);
        auction.setStatus(AuctionStatus.OPEN);
        return auction;
    }

    private static Bid bid(String id, String auctionId, String bidderId, double amount, long time) {
        Bid bid = new Bid(id, auctionId, bidderId, "Bidder", amount);
        bid.setBidTime(time);
        return bid;
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

    private static void setNotificationObservers(List<NotificationObserver> observers) throws Exception {
        NotificationManager manager = NotificationManager.getInstance();
        Field field = NotificationManager.class.getDeclaredField("observers");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<NotificationObserver> current = (List<NotificationObserver>) field.get(manager);
        current.clear();
        current.addAll(observers);
    }

    private static final class CaptureNotificationObserver implements NotificationObserver {
        private final List<Notification> notifications = new ArrayList<>();

        @Override
        public void update(NotificationEvent event) {
            if (NotificationEvent.ADD_NOTIFICATION.equals(event.getType())) {
                notifications.add(event.getNotification());
            }
        }
    }
}
