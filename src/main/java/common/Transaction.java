package common;

import java.io.Serializable;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    public String id;
    public String userId;
    public String type;
    public double amount;
    public double balanceAfter;
    public String description;
    public long timestamp;

    public Transaction() {}

    public Transaction(String userId, String type, double amount, double balanceAfter, String description) {
        this.id = "TXN_" + System.currentTimeMillis() + "_" + System.nanoTime();
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.timestamp = System.currentTimeMillis();
    }

    // ===== IMPORTANT GETTERS =====
    public String getFormattedDate() {
        return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm")
                .format(new java.util.Date(timestamp));
    }

    public String getTypeLabel() {
        if ("DEPOSIT".equals(type)) {
            return "+ N\u1ea1p ti\u1ec1n";
        }
        if ("PAYMENT".equals(type)) {
            return "- Thanh to\u00e1n";
        }
        return "- R\u00fat ti\u1ec1n";
    }

    public double getAmount() {
        return amount;
    }

    public String getMoneyIn() {
        return "DEPOSIT".equals(type) ? formatVnd(amount) : "";
    }

    public String getMoneyOut() {
        return "WITHDRAW".equals(type) || "PAYMENT".equals(type) ? formatVnd(amount) : "";
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public String getFormattedBalanceAfter() {
        return formatVnd(balanceAfter);
    }

    public String getDescription() {
        return description;
    }

    public String getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    private String formatVnd(double value) {
        return String.format("%,.0f đ", value);
    }
}
