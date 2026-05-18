package server.handler;

import common.Message;
import common.MessageType;
import server.model.Notification;
import server.service.NotificationService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NotificationMessageProcessor {
    private final ClientSessionContext context;
    private final NotificationService notificationService = new NotificationService();

    public NotificationMessageProcessor(ClientSessionContext context) {
        this.context = context;
    }

    public void handleGetNotifications(Message message) throws IOException {
        try {
            List<Notification> list = notificationService.getNotifications(context.getCurrentUser());
            Message response = new Message(MessageType.GET_NOTIFICATIONS, new ArrayList<>(list), "SERVER");
            context.sendMessage(response);
        } catch (Exception e) {
            context.sendError(e.getMessage());
        }
    }

    public void handleMarkNotificationsRead(Message message) throws IOException {
        try {
            notificationService.markAllRead(context.getCurrentUser(), message.getData());
            Message response = new Message(MessageType.MARK_NOTIFICATIONS_READ, "SUCCESS", "Da doc tat ca");
            context.sendMessage(response);
        } catch (Exception e) {
            context.sendError(e.getMessage());
        }
    }
}
