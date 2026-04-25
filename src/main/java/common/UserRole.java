package common;

import server.exception.PermissionDeniedException;

import java.io.Serializable;

public enum UserRole implements Serializable {
    BIDDER("Người mua - Tham gia đấu giá"),
    SELLER("Người bán - Tạo phiên đấu giá"),
    ADMIN("Quản trị viên - Toàn quyền quản lý");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}





