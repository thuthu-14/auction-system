package server.observer;

public interface NotificationObserver {
    void update(NotificationEvent event) throws Exception;
}
