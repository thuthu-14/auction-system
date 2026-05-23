package client.logic;

import common.Transaction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WalletLogic {

    public enum TransactionFilter {
        ALL, IN, OUT
    }

    public List<Transaction> filterTransactions(List<Transaction> transactions,
                                                 TransactionFilter typeFilter,
                                                 int timeFilterIndex,
                                                 String keyword,
                                                 long nowMillis) {
        if (transactions == null || transactions.isEmpty()) {
            return new ArrayList<>();
        }

        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        TransactionFilter resolvedFilter = typeFilter == null ? TransactionFilter.ALL : typeFilter;

        return transactions.stream()
                .filter(t -> matchesTypeFilter(t, resolvedFilter))
                .filter(t -> matchesTimeFilter(t, timeFilterIndex, nowMillis))
                .filter(t -> normalizedKeyword.isEmpty() || matchesSearchKeyword(t, normalizedKeyword))
                .toList();
    }

    public boolean matchesTypeFilter(Transaction transaction, TransactionFilter typeFilter) {
        if (transaction == null) {
            return false;
        }
        if (typeFilter == TransactionFilter.IN) {
            return "DEPOSIT".equals(transaction.type);
        }
        if (typeFilter == TransactionFilter.OUT) {
            return "WITHDRAW".equals(transaction.type) || "PAYMENT".equals(transaction.type);
        }
        return true;
    }

    public boolean matchesTimeFilter(Transaction transaction, int selectedIndex, long nowMillis) {
        if (transaction == null || selectedIndex < 0 || selectedIndex == 3) {
            return true;
        }

        Calendar transactionDate = Calendar.getInstance();
        transactionDate.setTimeInMillis(transaction.timestamp);

        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowMillis);
        if (selectedIndex == 0) {
            return sameYearAndMonth(transactionDate, now);
        }

        if (selectedIndex == 1) {
            now.add(Calendar.MONTH, -1);
            return sameYearAndMonth(transactionDate, now);
        }

        Calendar threeMonthsAgo = Calendar.getInstance();
        threeMonthsAgo.setTimeInMillis(nowMillis);
        threeMonthsAgo.add(Calendar.MONTH, -3);
        return transaction.timestamp >= threeMonthsAgo.getTimeInMillis();
    }

    public boolean matchesSearchKeyword(Transaction transaction, String keyword) {
        if (transaction == null) {
            return false;
        }
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        if (normalizedKeyword.isEmpty()) {
            return true;
        }
        String description = transaction.description == null ? "" : transaction.description.toLowerCase();
        String typeLabel = transaction.getTypeLabel() == null ? "" : transaction.getTypeLabel().toLowerCase();
        return description.contains(normalizedKeyword)
                || typeLabel.contains(normalizedKeyword)
                || transaction.getFormattedDate().contains(normalizedKeyword);
    }

    public String formatVnd(double value) {
        return String.format("%,.0f đ", value);
    }

    public String formatSignedTransactionAmount(Transaction transaction) {
        String prefix = isMoneyIn(transaction) ? "+ " : "- ";
        return prefix + formatVnd(transaction == null ? 0 : transaction.getAmount());
    }

    public boolean isMoneyIn(Transaction transaction) {
        return transaction != null && "DEPOSIT".equals(transaction.getType());
    }

    public String maskAccountNumber(String accountNumber) {
        String acc = accountNumber == null ? "" : accountNumber;
        return acc.length() >= 4 ? "**** **** **** " + acc.substring(acc.length() - 4) : acc;
    }

    public String emptyToDash(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }

    private boolean sameYearAndMonth(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.MONTH) == second.get(Calendar.MONTH);
    }
}
