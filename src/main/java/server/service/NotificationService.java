package server.service;

import server.model.Notification;
import server.model.User;
import server.observer.NotificationManager;

import java.util.List;

public class NotificationService {
    private final NotificationManager notificationManager = NotificationManager.getInstance();

    public List<Notification> getNotifications(User user) throws Exception {
        return notificationManager.getNotifications(user);
    }

    public void markAllRead(User user, Object scopeData) throws Exception {
        notificationManager.markAllRead(user, scopeData);
    }
}
