package server.handler;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import server.model.Notification;
import server.model.User;
import server.service.NotificationService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationMessageProcessorTest {

    @Mock
    ClientSessionContext context;

    @Mock
    NotificationService notificationService;

    private AutoCloseable mocksClose;

    @BeforeEach
    void init() {
        mocksClose = MockitoAnnotations.openMocks(this);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception {
        if (mocksClose != null) {
            mocksClose.close();
        }
    }

    @Test
    void handleGetNotificationsSendsArrayListCopy() throws Exception {
        NotificationMessageProcessor processor = new NotificationMessageProcessor(context, notificationService);
        User user = mock(User.class);
        Notification notification = new Notification();
        notification.setId("N1");
        when(context.getCurrentUser()).thenReturn(user);
        when(notificationService.getNotifications(user)).thenReturn(List.of(notification));

        processor.handleGetNotifications(new Message(MessageType.GET_NOTIFICATIONS, (Object) null, "client"));

        ArgumentCaptor<Message> responseCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context).sendMessage(responseCaptor.capture());
        Message response = responseCaptor.getValue();
        assertEquals(MessageType.GET_NOTIFICATIONS, response.getType());
        assertTrue(response.getData() instanceof ArrayList<?>);
        assertEquals(List.of(notification), response.getData());
    }

    @Test
    void handleMarkNotificationsReadSendsSuccess() throws Exception {
        NotificationMessageProcessor processor = new NotificationMessageProcessor(context, notificationService);
        User user = mock(User.class);
        when(context.getCurrentUser()).thenReturn(user);

        processor.handleMarkNotificationsRead(new Message(MessageType.MARK_NOTIFICATIONS_READ, (Object) "seller", "client"));

        verify(notificationService).markAllRead(user, "seller");
        ArgumentCaptor<Message> responseCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context).sendMessage(responseCaptor.capture());
        assertEquals(MessageType.MARK_NOTIFICATIONS_READ, responseCaptor.getValue().getType());
        assertEquals("SUCCESS", responseCaptor.getValue().getStatus());
    }

    @Test
    void serviceErrorsSendError() throws Exception {
        NotificationMessageProcessor processor = new NotificationMessageProcessor(context, notificationService);
        when(notificationService.getNotifications(any())).thenThrow(new IllegalStateException("login required"));

        processor.handleGetNotifications(new Message(MessageType.GET_NOTIFICATIONS, (Object) null, "client"));

        verify(context).sendError("login required");
        verify(context, never()).sendMessage(any());
    }
}
