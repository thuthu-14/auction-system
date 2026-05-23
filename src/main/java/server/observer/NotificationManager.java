package server.observer;

import server.model.Notification;
import server.model.User;
import util.LoggerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {
    private static NotificationManager instance;
    private final List<NotificationObserver> observers = new CopyOnWriteArrayList<>();

    private NotificationManager() {
        subscribe(new SqlNotificationObserver());
    }

    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    public void subscribe(NotificationObserver observer) {
        if (observer != null && observers.stream().noneMatch(existing -> existing.getClass().equals(observer.getClass()))) {
            observers.add(observer);
            LoggerUtil.debug("Notification observer subscribed. Total observers: " + observers.size());
        }
    }

    public List<Notification> getNotifications(User user) throws Exception {
        NotificationEvent event = NotificationEvent.get(user);
        notifyObservers(event);
        return new ArrayList<>(event.getNotifications());
    }

    public void markAllRead(User user, Object scopeData) throws Exception {
        notifyObservers(NotificationEvent.markAllRead(user, scopeData));
    }

    public void addNotification(Notification notification) throws Exception {
        notifyObservers(NotificationEvent.add(notification));
    }

    private void notifyObservers(NotificationEvent event) throws Exception {
        for (NotificationObserver observer : observers) {
            observer.update(event);
        }
    }
}
