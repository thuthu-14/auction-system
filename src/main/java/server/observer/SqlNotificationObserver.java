package server.observer;

import server.model.Notification;
import server.model.User;
import server.repository.SqlNotificationRepository;

import java.util.Set;

public class SqlNotificationObserver implements NotificationObserver {
    private final SqlNotificationRepository notificationRepository = new SqlNotificationRepository();

    @Override
    public void update(NotificationEvent event) throws Exception {
        switch (event.getType()) {
            case NotificationEvent.GET_NOTIFICATIONS -> {
                User user = requireUser(event.getUser());
                event.setNotifications(notificationRepository.getNotificationsByUser(user.getUserId()));
            }
            case NotificationEvent.MARK_ALL_READ -> {
                User user = requireUser(event.getUser());
                notificationRepository.markAllAsRead(user.getUserId(), resolveNotificationTypes(event.getScopeData()));
            }
            case NotificationEvent.ADD_NOTIFICATION -> {
                Notification notification = event.getNotification();
                if (notification == null) {
                    throw new IllegalArgumentException("Notification is required");
                }
                notificationRepository.addNotification(notification);
            }
            default -> {
            }
        }
    }

    private User requireUser(User user) {
        if (user == null) {
            throw new IllegalStateException("Bạn phải đăng nhập");
        }
        return user;
    }

    private Set<String> resolveNotificationTypes(Object data) {
        if (!(data instanceof String scope)) {
            return null;
        }

        return switch (scope.toUpperCase()) {
            case "SELLER" -> Set.of("SELLER_BID", "AUCTION_FINISHED", "SALE", "PAYMENT");
            case "BIDDER" -> Set.of("WIN", "OUTBID", "SYSTEM", "PAYMENT", "AUCTION_FINISHED", "AUCTION_CANCELLED");
            default -> null;
        };
    }
}
