package server.model;

import common.AuctionStatus;
import java.io.Serializable;
import java.util.*;

public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;

    private String auctionId;
    private String itemId;
    private String sellerId;
    private String sellerName;
    private Item item;

    private double currentPrice;
    private String highestBidderId;
    private String highestBidderName;

    private AuctionStatus status;
    private long startTime;
    private long endTime;
    private long createdAt;

    private List<String> bidIds;
    private int viewCount;

    // Constructor 1: Với startTimeMillis/endTimeMillis (dùng cho AddAuctionProductController)
    public Auction(String auctionId, String itemId, String sellerId, String sellerName,
                   Item item, double startingPrice, long startTimeMillis, long endTimeMillis) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.item = item;

        this.currentPrice = startingPrice;
        this.highestBidderId = null;
        this.highestBidderName = null;

        this.status = AuctionStatus.OPEN;
        this.startTime = startTimeMillis;
        this.endTime = endTimeMillis;
        this.createdAt = System.currentTimeMillis();

        this.bidIds = new ArrayList<>();
        this.viewCount = 0;
    }

    // Constructor 2: Với durationMinutes (dùng cho AuctionService)
    public Auction(String auctionId, String itemId, String sellerId, String sellerName,
                   Item item, double startingPrice, int durationMinutes) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.item = item;

        this.currentPrice = startingPrice;
        this.highestBidderId = null;
        this.highestBidderName = null;

        this.status = AuctionStatus.OPEN;
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + (durationMinutes * 60 * 1000L);
        this.createdAt = System.currentTimeMillis();

        this.bidIds = new ArrayList<>();
        this.viewCount = 0;
    }

    // Constructor 3: Empty
    public Auction() {
        this.bidIds = new ArrayList<>();
    }

    public synchronized boolean placeBid(String bidderId, String bidderName, double amount) throws Exception {
        if (status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING) {
            throw new Exception("Phiên đấu giá đã kết thúc!");
        }

        if (amount <= currentPrice) {
            throw new Exception("Giá đấu phải cao hơn: $" + currentPrice);
        }

        this.currentPrice = amount;
        this.highestBidderId = bidderId;
        this.highestBidderName = bidderName;
        this.status = AuctionStatus.RUNNING;

        return true;
    }

    public synchronized void extendAuctionIfNeeded() {
        long now = System.currentTimeMillis();
        long fiveMinutesBefore = endTime - (5 * 60 * 1000L);

        if (now > fiveMinutesBefore && bidIds.size() > 0) {
            endTime += (2 * 60 * 1000L);
            System.out.println("Phiên đấu được gia hạn thêm 2 phút!");
        }
    }

    public long getTimeRemainingSeconds() {
        long now = System.currentTimeMillis();
        if (now > endTime) return 0;
        return (endTime - now) / 1000;
    }

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public String getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(String highestBidderId) { this.highestBidderId = highestBidderId; }

    public String getHighestBidderName() { return highestBidderName; }
    public void setHighestBidderName(String highestBidderName) { this.highestBidderName = highestBidderName; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public long getCreatedAt() { return createdAt; }

    public List<String> getBidIds() { return new ArrayList<>(bidIds); }
    public void setBidIds(List<String> bidIds) { this.bidIds = bidIds; }

    public void addBidId(String bidId) {
        if (!bidIds.contains(bidId)) {
            bidIds.add(bidId);
        }
    }

    public int getViewCount() {
        return viewCount; }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount; }

    @Override
    public String toString() {
        return "Auction{" + "auctionId='" + auctionId + '\'' + ", item=" +
                (item != null ? item.getName() : "null") + '}';
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}