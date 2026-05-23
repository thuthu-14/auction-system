package server.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void constructorInitializesFieldsAndUnreadState() {
        Notification notification = new Notification(
                "u1", "WIN", "Title", "Description", "fallback", "Pay", "A1");

        assertNotNull(notification.getId());
        assertEquals("u1", notification.getUserId());
        assertEquals("WIN", notification.getType());
        assertEquals("Title", notification.getTitle());
        assertEquals("Description", notification.getDescription());
        assertEquals("Pay", notification.getButtonText());
        assertEquals("A1", notification.getReferenceId());
        assertFalse(notification.isRead());
        assertEquals("Vừa xong", notification.getTimeAgo());
    }

    @Test
    void settersUpdateAllMutableFields() {
        Notification notification = new Notification();

        notification.setId("N1");
        notification.setUserId("u1");
        notification.setType("SYSTEM");
        notification.setTitle("Title");
        notification.setDescription("Description");
        notification.setTimeAgo("old");
        notification.setCreatedAt(0);
        notification.setButtonText("Open");
        notification.setReferenceId("REF");
        notification.setRead(true);

        assertEquals("N1", notification.getId());
        assertEquals("u1", notification.getUserId());
        assertEquals("SYSTEM", notification.getType());
        assertEquals("Title", notification.getTitle());
        assertEquals("Description", notification.getDescription());
        assertEquals("old", notification.getTimeAgo());
        assertEquals("Open", notification.getButtonText());
        assertEquals("REF", notification.getReferenceId());
        assertTrue(notification.isRead());
    }

    @Test
    void formatTimeAgoCoversTimeBuckets() {
        Notification notification = new Notification();
        long now = System.currentTimeMillis();

        notification.setCreatedAt(now - 30_000L);
        assertEquals("Vừa xong", notification.formatTimeAgo());
        notification.setCreatedAt(now - 5 * 60_000L);
        assertEquals("5 phút trước", notification.formatTimeAgo());
        notification.setCreatedAt(now - 2 * 60 * 60_000L);
        assertEquals("2 giờ trước", notification.formatTimeAgo());
        notification.setCreatedAt(now - 3L * 24 * 60 * 60_000L);
        assertEquals("3 ngày trước", notification.formatTimeAgo());
        notification.setCreatedAt(now - 2L * 30 * 24 * 60 * 60_000L);
        assertEquals("2 tháng trước", notification.formatTimeAgo());
        notification.setCreatedAt(now - 13L * 30 * 24 * 60 * 60_000L);
        assertEquals("1 năm trước", notification.formatTimeAgo());
    }
}
