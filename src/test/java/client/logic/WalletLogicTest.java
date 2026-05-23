package client.logic;

import common.Transaction;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WalletLogicTest {

    private final WalletLogic logic = new WalletLogic();

    @Test
    void filterTransactionsFiltersByMoneyInOutAndKeyword() {
        long now = millis(2026, Calendar.MAY, 20);
        Transaction deposit = transaction("T1", "DEPOSIT", 100, now, "Nap tien vao vi");
        Transaction withdraw = transaction("T2", "WITHDRAW", 50, now, "Rut ve ngan hang");
        Transaction payment = transaction("T3", "PAYMENT", 25, now, "Thanh toan dau gia");

        assertEquals(List.of(deposit), logic.filterTransactions(
                List.of(deposit, withdraw, payment),
                WalletLogic.TransactionFilter.IN,
                3,
                "",
                now));

        assertEquals(List.of(withdraw, payment), logic.filterTransactions(
                List.of(deposit, withdraw, payment),
                WalletLogic.TransactionFilter.OUT,
                3,
                "",
                now));

        assertEquals(List.of(payment), logic.filterTransactions(
                List.of(deposit, withdraw, payment),
                WalletLogic.TransactionFilter.ALL,
                3,
                "dau gia",
                now));
    }

    @Test
    void filterTransactionsAppliesTimeBuckets() {
        long now = millis(2026, Calendar.MAY, 20);
        Transaction thisMonth = transaction("T1", "DEPOSIT", 100, millis(2026, Calendar.MAY, 1), "this month");
        Transaction lastMonth = transaction("T2", "DEPOSIT", 100, millis(2026, Calendar.APRIL, 15), "last month");
        Transaction withinThreeMonths = transaction("T3", "DEPOSIT", 100, millis(2026, Calendar.MARCH, 20), "three");
        Transaction tooOld = transaction("T4", "DEPOSIT", 100, millis(2026, Calendar.JANUARY, 1), "old");

        assertEquals(List.of(thisMonth), logic.filterTransactions(
                List.of(thisMonth, lastMonth, withinThreeMonths, tooOld),
                WalletLogic.TransactionFilter.ALL,
                0,
                "",
                now));

        assertEquals(List.of(lastMonth), logic.filterTransactions(
                List.of(thisMonth, lastMonth, withinThreeMonths, tooOld),
                WalletLogic.TransactionFilter.ALL,
                1,
                "",
                now));

        assertEquals(List.of(thisMonth, lastMonth, withinThreeMonths), logic.filterTransactions(
                List.of(thisMonth, lastMonth, withinThreeMonths, tooOld),
                WalletLogic.TransactionFilter.ALL,
                2,
                "",
                now));
    }

    @Test
    void helpersFormatAmountsAndAccountNumbers() {
        Transaction deposit = transaction("T1", "DEPOSIT", 1200000, 1, "deposit");
        Transaction withdraw = transaction("T2", "WITHDRAW", 50000, 1, "withdraw");

        assertEquals("+ 1,200,000 đ", logic.formatSignedTransactionAmount(deposit));
        assertEquals("- 50,000 đ", logic.formatSignedTransactionAmount(withdraw));
        assertTrue(logic.isMoneyIn(deposit));
        assertFalse(logic.isMoneyIn(withdraw));
        assertEquals("**** **** **** 7890", logic.maskAccountNumber("1234567890"));
        assertEquals("123", logic.maskAccountNumber("123"));
        assertEquals("", logic.maskAccountNumber(null));
        assertEquals("--", logic.emptyToDash(null));
        assertEquals("--", logic.emptyToDash("  "));
        assertEquals("ok", logic.emptyToDash("ok"));
    }

    @Test
    void nullAndEmptyInputsAreHandled() {
        assertTrue(logic.filterTransactions(null, WalletLogic.TransactionFilter.ALL, 3, "", 1).isEmpty());
        assertFalse(logic.matchesTypeFilter(null, WalletLogic.TransactionFilter.ALL));
        assertTrue(logic.matchesTimeFilter(null, 0, 1));
        assertFalse(logic.matchesSearchKeyword(null, "x"));
        assertTrue(logic.matchesSearchKeyword(transaction("T1", "DEPOSIT", 1, 1, null), ""));
    }

    private Transaction transaction(String id, String type, double amount, long timestamp, String description) {
        Transaction transaction = new Transaction();
        transaction.id = id;
        transaction.userId = "U1";
        transaction.type = type;
        transaction.amount = amount;
        transaction.balanceAfter = amount;
        transaction.timestamp = timestamp;
        transaction.description = description;
        return transaction;
    }

    private long millis(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day, 12, 0);
        return calendar.getTimeInMillis();
    }
}
