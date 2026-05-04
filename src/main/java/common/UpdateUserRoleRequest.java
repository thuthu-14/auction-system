package common;

import java.io.Serializable;

/**
 * Request để upgrade user từ BIDDER thành SELLER
 */
public class UpdateUserRoleRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private UserRole newRole;
    private String shopName;
    private String shopEmail;
    private String shopPhone;
    private String shopAddress;

    public UpdateUserRoleRequest(String userId, UserRole newRole,
                                 String shopName, String shopEmail,
                                 String shopPhone, String shopAddress) {
        this.userId = userId;
        this.newRole = newRole;
        this.shopName = shopName;
        this.shopEmail = shopEmail;
        this.shopPhone = shopPhone;
        this.shopAddress = shopAddress;
    }

    public UpdateUserRoleRequest() {}

    // Getters
    public String getUserId() { return userId; }
    public UserRole getNewRole() { return newRole; }
    public String getShopName() { return shopName; }
    public String getShopEmail() { return shopEmail; }
    public String getShopPhone() { return shopPhone; }
    public String getShopAddress() { return shopAddress; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setNewRole(UserRole newRole) { this.newRole = newRole; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public void setShopEmail(String shopEmail) { this.shopEmail = shopEmail; }
    public void setShopPhone(String shopPhone) { this.shopPhone = shopPhone; }
    public void setShopAddress(String shopAddress) { this.shopAddress = shopAddress; }

    @Override
    public String toString() {
        return "UpdateUserRoleRequest{" +
                "userId='" + userId + '\'' +
                ", newRole=" + newRole +
                ", shopName='" + shopName + '\'' +
                ", shopEmail='" + shopEmail + '\'' +
                ", shopPhone='" + shopPhone + '\'' +
                ", shopAddress='" + shopAddress + '\'' +
                '}';
    }
}