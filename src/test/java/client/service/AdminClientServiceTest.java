package client.service;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.RegularUser;
import server.model.User;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdminClientServiceTest {
    private final AdminClientService service = new AdminClientService();
    private final User admin = new RegularUser("A1", "admin", "pw", "admin@example.com");

    @Test
    void fetchAllUsersFiltersNonUserData() throws Exception {
        User bidder = new RegularUser("U1", "bidder", "pw", "bidder@example.com");
        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_ALL_USERS, List.of("ignore", bidder), "server"));

        List<User> users = service.fetchAllUsers(transport, admin);

        assertEquals(List.of(bidder), users);
        assertEquals(MessageType.GET_ALL_USERS, transport.lastSent().getType());
        assertEquals("admin", transport.lastSent().getSenderId());
    }

    @Test
    void updateUserStatusSendsExpectedPayload() throws Exception {
        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.UPDATE_USER_STATUS, "SUCCESS", "ok"));

        service.updateUserStatus(transport, admin, "U1", false);

        Message sent = transport.lastSent();
        assertEquals(MessageType.UPDATE_USER_STATUS, sent.getType());
        Map<?, ?> payload = assertInstanceOf(Map.class, sent.getData());
        assertEquals("U1", payload.get("userId"));
        assertEquals(false, payload.get("active"));
    }

    @Test
    void fetchAllAuctionsFiltersNonAuctionData() throws Exception {
        Auction auction = new Auction();
        auction.setAuctionId("A1");
        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_ALL_AUCTIONS, List.of(auction, "ignore"), "server"));

        List<Auction> auctions = service.fetchAllAuctions(transport, admin);

        assertEquals(List.of(auction), auctions);
        assertEquals(MessageType.GET_ALL_AUCTIONS, transport.lastSent().getType());
    }

    @Test
    void adminRequestsRejectDisconnectedNullAndErrorResponses() {
        assertThrows(IOException.class,
                () -> service.fetchAllUsers(new FakeMessageTransport().disconnected(), admin));

        FakeMessageTransport noResponse = new FakeMessageTransport();
        IOException noResponseError = assertThrows(IOException.class,
                () -> service.fetchAllUsers(noResponse, admin));
        assertEquals("No response from server", noResponseError.getMessage());

        FakeMessageTransport serverError = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_ALL_USERS, "ERROR", "denied"));
        IOException error = assertThrows(IOException.class,
                () -> service.fetchAllUsers(serverError, admin));
        assertEquals("denied", error.getMessage());
    }
}
