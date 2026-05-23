package client.controller.auctiondetail;

import client.network.AuctionListener;
import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.Bid;

import static org.junit.jupiter.api.Assertions.*;

class AuctionRealtimeDispatcherTest {
    @Test
    void dispatchRoutesRealtimeBidAndPriceMessagesByPayloadType() {
        RecordingAuctionListener listener = new RecordingAuctionListener();
        AuctionRealtimeDispatcher dispatcher = new AuctionRealtimeDispatcher(listener);
        Bid bid = new Bid("B1", "A1", "U1", "bidder", 100.0);
        Auction auction = new Auction();
        auction.setAuctionId("A1");

        dispatcher.dispatch(new Message(MessageType.UPDATE_PRICE_REALTIME, bid, "server"));
        dispatcher.dispatch(new Message(MessageType.UPDATE_PRICE_REALTIME, auction, "server"));

        assertSame(bid, listener.bidPlaced);
        assertSame(auction, listener.priceUpdated);
    }

    @Test
    void dispatchRoutesAuctionLifecycleMessages() {
        RecordingAuctionListener listener = new RecordingAuctionListener();
        AuctionRealtimeDispatcher dispatcher = new AuctionRealtimeDispatcher(listener);
        Auction auction = new Auction();
        auction.setAuctionId("A1");

        dispatcher.dispatch(new Message(MessageType.UPDATE, auction, "server"));
        dispatcher.dispatch(new Message(MessageType.AUCTION_FINISHED_NOTIFICATION, auction, "server"));
        dispatcher.dispatch(new Message(MessageType.OUTBID_NOTIFICATION, auction, "server"));
        dispatcher.dispatch(new Message(MessageType.AUCTION_EXTENDED_NOTIFICATION, auction, "server"));

        assertSame(auction, listener.auctionUpdated);
        assertSame(auction, listener.auctionFinished);
        assertSame(auction, listener.outbid);
        assertSame(auction, listener.auctionExtended);
    }

    @Test
    void dispatchRoutesTextMessagesAndIgnoresInvalidInputs() {
        RecordingAuctionListener listener = new RecordingAuctionListener();
        AuctionRealtimeDispatcher dispatcher = new AuctionRealtimeDispatcher(listener);
        Message notification = new Message(MessageType.NOTIFICATION, (Object) null, "server");
        notification.setMessage("new event");
        Message error = new Message(MessageType.ERROR, (Object) null, "server");
        error.setMessage("failed");

        dispatcher.dispatch(null);
        dispatcher.dispatch(new Message(null, (Object) null, "server"));
        dispatcher.dispatch(notification);
        dispatcher.dispatch(error);

        assertEquals("new event", listener.notification);
        assertEquals("failed", listener.error);
    }

    private static final class RecordingAuctionListener implements AuctionListener {
        private Auction auctionUpdated;
        private Bid bidPlaced;
        private Auction priceUpdated;
        private Auction auctionFinished;
        private String error;
        private String notification;
        private Auction outbid;
        private Auction auctionExtended;

        @Override
        public void onAuctionUpdated(Auction auction) {
            auctionUpdated = auction;
        }

        @Override
        public void onBidPlaced(Bid bid) {
            bidPlaced = bid;
        }

        @Override
        public void onPriceUpdated(Auction auction) {
            priceUpdated = auction;
        }

        @Override
        public void onAuctionFinished(Auction auction) {
            auctionFinished = auction;
        }

        @Override
        public void onError(String errorMessage) {
            error = errorMessage;
        }

        @Override
        public void onNotification(String message) {
            notification = message;
        }

        @Override
        public void onOutbid(Auction auction) {
            outbid = auction;
        }

        @Override
        public void onAuctionExtended(Auction auction) {
            auctionExtended = auction;
        }
    }
}
