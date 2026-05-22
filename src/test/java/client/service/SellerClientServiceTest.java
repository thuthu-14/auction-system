package client.service;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.Bid;
import server.model.RegularUser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SellerClientServiceTest {
    private final SellerClientService service = new SellerClientService();
    private final RegularUser user = new RegularUser("U1", "seller", "pw", "seller@example.com");

    @Test
    void upgradeSellerSendsShopPayload() throws Exception {
        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.UPGRADE_SELLER, "SUCCESS", "ok"));

        service.upgradeSeller(transport, user, "Shop", "090", "HN", "shop@example.com", "secret");

        Message sent = transport.lastSent();
        assertEquals(MessageType.UPGRADE_SELLER, sent.getType());
        assertEquals("seller", sent.getSenderId());
        Map<?, ?> payload = assertInstanceOf(Map.class, sent.getData());
        assertEquals("U1", payload.get("userId"));
        assertEquals("Shop", payload.get("shopName"));
        assertEquals("090", payload.get("phone"));
        assertEquals("HN", payload.get("address"));
        assertEquals("shop@example.com", payload.get("email"));
        assertEquals("secret", payload.get("password"));
    }

    @Test
    void upgradeSellerRejectsDisconnectedTransportAndServerFailure() {
        assertThrows(IOException.class,
                () -> service.upgradeSeller(new FakeMessageTransport().disconnected(),
                        user, "Shop", "090", "HN", "shop@example.com", "secret"));

        FakeMessageTransport failingTransport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.UPGRADE_SELLER, "ERROR", "bad password"));
        IOException error = assertThrows(IOException.class,
                () -> service.upgradeSeller(failingTransport, user, "Shop", "090", "HN", "shop@example.com", "wrong"));
        assertEquals("bad password", error.getMessage());

        FakeMessageTransport nullResponse = new FakeMessageTransport();
        IOException defaultError = assertThrows(IOException.class,
                () -> service.upgradeSeller(nullResponse, user, "Shop", "090", "HN", "shop@example.com", "secret"));
        assertEquals("Upgrade seller failed", defaultError.getMessage());
    }

    @Test
    void fetchSellerAuctionsExtractsAuctionListAndValidatesContext() throws Exception {
        Auction auction = new Auction();
        auction.setAuctionId("A1");
        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_SELLER_AUCTIONS, List.of("ignore", auction), "server"));

        assertEquals(List.of(auction), service.fetchSellerAuctions(transport, user));
        assertEquals(MessageType.GET_SELLER_AUCTIONS, transport.lastSent().getType());
        assertNull(transport.lastSent().getSenderId());

        assertThrows(IllegalStateException.class,
                () -> service.fetchSellerAuctions(new FakeMessageTransport(), null));
        IOException error = assertThrows(IOException.class,
                () -> service.fetchSellerAuctions(new FakeMessageTransport(), user));
        assertEquals("Cannot load seller auctions", error.getMessage());
    }

    @Test
    void fetchAuctionDetailAcceptsSingleAuctionResponse() throws Exception {
        Auction auction = new Auction();
        auction.setAuctionId("A1");
        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_AUCTION_DETAIL, auction, "server"));

        Auction actual = service.fetchAuctionDetail(transport, user, "A1");

        assertSame(auction, actual);
        Message sent = transport.lastSent();
        assertEquals(MessageType.GET_AUCTION_DETAIL, sent.getType());
        assertEquals("A1", sent.getData());
        assertEquals("seller", sent.getSenderId());
    }

    @Test
    void fetchAuctionDetailAcceptsListResponseAndReturnsNullForInvalidData() throws Exception {
        Auction auction = new Auction();
        auction.setAuctionId("A1");
        FakeMessageTransport listTransport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_AUCTION_DETAIL, List.of("ignore", auction), "server"));
        assertSame(auction, service.fetchAuctionDetail(listTransport, user, "A1"));

        FakeMessageTransport invalidTransport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_AUCTION_DETAIL, (Object) "not auction", "server"));
        assertNull(service.fetchAuctionDetail(invalidTransport, user, "A1"));

        assertThrows(IllegalStateException.class,
                () -> service.fetchAuctionDetail(new FakeMessageTransport(), null, "A1"));
        assertThrows(IOException.class,
                () -> service.fetchAuctionDetail(new FakeMessageTransport().disconnected(), user, "A1"));
    }

    @Test
    void fetchBidHistoryAcceptsSingleBidOrList() throws Exception {
        Bid single = new Bid("B1", "A1", "U2", "bidder", 200.0);
        FakeMessageTransport singleTransport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_BID_HISTORY, single, "server"));

        assertEquals(List.of(single), service.fetchBidHistory(singleTransport, user, "A1"));

        Bid second = new Bid("B2", "A1", "U3", "other", 250.0);
        FakeMessageTransport listTransport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_BID_HISTORY, List.of(single, "ignore", second), "server"));

        assertEquals(List.of(single, second), service.fetchBidHistory(listTransport, user, "A1"));
    }

    @Test
    void fetchBidHistoryFallbackDelegatesAndLocalHistoryIsEmpty() throws Exception {
        Bid bid = new Bid("B1", "A1", "U2", "bidder", 200.0);
        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_BID_HISTORY, bid, "server"));

        assertEquals(List.of(bid), service.fetchBidHistoryWithFallback(transport, user, "A1"));
        assertTrue(service.loadLocalBidHistory("A1").isEmpty());
    }

    @Test
    void fetchBidHistoryRejectsInvalidContextAndServerFailure() {
        assertThrows(IllegalStateException.class,
                () -> service.fetchBidHistory(new FakeMessageTransport(), null, "A1"));
        assertThrows(IllegalArgumentException.class,
                () -> service.fetchBidHistory(new FakeMessageTransport(), user, " "));

        FakeMessageTransport failing = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_BID_HISTORY, "ERROR", "cannot load"));
        IOException error = assertThrows(IOException.class,
                () -> service.fetchBidHistory(failing, user, "A1"));
        assertEquals("cannot load", error.getMessage());
    }

    @Test
    void cancelAuctionValidatesAuctionIdAndPropagatesServerError() {
        FakeMessageTransport transport = new FakeMessageTransport();
        assertThrows(IllegalArgumentException.class,
                () -> service.cancelAuction(transport, user, " "));
        assertEquals(0, transport.sentCount());

        FakeMessageTransport failingTransport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.CANCEL_AUCTION, "ERROR", "cannot cancel"));
        IOException error = assertThrows(IOException.class,
                () -> service.cancelAuction(failingTransport, user, "A1"));
        assertEquals("cannot cancel", error.getMessage());
    }

    @Test
    void cancelAuctionReturnsAuctionOrNullForSuccessfulResponse() throws Exception {
        Auction auction = new Auction();
        auction.setAuctionId("A1");
        FakeMessageTransport auctionTransport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.CANCEL_AUCTION, auction, "server"));
        assertSame(auction, service.cancelAuction(auctionTransport, user, "A1"));

        FakeMessageTransport nullDataTransport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.CANCEL_AUCTION, "SUCCESS", "ok"));
        assertNull(service.cancelAuction(nullDataTransport, user, "A1"));

        assertThrows(IllegalStateException.class,
                () -> service.cancelAuction(new FakeMessageTransport(), null, "A1"));
        assertThrows(IllegalArgumentException.class,
                () -> service.cancelAuction(new FakeMessageTransport(), user, "--"));
    }

    @Test
    void fetchUserContactReturnsStringMapAndSkipsBlankUserId() throws Exception {
        assertTrue(service.fetchUserContact(new FakeMessageTransport(), user, "").isEmpty());

        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_SELLER_CONTACT,
                        Map.of("email", "seller@example.com", "phone", 123), "server"));

        Map<String, String> contact = service.fetchUserContact(transport, user, "U1");

        assertEquals("seller@example.com", contact.get("email"));
        assertEquals("123", contact.get("phone"));
        assertEquals(MessageType.GET_SELLER_CONTACT, transport.lastSent().getType());
    }

    @Test
    void fetchUserContactHandlesInvalidDataAndErrors() throws Exception {
        FakeMessageTransport invalidData = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_SELLER_CONTACT, (Object) "not map", "server"));
        assertTrue(service.fetchUserContact(invalidData, user, "U1").isEmpty());

        assertThrows(IllegalStateException.class,
                () -> service.fetchUserContact(new FakeMessageTransport(), null, "U1"));

        FakeMessageTransport failing = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_SELLER_CONTACT, "ERROR", "no contact"));
        IOException error = assertThrows(IOException.class,
                () -> service.fetchUserContact(failing, user, "U1"));
        assertEquals("no contact", error.getMessage());
    }
}
