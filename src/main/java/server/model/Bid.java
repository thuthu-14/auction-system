package server.model;

import java.io.Serializable;

public class Bid implements Serializable {
    private static final long serialVersionUID = 1L;

    private String bidId;
    private String auctionId;
    private String bidderId;
    private String bidderName;
    private double amount;
    private long bidTime;
    private String status;

    public Bid(String bidId, String auctionId, String bidderId,
               String bidderName, double amount) {
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = amount;
        this.bidTime = System.currentTimeMillis();
        this.status = "ACTIVE";
    }

    public Bid() {
        this.bidTime = System.currentTimeMillis();
        this.status = "ACTIVE";
    }

    public String getBidId() { return bidId; }
    public void setBidId(String bidId) { this.bidId = bidId; }

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public String getBidderId() { return bidderId; }
    public void setBidderId(String bidderId) { this.bidderId = bidderId; }

    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public long getBidTime() { return bidTime; }
    public void setBidTime(long bidTime) { this.bidTime = bidTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Bid{" + "bidId='" + bidId + '\'' + ", auctionId='" + auctionId + '\'' + '}';
    }
}
