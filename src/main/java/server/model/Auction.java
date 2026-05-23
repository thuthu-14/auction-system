package server.model;

import common.AuctionStatus;
import common.Constants;
import util.LoggerUtil;
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
    private double reservePrice;
    private double minimumBidIncrement;

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

        long now = System.currentTimeMillis();
        // ← SET TRẠNG THÁI ĐÚNG DỰA TRÊN THỜI GIAN
        if (startTimeMillis > now) {
            this.status = AuctionStatus.WAITING;  // Chưa đến giờ
        } else {
            this.status = AuctionStatus.OPEN;     // Đã đến giờ, mở đấu
        }

        this.startTime = startTimeMillis;
        this.endTime = endTimeMillis;
        this.createdAt = System.currentTimeMillis();
        this.reservePrice = 0.0;
        this.minimumBidIncrement = 0.0;

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

        // Với constructor này, startTime lúc nào cũng = now, nên trạng thái là OPEN
        this.status = AuctionStatus.OPEN;
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + (durationMinutes * 60 * 1000L);
        this.createdAt = System.currentTimeMillis();
        this.reservePrice = 0.0;
        this.minimumBidIncrement = 0.0;

        this.bidIds = new ArrayList<>();
        this.viewCount = 0;
    }
    // Constructor 3: Empty (dùng cho mapping database)
    public Auction() {
        this.bidIds = new ArrayList<>();
    }

    // Constructor 3: Empty
    public synchronized boolean placeBid(String bidderId, String bidderName, double amount) throws Exception {
        long now = System.currentTimeMillis();

        // Nếu chưa đến thời gian bắt đầu
        if (now < startTime) {
            throw new Exception("Phiên đấu giá chưa bắt đầu! Vui lòng đợi đến " +
                    new java.util.Date(startTime));
        }

        // Nếu quá thời gian kết thúc
        if (now > endTime) {
            throw new Exception("Phiên đấu giá đã kết thúc!");
        }

        // Kiểm tra giá
        if (amount <= currentPrice) {
            throw new Exception("Giá đấu phải cao hơn: $" + currentPrice);
        }

        this.currentPrice = amount;
        this.highestBidderId = bidderId;
        this.highestBidderName = bidderName;

        // ← THAY ĐỔI: Chuyển từ WAITING → OPEN → RUNNING
        if (status == AuctionStatus.WAITING) {
            this.status = AuctionStatus.OPEN;  // Chuyển từ "chờ bắt đầu" sang "đang mở"
        }
        this.status = AuctionStatus.RUNNING;   // Rồi chuyển sang "đang diễn ra"

        return true;
    }

    public synchronized void extendAuctionIfNeeded() {
        long now = System.currentTimeMillis();
        long remainingMillis = endTime - now;
        long antiSnipeWindowMillis = Constants.ANTI_SNIPE_MINUTES * 60 * 1000L;

        if (isOpenForBidding() && remainingMillis > 0 && remainingMillis <= antiSnipeWindowMillis) {
            long extensionMillis = Constants.AUTO_EXTEND_MINUTES * 60 * 1000L;
            endTime += extensionMillis;
            LoggerUtil.info("Anti-sniping extension: auction " + auctionId
                    + " extended by " + Constants.AUTO_EXTEND_MINUTES
                    + " minutes. Remaining before extension: " + (remainingMillis / 1000) + "s");
        }
    }

    private boolean isOpenForBidding() {
        return status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING;
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

    public double getReservePrice() { return reservePrice; }
    public void setReservePrice(double reservePrice) { this.reservePrice = reservePrice; }

    public double getMinimumBidIncrement() { return minimumBidIncrement; }
    public void setMinimumBidIncrement(double minimumBidIncrement) { this.minimumBidIncrement = minimumBidIncrement; }

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

    /**
     * Kiểm tra và cập nhật trạng thái dựa trên thời gian hiện tại
     * Gọi method này mỗi khi load auction hoặc trước khi hiển thị
     */
    public synchronized void syncStatusWithTime() {
        long now = System.currentTimeMillis();

        if (status == AuctionStatus.CLOSED || status == AuctionStatus.FINISHED ||
                status == AuctionStatus.CANCELLED) {
            return;  // Không cập nhật trạng thái kết thúc
        }

        // Nếu chưa đến giờ bắt đầu → WAITING
        if (now < startTime && status != AuctionStatus.WAITING) {
            this.status = AuctionStatus.WAITING;
        }

        // Nếu quá giờ kết thúc → cần CLOSED
        if (now > endTime && (status == AuctionStatus.WAITING ||
                status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING)) {
            // Lưu ý: không tự động set FINISHED ở đây, cần server signal
            // Chỉ set CLOSED tạm thời để khóa đấu giá
            this.status = AuctionStatus.CLOSED;
        }
    }
}
