package server.model;

import common.UserRole;
import util.LoggerUtil;

import java.util.*;

public class RegularUser extends User {
    private static final long serialVersionUID = 1L;

    private List<String> ownedAuctionIds;
    private List<String> bidIds;
    private boolean isSeller;

    // ✅ THÊM: Thông tin cửa hàng
    private String shopName;
    private String shopPhone;
    private String shopAddress;
    private String shopEmail;

    public RegularUser(String userId, String username, String password, String email) {
        super(userId, username, password, email, UserRole.BIDDER);
        this.ownedAuctionIds = new ArrayList<>();
        this.bidIds = new ArrayList<>();
        this.isSeller = false;
        this.shopName = null;
        this.shopPhone = null;
        this.shopAddress = null;
        this.shopEmail = null;
    }

    public RegularUser() {
        super();
        this.ownedAuctionIds = new ArrayList<>();
        this.bidIds = new ArrayList<>();
        this.isSeller = false;
        this.shopName = null;
        this.shopPhone = null;
        this.shopAddress = null;
        this.shopEmail = null;
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

        if (isSeller) {
            actions.add("📝 Tạo phiên đấu giá mới");
            actions.add("📦 Quản lý sản phẩm");
        }

        actions.add("📊 Xem lịch sử bid");
        actions.add("💳 Nạp tiền");

        return actions;
    }

    /**
     * ← THÊM: Nâng cấp lên seller với shop info
     */
    /**
     * ← THÊM: Nâng cấp lên seller với shop info
     */
    public void upgradeSeller(String shopName, String shopPhone, String shopAddress, String shopEmail) {
        this.isSeller = true;
        this.shopName = shopName;
        this.shopPhone = shopPhone;
        this.shopAddress = shopAddress;
        this.shopEmail = shopEmail != null ? shopEmail : email;
        this.role = common.UserRole.SELLER;  // ← THÊM DÒNG NÀY: Thay đổi role từ BIDDER → SELLER
        LoggerUtil.info("✓ User " + username + " upgraded to SELLER - Shop: " + shopName);
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

    // ✅ THÊM: Getters/Setters cho shop info
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getShopPhone() { return shopPhone; }
    public void setShopPhone(String shopPhone) { this.shopPhone = shopPhone; }

    public String getShopAddress() { return shopAddress; }
    public void setShopAddress(String shopAddress) { this.shopAddress = shopAddress; }

    public String getShopEmail() { return shopEmail; }
    public void setShopEmail(String shopEmail) { this.shopEmail = shopEmail; }

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

    public void setRole(common.UserRole role) {
        this.role = role;
    }
}