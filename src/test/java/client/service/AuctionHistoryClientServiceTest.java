package client.service;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.Bid;
import server.model.RegularUser;
import server.model.User;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuctionHistoryClientServiceTest {
    private final User user = new RegularUser("U1", "bidder", "pw", "bidder@example.com");

    @Test
    void fetchUserBidsSendsUserIdAndExtractsBidList() throws Exception {
        Bid bid = new Bid("B1", "A1", "U1", "bidder", 150.0);
        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_USER_BIDS, List.of("ignore", bid), "server"));

        List<Bid> bids = new AuctionHistoryClientService().fetchUserBids(transport, user);

        assertEquals(List.of(bid), bids);
        Message sent = transport.lastSent();
        assertEquals(MessageType.GET_USER_BIDS, sent.getType());
        assertEquals("U1", sent.getData());
        assertEquals("bidder", sent.getSenderId());
    }

    @Test
    void fetchUserBidsReturnsEmptyForMissingUserAndRejectsDisconnectedTransport() {
        AuctionHistoryClientService service = new AuctionHistoryClientService();

        assertTrue(assertDoesNotThrow(() -> service.fetchUserBids(new FakeMessageTransport(), null)).isEmpty());
        assertThrows(IOException.class,
                () -> service.fetchUserBids(new FakeMessageTransport().disconnected(), user));
    }

    @Test
    void fetchAuctionReturnsAuctionForSuccessfulResponseAndNullForInvalidInput() {
        Auction auction = new Auction();
        auction.setAuctionId("A1");
        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_AUCTION_DETAIL, auction, "server"));

        AuctionHistoryClientService service = new AuctionHistoryClientService();

        assertSame(auction, service.fetchAuction(transport, user, "A1"));
        assertNull(service.fetchAuction(transport, user, " "));
        assertNull(service.fetchAuction(transport, null, "A1"));
    }

    @Test
    void fetchAuctionReturnsNullForFailureDisconnectedOrNonAuctionData() {
        AuctionHistoryClientService service = new AuctionHistoryClientService();

        assertNull(service.fetchAuction(new FakeMessageTransport().disconnected(), user, "A1"));
        assertNull(service.fetchAuction(new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_AUCTION_DETAIL, "ERROR", "nope")), user, "A1"));
        assertNull(service.fetchAuction(new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_AUCTION_DETAIL, "not auction", "server")), user, "A1"));
    }

    @Test
    void fetchSellerContactPrefersServerMapAndFallsBackToLocalContact() {
        Auction auction = new Auction();
        auction.setSellerId("S1");
        auction.setSellerName("Seller");
        TestLocalAuctionDataService local = new TestLocalAuctionDataService();
        local.contact = Map.of("name", "Local Seller");
        AuctionHistoryClientService service = new AuctionHistoryClientService(local);

        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_SELLER_CONTACT,
                        Map.of("name", "", "email", "seller@example.com", "phone", "", "address", "HN"), "server"));

        Map<String, String> serverContact = service.fetchSellerContact(transport, user, auction);

        assertEquals("Seller", serverContact.get("name"));
        assertEquals("seller@example.com", serverContact.get("email"));
        assertEquals("Chưa cập nhật", serverContact.get("phone"));
        assertEquals("HN", serverContact.get("address"));
        assertEquals(MessageType.GET_SELLER_CONTACT, transport.lastSent().getType());

        assertEquals(local.contact, service.fetchSellerContact(new FakeMessageTransport().disconnected(), user, auction));
    }

    @Test
    void fetchSellerContactFallsBackForInvalidInputsAndServerFailures() {
        Auction auction = new Auction();
        auction.setSellerId("S1");
        auction.setSellerName("Seller");
        TestLocalAuctionDataService local = new TestLocalAuctionDataService();
        local.contact = Map.of("name", "Local Seller", "email", "local@example.com");
        AuctionHistoryClientService service = new AuctionHistoryClientService(local);

        assertEquals(local.contact, service.fetchSellerContact(null, user, auction));
        assertEquals(local.contact, service.fetchSellerContact(new FakeMessageTransport(), null, auction));
        assertNull(new AuctionHistoryClientService(new TestLocalAuctionDataService())
                .fetchSellerContact(new FakeMessageTransport(), user, null));
        assertEquals(local.contact, service.fetchSellerContact(new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_SELLER_CONTACT, "ERROR", "bad")), user, auction));
    }

    @Test
    void localDelegatesExposeStoredDataAndBidOwnership() {
        Bid bid = new Bid("B1", "A1", "U1", "bidder", 150.0);
        Auction auction = new Auction();
        auction.setAuctionId("A1");
        TestLocalAuctionDataService local = new TestLocalAuctionDataService();
        local.userBids = List.of(bid);
        local.auctionsById = Map.of("A1", auction);
        local.contact = Map.of("name", "Seller");
        AuctionHistoryClientService service = new AuctionHistoryClientService(local);

        assertEquals(List.of(bid), service.loadLocalBids(user, true));
        assertEquals(Map.of("A1", auction), service.loadLocalAuctionsById());
        assertEquals(Map.of("name", "Seller"), service.findLocalSellerContact(auction));
        assertTrue(service.isBidFromUser(bid, user));
    }

    private static final class TestLocalAuctionDataService extends LocalAuctionDataService {
        private List<Bid> userBids = List.of();
        private Map<String, Auction> auctionsById = Map.of();
        private Map<String, String> contact = null;

        @Override
        public List<Bid> loadBidsForUser(User user, boolean filterByCurrentUser) {
            return userBids;
        }

        @Override
        public Map<String, Auction> loadAuctionsById() {
            return auctionsById;
        }

        @Override
        public Map<String, String> findSellerContact(Auction auction) {
            return contact;
        }
    }
}
