package client.service;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;
import server.model.Auction;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DashboardClientServiceTest {
    private final DashboardClientService service = new DashboardClientService();

    @Test
    void fetchAllAuctionsSendsRequestAndFiltersResponseList() throws Exception {
        Auction auction = new Auction();
        auction.setAuctionId("A1");
        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_ALL_AUCTIONS, List.of("ignore", auction), "server"));

        List<Auction> auctions = service.fetchAllAuctions(transport);

        assertEquals(List.of(auction), auctions);
        Message sent = transport.lastSent();
        assertEquals(MessageType.GET_ALL_AUCTIONS, sent.getType());
        assertEquals("client", sent.getSenderId());
        assertNull(sent.getData());
    }

    @Test
    void fetchAllAuctionsRejectsDisconnectedAndServerError() {
        assertThrows(IOException.class,
                () -> service.fetchAllAuctions(new FakeMessageTransport().disconnected()));

        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.GET_ALL_AUCTIONS, "ERROR", "cannot load"));
        IOException error = assertThrows(IOException.class,
                () -> service.fetchAllAuctions(transport));
        assertEquals("cannot load", error.getMessage());
    }

    @Test
    void extractAuctionListAcceptsSingleAuctionAndIgnoresOtherData() {
        Auction auction = new Auction();
        auction.setAuctionId("A1");

        assertEquals(List.of(auction), service.extractAuctionList(auction));
        assertEquals(List.of(auction), service.extractAuctionList(List.of("ignore", auction, 1)));
        assertTrue(service.extractAuctionList("not auctions").isEmpty());
    }
}
