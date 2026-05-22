package server.handler;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;
import server.AuctionServer;
import server.ClientHandler;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientMessageProcessorTest {

    @Test
    void constructorGetCurrentUserAndInvalidRequestsCoverDelegatesWithoutDatabaseWork() throws Exception {
        ClientHandler client = mock(ClientHandler.class);
        AuctionServer server = mock(AuctionServer.class);
        ClientMessageProcessor processor = new ClientMessageProcessor(client, server);

        assertNull(processor.getCurrentUser());

        processor.handleGetAuctionDetail(new Message(MessageType.GET_AUCTION_DETAIL, (Object) "", "client"));
        processor.handlePlaceBid(new Message(MessageType.PLACE_BID, Map.of(), "client"));
        processor.handleGetBidHistory(new Message(MessageType.GET_BID_HISTORY, (Object) "", "client"));
        processor.handleGetUserBids(new Message(MessageType.GET_USER_BIDS, (Object) null, "client"));
        processor.handleUploadImage(new Message(MessageType.UPLOAD_IMAGE, Map.of(), "client"));
        processor.handleDownloadImage(new Message(MessageType.DOWNLOAD_IMAGE, Map.of(), "client"));
        processor.handleGetNotifications(new Message(MessageType.GET_NOTIFICATIONS, (Object) null, "client"));
        processor.handleMarkNotificationsRead(new Message(MessageType.MARK_NOTIFICATIONS_READ, (Object) null, "client"));

        verify(client, atLeast(6)).sendError(anyString());
    }
}
