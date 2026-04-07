// src/main/java/server/model/RegularUser.java
package server.model;

import common.UserRole;
import java.util.*;

/**
 * RegularUser - Người dùng thường (Seller + Bidder)
 * OOP: Inheritance, Encapsulation
 *
 * Flow:
 * 1. Đầu tiên là BIDDER (mặc định)
 * 2. Có thể nâng cấp thành SELLER
 * 3. Sau khi nâng cấp, vừa có thể BID vừa có thể SELL
 */
public class RegularUser extends User {
    private static final long serialVersionUID = 1L;

    private List<String> ownedAuctionIds;
    private List<String> bidIds;
    private boolean isSeller;  // ← THÊM: Track xem có phải seller không

    public RegularUser(String userId, String username, String password, String email) {
        super(userId, username, password, email, UserRole.BIDDER);
        this.ownedAuctionIds = new ArrayList<>();
        this.bidIds = new ArrayList<>();
        this.isSeller = false;  // ← THÊM: Mặc định là false (chỉ là bidder)
    }

    public RegularUser() {
        super();
        this.ownedAuctionIds = new ArrayList<>();
        this.bidIds = new ArrayList<>();
        this.isSeller = false;
    }

    @Override
    public String getDashboardTitle() {
        if (isSeller) {
            return "🏪 CHỢ ĐẤU GIÁ - " + username + " (Người mua & Người bán)";
        } else {
            return "🏪 CHỢ ĐẤU GIÁ - " + username + " (Người mua)";
        }
    }

    @Override
    public List<String> getAvailableActions() {
        List<String> actions = new ArrayList<>();
        actions.add("👁 Xem đấu giá");
        actions.add("💰 Tham gia trả giá");

        // ← THÊM: Actions chỉ khi là seller
        if (isSeller) {
            actions.add("📝 Tạo phiên đấu giá mới");
            actions.add("📦 Quản lý sản phẩm");
        }

        actions.add("📊 Xem lịch sử bid");
        actions.add("💳 Nạp tiền");

        return actions;
    }

    /**
     * ← THÊM: Nâng cấp lên seller
     */
    public void upgradeSeller() {
        this.isSeller = true;
        LoggerUtil.info("✓ User " + username + " upgraded to SELLER");
    }

    /**
     * ← THÊM: Kiểm tra có phải seller không
     */
    public boolean isSeller() {
        return isSeller;
    }

    public void setSeller(boolean seller) {
        this.isSeller = seller;
    }

    public void addOwnedAuction(String auctionId) {
        if (!ownedAuctionIds.contains(auctionId)) {
            ownedAuctionIds.add(auctionId);
        }
    }

    public void addBidId(String bidId) {
        if (!bidIds.contains(bidId)) {
            bidIds.add(bidId);
        }
    }

    public List<String> getOwnedAuctionIds() {
        return new ArrayList<>(ownedAuctionIds);
    }

    public void setOwnedAuctionIds(List<String> ownedAuctionIds) {
        this.ownedAuctionIds = ownedAuctionIds;
    }

    public List<String> getBidIds() {
        return new ArrayList<>(bidIds);
    }

    public void setBidIds(List<String> bidIds) {
        this.bidIds = bidIds;
    }

    @Override
    public String toString() {
        return "RegularUser{" + "userId='" + userId + '\'' + ", username='" + username +
                '\'' + ", isSeller=" + isSeller + '}';
    }
}
