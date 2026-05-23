package client.service;

import client.network.ClientSocket;
import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;
import server.model.Notification;
import server.model.RegularUser;

import static client.service.NotificationClientService.NotificationAction.*;
import static client.service.NotificationClientService.NotificationScope.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class NotificationClientServiceTest {
    private final NotificationClientService service = new NotificationClientService();

    @Test
    void presentationControlsVisibilityAndActionsByNotificationType() {
        Notification outbid = notification("OUTBID", false);

        assertTrue(service.isVisible(outbid, BIDDER));
        assertFalse(service.isVisible(outbid, SELLER));
        assertEquals(AUCTION_DETAIL, service.actionFor(outbid, BIDDER));
        assertTrue(service.containerStyle(outbid).contains("#ef4444"));
    }

    @Test
    void readAndUnknownNotificationsUseDefaultPresentation() {
        Notification readUnknown = notification("UNKNOWN", true);

        assertTrue(service.isVisible(readUnknown, BIDDER));
        assertTrue(service.isVisible(readUnknown, SELLER));
        assertEquals(PROFILE, service.actionFor(readUnknown, BIDDER));
        assertEquals(SELLER_DASHBOARD, service.actionFor(readUnknown, SELLER));
        assertTrue(service.containerStyle(readUnknown).contains("#f3f4f6"));
    }

    @Test
    void typeMatchingIsCaseInsensitiveAndNullSafe() {
        Notification payment = notification("payment", false);

        assertEquals(WALLET, service.actionFor(payment, BIDDER));
        assertEquals(WALLET, service.actionFor(payment, SELLER));
        assertEquals(PROFILE, service.actionFor(null, BIDDER));
    }

    @Test
    void allKnownNotificationPresentationsExposeExpectedActionsAndVisibility() {
        assertEquals(WALLET, service.actionFor(notification("WIN", false), BIDDER));
        assertFalse(service.isVisible(notification("WIN", false), SELLER));

        assertEquals(PROFILE, service.actionFor(notification("SYSTEM", false), BIDDER));
        assertEquals(SELLER_MANAGE_AUCTIONS, service.actionFor(notification("SELLER_BID", false), SELLER));
        assertFalse(service.isVisible(notification("SELLER_BID", false), BIDDER));

        assertEquals(AUCTION_HISTORY, service.actionFor(notification("AUCTION_FINISHED", false), BIDDER));
        assertEquals(SELLER_MANAGE_AUCTIONS, service.actionFor(notification("AUCTION_FINISHED", false), SELLER));

        assertEquals(WALLET, service.actionFor(notification("AUCTION_CANCELLED", false), BIDDER));
        assertEquals(SELLER_MANAGE_AUCTIONS, service.actionFor(notification("SALE", false), SELLER));
    }

    @Test
    void socketRequestsSendExpectedMessagesAndValidateContext() throws Exception {
        RegularUser user = new RegularUser("U1", "user", "pw", "u@example.com");
        ClientSocket socket = connectedSocket(new Message(MessageType.GET_NOTIFICATIONS, "SUCCESS", "ok"));

        service.fetchNotifications(socket, user);

        verify(socket).sendAndReceive(argThat(message ->
                message.getType() == MessageType.GET_NOTIFICATIONS));

        ClientSocket markSocket = connectedSocket(new Message(MessageType.MARK_NOTIFICATIONS_READ, "SUCCESS", "ok"));
        service.markAllRead(markSocket, user, SELLER);
        verify(markSocket).sendAndReceive(argThat(message ->
                message.getType() == MessageType.MARK_NOTIFICATIONS_READ
                        && "SELLER".equals(message.getData())
                        && "U1".equals(message.getSenderId())));

        ClientSocket detailSocket = connectedSocket(new Message(MessageType.GET_AUCTION_DETAIL, "SUCCESS", "ok"));
        service.fetchAuctionDetail(detailSocket, "A1");
        verify(detailSocket).sendAndReceive(argThat(message ->
                message.getType() == MessageType.GET_AUCTION_DETAIL
                        && "A1".equals(message.getData())
                        && "CLIENT".equals(message.getSenderId())));

        assertThrows(IllegalStateException.class, () -> service.fetchNotifications(socket, null));

        ClientSocket disconnected = mock(ClientSocket.class);
        when(disconnected.isConnected()).thenReturn(false);
        assertThrows(java.io.IOException.class, () -> service.fetchNotifications(disconnected, user));
        assertThrows(java.io.IOException.class, () -> service.fetchAuctionDetail(disconnected, "A1"));
    }

    private Notification notification(String type, boolean read) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setRead(read);
        return notification;
    }

    private static ClientSocket connectedSocket(Message response) throws Exception {
        ClientSocket socket = mock(ClientSocket.class);
        when(socket.isConnected()).thenReturn(true);
        when(socket.sendAndReceive(any(Message.class))).thenReturn(response);
        return socket;
    }
}
