package Common;

import java.io.Serializable;
import java.time.LocalDateTime;

public abstract class Item implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private String description;
    private double startingPrice;
    private double currentPrice;
    private String currentWinnerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String sellerId;
    private String status; // UPCOMING, ONGOING, FINISHED

    public Item(String id, String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime, String sellerId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sellerId = sellerId;
        this.status = "UPCOMING";
    }

    // Getters & Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public String getCurrentWinnerId() { return currentWinnerId; }
    public void setCurrentWinnerId(String currentWinnerId) { this.currentWinnerId = currentWinnerId; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Phương thức đa hình để lấy loại sản phẩm
    public abstract String getCategory();
}
