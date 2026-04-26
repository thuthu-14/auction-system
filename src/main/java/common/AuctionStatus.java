package common;

import java.io.Serializable;

public enum AuctionStatus implements Serializable {
    DRAFT("Bản nháp - Chưa công bố"),
    OPEN("Mở - Đang chờ người trả giá"),
    RUNNING("Đang diễn ra - Có người đã trả giá"),
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
