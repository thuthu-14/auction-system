package server.observer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import server.model.Notification;
import server.model.RegularUser;
import server.model.User;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NotificationObserverTest {
    @AfterEach
    void restoreManager() throws Exception {
        observers().clear();
        observers().add(new SqlNotificationObserver());
    }

    @Test
    void notificationEventFactoriesAndNullListSetterWork() {
        User user = user();
        Notification notification = notification("u1", "SYSTEM");

        NotificationEvent get = NotificationEvent.get(user);
        assertEquals(NotificationEvent.GET_NOTIFICATIONS, get.getType());
        assertSame(user, get.getUser());

        NotificationEvent mark = NotificationEvent.markAllRead(user, "SELLER");
        assertEquals("SELLER", mark.getScopeData());

        NotificationEvent add = NotificationEvent.add(notification);
        assertSame(notification, add.getNotification());

        get.setNotifications(List.of(notification));
        assertEquals(1, get.getNotifications().size());
        get.setNotifications(null);
        assertTrue(get.getNotifications().isEmpty());
    }

    @Test
    void notificationManagerDelegatesToObserversAndCopiesReturnedNotifications() throws Exception {
        observers().clear();
        CaptureNotificationObserver observer = new CaptureNotificationObserver();
        observers().add(observer);
        NotificationManager manager = NotificationManager.getInstance();
        User user = user();
        Notification notification = notification("u1", "SYSTEM");

        List<Notification> result = manager.getNotifications(user);
        result.clear();
        assertEquals(1, observer.notifications.size());

        manager.markAllRead(user, "BIDDER");
        manager.addNotification(notification);

        assertEquals(List.of(NotificationEvent.GET_NOTIFICATIONS,
                NotificationEvent.MARK_ALL_READ,
                NotificationEvent.ADD_NOTIFICATION), observer.types);
        assertSame(notification, observer.added);
    }

    @Test
    void sqlNotificationObserverPrivateHelpersValidateUserAndResolveScopes() throws Exception {
        SqlNotificationObserver observer = new SqlNotificationObserver();

        User user = user();
        assertSame(user, invoke(observer, "requireUser", user));
        assertThrows(Exception.class, () -> invoke(observer, "requireUser", new Object[]{null}));

        @SuppressWarnings("unchecked")
        Set<String> seller = (Set<String>) invoke(observer, "resolveNotificationTypes", "SELLER");
        assertTrue(seller.contains("SELLER_BID"));
        @SuppressWarnings("unchecked")
        Set<String> bidder = (Set<String>) invoke(observer, "resolveNotificationTypes", "bidder");
        assertTrue(bidder.contains("OUTBID"));
        assertNull(invoke(observer, "resolveNotificationTypes", "all"));
        assertNull(invoke(observer, "resolveNotificationTypes", 123));
    }

    @Test
    void sqlNotificationObserverRejectsMissingUserAndNotificationBeforeRepositoryUse() {
        SqlNotificationObserver observer = new SqlNotificationObserver();

        assertThrows(Exception.class, () -> observer.update(NotificationEvent.get(null)));
        assertThrows(Exception.class, () -> observer.update(NotificationEvent.markAllRead(null, "SELLER")));
        assertThrows(IllegalArgumentException.class, () -> observer.update(NotificationEvent.add(null)));
        assertDoesNotThrow(() -> observer.update(newUnknownEvent()));
    }

    private static NotificationEvent newUnknownEvent() throws Exception {
        var constructor = NotificationEvent.class.getDeclaredConstructor(String.class, User.class, Object.class, Notification.class);
        constructor.setAccessible(true);
        return constructor.newInstance("UNKNOWN", null, null, null);
    }

    private static User user() {
        return new RegularUser("u1", "alice", "p", "a@example.com");
    }

    private static Notification notification(String userId, String type) {
        return new Notification(userId, type, "title", "desc", "now", "button", "ref");
    }

    private static Object invoke(Object target, String methodName, Object... args) throws Exception {
        Method method = findMethod(target.getClass(), methodName, args.length);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Method findMethod(Class<?> type, String methodName, int argCount) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == argCount) {
                return method;
            }
        }
        throw new IllegalArgumentException("Method not found: " + methodName);
    }

    @SuppressWarnings("unchecked")
    private static List<NotificationObserver> observers() throws Exception {
        NotificationManager manager = NotificationManager.getInstance();
        Field field = NotificationManager.class.getDeclaredField("observers");
        field.setAccessible(true);
        return (List<NotificationObserver>) field.get(manager);
    }

    private static final class CaptureNotificationObserver implements NotificationObserver {
        private final List<String> types = new ArrayList<>();
        private final List<Notification> notifications = new ArrayList<>();
        private Notification added;

        @Override
        public void update(NotificationEvent event) {
            types.add(event.getType());
            if (NotificationEvent.GET_NOTIFICATIONS.equals(event.getType())) {
                event.setNotifications(List.of(notification("u1", "SYSTEM")));
                notifications.addAll(event.getNotifications());
            } else if (NotificationEvent.ADD_NOTIFICATION.equals(event.getType())) {
                added = event.getNotification();
            }
        }
    }
}
