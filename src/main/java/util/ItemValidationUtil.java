// src/main/java/util/ItemValidationUtil.java
package util;

import common.ItemCategory;

/**
 * ItemValidationUtil - Utility cho item validation
 *
 * Công thức chung:
 * 1. Starting bid phải >= minimum của category
 * 2. Bid mới phải >= (current bid + increment của category)
 * 3. Duration phải trong range [min, max] của category
 * 4. Item properties phải hợp lệ dựa trên category
 */
public class ItemValidationUtil {

    /**
     * Kiểm tra starting price có hợp lệ cho category không
     */
    public static boolean isValidStartingPrice(ItemCategory category, double price) {
        if (!ValidationUtil.isValidPrice(price)) {
            return false;
        }

        double minimumBid = category.getMinimumStartingBid();
        return price >= minimumBid;
    }

    /**
     * Lấy error message nếu starting price không hợp lệ
     */
    public static String getStartingPriceErrorMessage(ItemCategory category, double price) {
        if (price <= 0) {
            return "Giá khởi điểm phải lớn hơn 0";
        }

        double minimumBid = category.getMinimumStartingBid();
        if (price < minimumBid) {
            return String.format("Giá khởi điểm tối thiểu cho %s là: $%.2f",
                    category.name(), minimumBid);
        }

        return null; // Valid
    }

    /**
     * ← THÊM: Kiểm tra bid amount có hợp lệ không
     * Phải >= (current price + increment)
     */
    public static boolean isValidBidAmount(ItemCategory category, double currentPrice, double bidAmount) {
        if (!ValidationUtil.isValidBidAmount(bidAmount)) {
            return false;
        }

        double minimumRequired = currentPrice + category.getMinimumBidIncrement();
        return bidAmount >= minimumRequired;
    }

    /**
     * ← THÊM: Lấy error message nếu bid amount không hợp lệ
     */
    public static String getBidAmountErrorMessage(ItemCategory category, double currentPrice, double bidAmount) {
        double increment = category.getMinimumBidIncrement();
        double minimumRequired = currentPrice + increment;

        if (bidAmount <= currentPrice) {
            return String.format("Giá phải cao hơn giá hiện tại: $%.2f", currentPrice);
        }

        if (bidAmount < minimumRequired) {
            return String.format("Giá phải cao hơn ít nhất $%.2f (hiện tại: $%.2f + increment: $%.2f = $%.2f)",
                    increment, currentPrice, increment, minimumRequired);
        }

        return null; // Valid
    }

    /**
     * Kiểm tra duration có hợp lệ cho category không
     */
    public static boolean isValidDuration(ItemCategory category, int durationMinutes) {
        int minDuration = category.getMinDurationMinutes();
        int maxDuration = category.getMaxDurationMinutes();

        return durationMinutes >= minDuration && durationMinutes <= maxDuration;
    }

    /**
     * Lấy error message nếu duration không hợp lệ
     */
    public static String getDurationErrorMessage(ItemCategory category, int durationMinutes) {
        int minDuration = category.getMinDurationMinutes();
        int maxDuration = category.getMaxDurationMinutes();

        if (durationMinutes < minDuration) {
            int minDays = minDuration / 1440;
            return String.format("Thời gian đấu giá tối thiểu: %d ngày", minDays);
        }

        if (durationMinutes > maxDuration) {
            int maxDays = maxDuration / 1440;
            return String.format("Thời gian đấu giá tối đa: %d ngày", maxDays);
        }

        return null; // Valid
    }

    /**
     * Kiểm tra brand/chất liệu không được rỗng
     */
    public static boolean isValidBrand(String brand) {
        return brand != null && !brand.trim().isEmpty() && brand.length() <= 100;
    }

    /**
     * Kiểm tra chất liệu không được rỗng
     */
    public static boolean isValidMaterial(String material) {
        return material != null && !material.trim().isEmpty() && material.length() <= 100;
    }

    /**
     * Kiểm tra trọng lượng hợp lệ (cho jewelry)
     */
    public static boolean isValidWeight(double weight) {
        return weight > 0 && weight <= 100000; // Max 100kg
    }

    /**
     * Kiểm tra odometer hợp lệ (cho vehicle)
     */
    public static boolean isValidOdometer(int odometer) {
        return odometer >= 0 && odometer <= 10000000; // Max 10 million km
    }

    /**
     * Kiểm tra warranty period hợp lệ (cho electronics)
     */
    public static boolean isValidWarrantyPeriod(String warranty) {
        return warranty != null && !warranty.trim().isEmpty() && warranty.length() <= 100;
    }

    /**
     * Lấy thông tin category
     */
    public static String getCategoryInfo(ItemCategory category) {
        StringBuilder info = new StringBuilder();
        info.append("📦 Loại: ").append(category.getDescription()).append("\n");
        info.append("💰 Giá tối thiểu: $").append(String.format("%.2f", category.getMinimumStartingBid())).append("\n");
        info.append("📈 Mỗi lần bid tối thiểu: $").append(String.format("%.2f", category.getMinimumBidIncrement())).append("\n");

        int minDays = category.getMinDurationMinutes() / 1440;
        int maxDays = category.getMaxDurationMinutes() / 1440;
        info.append("⏱ Thời gian: ").append(minDays).append(" - ").append(maxDays).append(" ngày");

        return info.toString();
    }
}