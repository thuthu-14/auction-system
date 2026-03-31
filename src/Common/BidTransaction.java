package Common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidTransaction implements Serializable {
    private String itemId;
    private String bidderName;
    private double amount;
    private LocalDateTime time;

    public BidTransaction(String itemId, String bidderName, double amount) {
        this.itemId = itemId;
        this.bidderName = bidderName;
        this.amount = amount;
        this.time = LocalDateTime.now();
    }

    public double getAmount() { return amount; }
    public String getBidderName() { return bidderName; }
    public LocalDateTime getTime() { return time; }
}
