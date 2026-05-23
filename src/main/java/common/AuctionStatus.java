package common;

import java.io.Serializable;

public enum AuctionStatus implements Serializable {
    WAITING("Chờ bắt đầu - Chưa đến giờ"),  // ← THÊM TRẠNG THÁI NÀY
    OPEN("Mở - Đang chờ người trả giá"),
    RUNNING("Đang diễn ra - Có người đã trả giá"),
    CLOSED(""),
    FINISHED("Kết thúc - Xác định người thắng"),
    CANCELLED("Hủy - Không tiếp tục");

    private final String description;

    AuctionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}