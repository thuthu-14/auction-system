package server.repository;

import common.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.model.Notification;
import server.storage.DatabaseManager;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SqlSimpleRepositoryTest {
    @BeforeEach
    void resetTables() throws Exception {
        try (Connection c = DatabaseManager.getConnection();
             Statement st = c.createStatement()) {
            st.executeUpdate("DROP TABLE IF EXISTS bank_accounts");
            st.executeUpdate("DROP TABLE IF EXISTS transactions");
            st.executeUpdate("DROP TABLE IF EXISTS notifications");
            st.executeUpdate("DROP TABLE IF EXISTS item_images");
            st.executeUpdate("""
                    CREATE TABLE bank_accounts (
                        user_id TEXT PRIMARY KEY,
                        username TEXT,
                        email TEXT,
                        bank_name TEXT,
                        account_number TEXT,
                        initial_balance REAL,
                        created_at INTEGER
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE transactions (
                        id TEXT PRIMARY KEY,
                        user_id TEXT,
                        type TEXT,
                        amount REAL,
                        balance_after REAL,
                        description TEXT,
                        timestamp INTEGER
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE notifications (
                        notification_id TEXT PRIMARY KEY,
                        user_id TEXT,
                        type TEXT,
                        title TEXT,
                        description TEXT,
                        created_at INTEGER,
                        time_ago TEXT,
                        button_text TEXT,
                        reference_id TEXT,
                        is_read INTEGER
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE item_images (
                        image_id TEXT PRIMARY KEY,
                        item_id TEXT,
                        image_data BLOB,
                        image_size INTEGER,
                        image_type TEXT,
                        created_at INTEGER,
                        sort_index INTEGER DEFAULT 0
                    )
                    """);
        }
    }

    @Test
    void bankAccountRepositorySavesReplacesAndLoadsAccount() throws Exception {
        SqlBankAccountRepository repository = new SqlBankAccountRepository();

        assertFalse(repository.hasBankAccount("U1"));
        assertNull(repository.getBankAccount("U1"));

        repository.saveBankAccount("U1", "user", "user@example.com", "VCB", "001", 1000.0);
        assertTrue(repository.hasBankAccount("U1"));
        assertEquals(Map.of("bankName", "VCB", "accountNumber", "001"), repository.getBankAccount("U1"));

        repository.saveBankAccount("U1", "user", "user@example.com", "ACB", "002", 2000.0);
        assertEquals(Map.of("bankName", "ACB", "accountNumber", "002"), repository.getBankAccount("U1"));
    }

    @Test
    void transactionRepositoryReturnsTransactionsNewestFirst() throws Exception {
        SqlTransactionRepository repository = new SqlTransactionRepository();
        Transaction older = transaction("T1", "U1", "DEPOSIT", 100.0, 100.0, 10);
        Transaction newer = transaction("T2", "U1", "WITHDRAW", 40.0, 60.0, 20);
        Transaction otherUser = transaction("T3", "U2", "DEPOSIT", 1.0, 1.0, 30);

        repository.saveTransaction(older);
        repository.saveTransaction(newer);
        repository.saveTransaction(otherUser);

        List<Transaction> transactions = repository.getByUser("U1");

        assertEquals(List.of("T2", "T1"), transactions.stream().map(t -> t.id).toList());
        assertEquals("WITHDRAW", transactions.get(0).type);
        assertEquals(60.0, transactions.get(0).balanceAfter);
    }

    @Test
    void notificationRepositoryAddsLoadsAndMarksAllowedTypesRead() throws Exception {
        SqlNotificationRepository repository = new SqlNotificationRepository();
        Notification win = notification("N1", "U1", "WIN", false, 10);
        Notification outbid = notification("N2", "U1", "OUTBID", false, 20);
        Notification otherUser = notification("N3", "U2", "WIN", false, 30);

        repository.addNotification(win);
        repository.addNotification(outbid);
        repository.addNotification(otherUser);

        List<Notification> before = repository.getNotificationsByUser("U1");
        assertEquals(List.of("N2", "N1"), before.stream().map(Notification::getId).toList());

        repository.markAllAsRead("U1", Set.of("WIN"));

        List<Notification> after = repository.getNotificationsByUser("U1");
        Notification loadedOutbid = after.stream().filter(n -> "N2".equals(n.getId())).findFirst().orElseThrow();
        Notification loadedWin = after.stream().filter(n -> "N1".equals(n.getId())).findFirst().orElseThrow();
        assertFalse(loadedOutbid.isRead());
        assertTrue(loadedWin.isRead());
    }

    @Test
    void imageRepositoryStoresLoadsLinksAndDeletesImages() throws Exception {
        String imageId = ImageRepository.saveImage("ITEM1", new byte[]{1, 2, 3});

        assertArrayEquals(new byte[]{1, 2, 3}, ImageRepository.getImage(imageId));
        assertEquals(List.of(imageId), ImageRepository.getItemImages("ITEM1"));
        assertEquals(1, ImageRepository.getStorageInfo().get("imageCount"));
        assertEquals(3L, ImageRepository.getStorageInfo().get("totalBytes"));

        ImageRepository.linkImageToItem(imageId, "ITEM2", 2);
        ImageRepository.updateImageSortIndex(imageId, 1);
        assertTrue(ImageRepository.getItemImages("ITEM1").isEmpty());
        assertEquals(List.of(imageId), ImageRepository.getItemImages("ITEM2"));

        ImageRepository.deleteImage(imageId);
        assertTrue(ImageRepository.getItemImages("ITEM2").isEmpty());
    }

    private Transaction transaction(String id, String userId, String type, double amount,
                                    double balanceAfter, long timestamp) {
        Transaction transaction = new Transaction();
        transaction.id = id;
        transaction.userId = userId;
        transaction.type = type;
        transaction.amount = amount;
        transaction.balanceAfter = balanceAfter;
        transaction.description = type;
        transaction.timestamp = timestamp;
        return transaction;
    }

    private Notification notification(String id, String userId, String type, boolean read, long createdAt) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(type + " title");
        notification.setDescription(type + " desc");
        notification.setTimeAgo("now");
        notification.setButtonText("Open");
        notification.setReferenceId("A1");
        notification.setRead(read);
        notification.setCreatedAt(createdAt);
        return notification;
    }
}
