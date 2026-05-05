package common;

public class Transaction {
    public String id;
    public String userId;
    public String type;
    public double amount;
    public double balanceAfter;
    public String description;
    public long timestamp;

    public Transaction() {}

    public Transaction(String userId, String type, double amount, double balanceAfter, String description) {
        this.id = "TXN_" + System.currentTimeMillis();
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
        return "DEPOSIT".equals(type) ? "+ Nạp tiền" : "- Rút tiền";
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
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
}