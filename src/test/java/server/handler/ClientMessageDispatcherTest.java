package server.handler;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientMessageDispatcherTest {
    @Test
    void dispatchRoutesKnownMessageTypesToProcessorMethods() throws Exception {
        ClientMessageProcessor processor = mock(ClientMessageProcessor.class);
        ClientMessageDispatcher dispatcher = new ClientMessageDispatcher(processor);

        Message login = new Message(MessageType.LOGIN, (Object) null, "client");
        Message placeBid = new Message(MessageType.PLACE_BID, (Object) null, "client");
        Message linkBank = new Message(MessageType.LINK_BANK, (Object) null, "client");
        Message notifications = new Message(MessageType.GET_NOTIFICATIONS, (Object) null, "client");
        Message upload = new Message(MessageType.UPLOAD_IMAGE, (Object) null, "client");
        Message download = new Message(MessageType.DOWNLOAD_IMAGE, (Object) null, "client");

        dispatcher.dispatch(login);
        dispatcher.dispatch(placeBid);
        dispatcher.dispatch(linkBank);
        dispatcher.dispatch(notifications);
        dispatcher.dispatch(upload);
        dispatcher.dispatch(download);

        verify(processor).handleLogin(login);
        verify(processor).handlePlaceBid(placeBid);
        verify(processor).handleLinkBank(linkBank);
        verify(processor).handleGetNotifications(notifications);
        verify(processor).handleUploadImage(upload);
        verify(processor).handleDownloadImage(download);
    }

    @Test
    void dispatchThrowsForUnregisteredMessageType() {
        ClientMessageProcessor processor = mock(ClientMessageProcessor.class);
        ClientMessageDispatcher dispatcher = new ClientMessageDispatcher(processor);
        Message logout = new Message(MessageType.LOGOUT, (Object) null, "client");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> dispatcher.dispatch(logout));

        assertEquals("Unknown message type: LOGOUT", error.getMessage());
    }
}
