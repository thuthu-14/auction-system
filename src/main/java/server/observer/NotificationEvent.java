package server.observer;

import server.model.Notification;
import server.model.User;

import java.util.ArrayList;
import java.util.List;

public class NotificationEvent {
    public static final String GET_NOTIFICATIONS = "GET_NOTIFICATIONS";
    public static final String MARK_ALL_READ = "MARK_ALL_READ";
    public static final String ADD_NOTIFICATION = "ADD_NOTIFICATION";

    private final String type;
    private final User user;
    private final Object scopeData;
    private final Notification notification;
    private List<Notification> notifications = new ArrayList<>();

    private NotificationEvent(String type, User user, Object scopeData, Notification notification) {
        this.type = type;
        this.user = user;
        this.scopeData = scopeData;
        this.notification = notification;
    }

    public static NotificationEvent get(User user) {
        return new NotificationEvent(GET_NOTIFICATIONS, user, null, null);
    }

    public static NotificationEvent markAllRead(User user, Object scopeData) {
        return new NotificationEvent(MARK_ALL_READ, user, scopeData, null);
    }

    public static NotificationEvent add(Notification notification) {
        return new NotificationEvent(ADD_NOTIFICATION, null, null, notification);
    }

    public String getType() {
        return type;
    }

    public User getUser() {
        return user;
    }

    public Object getScopeData() {
        return scopeData;
    }

    public Notification getNotification() {
        return notification;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications != null ? notifications : new ArrayList<>();
    }
}
