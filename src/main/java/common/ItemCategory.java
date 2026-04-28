// src/main/java/common/ItemCategory.java
package common;

import java.io.Serializable;

/**
 * ItemCategory - Phân loại sản phẩm
 *
 * MỖI CATEGORY CÓ:
 * 1. minimumStartingBid - Giá khởi điểm tối thiểu
 * 2. minimumBidIncrement - Khoảng tiền tối thiểu mỗi lần bid
 * 3. minDurationMinutes - Thời gian tối thiểu
 * 4. maxDurationMinutes - Thời gian tối đa
 *
 * VD: ELECTRONICS
 * - Starting price tối thiểu: $50
 * - Mỗi lần bid phải cao hơn ít nhất: $50
 * - Thời gian: 1-7 ngày
 */
public enum ItemCategory implements Serializable {
    ELECTRONICS("Điện tử - Điện thoại, Laptop, Camera...", 50.0, 50.0, 1440, 10080),
    ART("Nghệ thuật - Tranh, Tượng, Điêu khắc...", 100.0, 100.0, 1440, 20160),
    VEHICLE("Xe cộ - Xe máy, Ô tô, Xe đạp...", 500.0, 500.0, 1440, 10080),
    FASHION("Thời trang - Quần áo, Giày, Túi xách...", 10.0, 10.0, 1440, 4320),
    JEWELRY("Trang sức - Nhẫn, Vòng, Dây chuyền...", 25.0, 25.0, 1440, 7200);

    private final String description;
    private final double minimumStartingBid;     // ← Giá khởi điểm tối thiểu
    private final double minimumBidIncrement;    // ← Khoảng bid tối thiểu
    private final int minDurationMinutes;        // ← Thời gian tối thiểu
    private final int maxDurationMinutes;        // ← Thời gian tối đa

    ItemCategory(String description, double minimumStartingBid, double minimumBidIncrement,
                 int minDurationMinutes, int maxDurationMinutes) {
        this.description = description;
        this.minimumStartingBid = minimumStartingBid;
        this.minimumBidIncrement = minimumBidIncrement;
        this.minDurationMinutes = minDurationMinutes;
        this.maxDurationMinutes = maxDurationMinutes;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Lấy giá khởi điểm tối thiểu
     */
    public double getMinimumStartingBid() {
        return minimumStartingBid;
    }

    /**
     * ← THÊM: Lấy khoảng bid tối thiểu
     * Ví dụ: ELECTRONICS → $50 (phải bid cao hơn ít nhất $50)
     */
    public double getMinimumBidIncrement() {
        return minimumBidIncrement;
    }

    /**
     * Lấy thời gian tối thiểu (phút)
     */
    public int getMinDurationMinutes() {
        return minDurationMinutes;
    }

    /**
     * Lấy thời gian tối đa (phút)
     */
    public int getMaxDurationMinutes() {
        return maxDurationMinutes;
    }

    /**
     * Lấy mô tả chi tiết về duration
     */
    public String getDurationInfo() {
        int minDays = getMinDurationMinutes() / 1440;
        int maxDays = getMaxDurationMinutes() / 1440;
        return String.format("Thời gian đấu giá: %d - %d ngày", minDays, maxDays);
    }

    /**
     * ← THÊM: Lấy mô tả chi tiết về bid increment
     */
    public String getBidIncrementInfo() {
        return String.format("Mỗi lần bid phải cao hơn ít nhất: $%.2f", minimumBidIncrement);
    }
}