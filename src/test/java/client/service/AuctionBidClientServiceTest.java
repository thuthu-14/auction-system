package client.service;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;
import server.model.Bid;
import server.model.RegularUser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuctionBidClientServiceTest {
    private final AuctionBidClientService service = new AuctionBidClientService();
    private final RegularUser user = new RegularUser("U1", "bidder", "pw", "bidder@example.com");

    @Test
    void placeBidSendsAuctionAndAmountPayload() throws Exception {
        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.PLACE_BID, "SUCCESS", "ok"));

        Message response = service.placeBid(transport, user, "A1", 150.0);

        assertEquals("ok", response.getMessage());
        Message sent = transport.lastSent();
        assertEquals(MessageType.PLACE_BID, sent.getType());
        assertEquals("bidder", sent.getSenderId());
        Map<?, ?> payload = assertInstanceOf(Map.class, sent.getData());
        assertEquals("A1", payload.get("auctionId"));
        assertEquals(150.0, payload.get("amount"));
    }

    @Test
    void placeBidRejectsMissingConnectionAndUserBeforeSending() {
        assertThrows(IOException.class,
                () -> service.placeBid(new FakeMessageTransport().disconnected(), user, "A1", 150.0));

        FakeMessageTransport transport = new FakeMessageTransport();
        assertThrows(IllegalStateException.class,
                () -> service.placeBid(transport, null, "A1", 150.0));
        assertEquals(0, transport.sentCount());
    }

    @Test
    void fetchBidHistoryUsesClientSenderWhenUserMissing() throws Exception {
        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_BID_HISTORY, List.of(), "server"));

        service.fetchBidHistory(transport, null, "A1");

        Message sent = transport.lastSent();
        assertEquals(MessageType.GET_BID_HISTORY, sent.getType());
        assertEquals("A1", sent.getData());
        assertEquals("CLIENT", sent.getSenderId());
    }

    @Test
    void extractBidListKeepsOnlyBidObjects() {
        Bid bid = new Bid("B1", "A1", "U1", "bidder", 150.0);

        List<Bid> bids = service.extractBidList(List.of("ignore", bid, 123));

        assertEquals(List.of(bid), bids);
        assertTrue(service.extractBidList("not a list").isEmpty());
    }

    @Test
    void fetchBidHistoryWithFallbackUsesRemoteListOrLocalFallback() throws Exception {
        Bid bid = new Bid("B1", "A1", "U1", "bidder", 150.0);
        TestLocalAuctionDataService local = new TestLocalAuctionDataService();
        local.bids = List.of(new Bid("LOCAL", "A1", "U2", "local", 90.0));
        AuctionBidClientService serviceWithLocal = new AuctionBidClientService(local);

        FakeMessageTransport remote = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_BID_HISTORY, List.of(bid), "server"));
        assertEquals(List.of(bid), serviceWithLocal.fetchBidHistoryWithFallback(remote, user, "A1"));

        FakeMessageTransport emptyRemote = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_BID_HISTORY, List.of(), "server"));
        assertEquals(local.bids, serviceWithLocal.fetchBidHistoryWithFallback(emptyRemote, user, "A1"));

        FakeMessageTransport errorRemote = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_BID_HISTORY, "ERROR", "cannot load"));
        assertEquals(local.bids, serviceWithLocal.fetchBidHistoryWithFallback(errorRemote, user, "A1"));
        assertEquals(local.bids, serviceWithLocal.fetchBidHistoryWithFallback(new FakeMessageTransport().disconnected(), user, "A1"));
        assertEquals(local.bids, serviceWithLocal.loadLocalBidHistory("A1"));
    }

    private static final class TestLocalAuctionDataService extends LocalAuctionDataService {
        private List<Bid> bids = List.of();

        @Override
        public List<Bid> loadBidsForAuction(String auctionId) {
            return bids;
        }
    }
}
