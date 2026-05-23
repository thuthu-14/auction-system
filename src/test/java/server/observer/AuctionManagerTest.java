package server.observer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.Bid;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionManagerTest {
    private final AuctionManager manager = AuctionManager.getInstance();

    @AfterEach
    void restoreManager() throws Exception {
        observers().clear();
        auctions().clear();
        manager.subscribe(new AuctionNotificationObserver());
    }

    @Test
    void subscribeAvoidsDuplicateObserverClassesAndUnsubscribeRemovesObserver() throws Exception {
        observers().clear();
        CaptureAuctionObserver first = new CaptureAuctionObserver();
        CaptureAuctionObserver second = new CaptureAuctionObserver();

        manager.subscribe(first);
        manager.subscribe(second);
        manager.subscribe(null);

        assertEquals(1, observers().size());
        manager.unsubscribe(first);
        assertTrue(observers().isEmpty());
    }

    @Test
    void notifyMethodsPublishExpectedEventTypesAndBidPayload() throws Exception {
        observers().clear();
        CaptureAuctionObserver observer = new CaptureAuctionObserver();
        manager.subscribe(observer);
        Auction auction = auction("A1");
        Bid bid = new Bid("B1", "A1", "u1", "alice", 150.0);

        manager.notifyAuctionCreated(auction);
        manager.notifyAuctionUpdated(auction);
        manager.notifyBidPlaced(auction);
        manager.notifyBidPlaced(auction, bid, "u0");
        manager.notifyAuctionFinished(auction);
        manager.notifyAuctionCancelled(auction);
        manager.notifyPriceUpdate(auction);

        assertEquals(List.of(
                AuctionEvent.AUCTION_CREATED,
                AuctionEvent.AUCTION_UPDATED,
                AuctionEvent.BID_PLACED,
                AuctionEvent.BID_PLACED,
                AuctionEvent.AUCTION_FINISHED,
                AuctionEvent.AUCTION_CANCELLED,
                AuctionEvent.PRICE_UPDATED), observer.types);
        assertSame(bid, observer.events.get(3).getBid());
        assertEquals("u0", observer.events.get(3).getPreviousHighestBidderId());
    }

    @Test
    void auctionStoreAddsGetsRemovesAndCopiesValues() throws Exception {
        auctions().clear();
        Auction a1 = auction("A1");
        Auction a2 = auction("A2");

        manager.addAuction(a1);
        manager.addAuction(a2);

        assertSame(a1, manager.getAuction("A1"));
        assertEquals(2, manager.getAllAuctions().size());
        manager.getAllAuctions().clear();
        assertEquals(2, manager.getAllAuctions().size());
        manager.removeAuction("A1");
        assertNull(manager.getAuction("A1"));
    }

    @Test
    void notifySkipsNullAuctionOrEventInternally() throws Exception {
        observers().clear();
        CaptureAuctionObserver observer = new CaptureAuctionObserver();
        manager.subscribe(observer);

        var method = AuctionManager.class.getDeclaredMethod("notifyObservers", Auction.class, AuctionEvent.class);
        method.setAccessible(true);
        method.invoke(manager, null, new AuctionEvent(AuctionEvent.AUCTION_CREATED));
        method.invoke(manager, auction("A1"), null);

        assertTrue(observer.events.isEmpty());
    }

    private static Auction auction(String id) {
        Auction auction = new Auction();
        auction.setAuctionId(id);
        return auction;
    }

    @SuppressWarnings("unchecked")
    private List<AuctionObserver> observers() throws Exception {
        Field field = AuctionManager.class.getDeclaredField("observers");
        field.setAccessible(true);
        return (List<AuctionObserver>) field.get(manager);
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Auction> auctions() throws Exception {
        Field field = AuctionManager.class.getDeclaredField("auctions");
        field.setAccessible(true);
        return (java.util.Map<String, Auction>) field.get(manager);
    }

    private static final class CaptureAuctionObserver implements AuctionObserver {
        private final List<AuctionEvent> events = new ArrayList<>();
        private final List<String> types = new ArrayList<>();

        @Override
        public void update(Auction auction, AuctionEvent event) {
            events.add(event);
            types.add(event.getType());
        }
    }
}
