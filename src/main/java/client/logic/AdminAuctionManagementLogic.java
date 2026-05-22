package client.logic;

import common.AuctionStatus;
import server.model.Auction;
import server.model.Item;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;

public final class AdminAuctionManagementLogic {
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat VND_FORMATTER =
            NumberFormat.getInstance(new Locale("vi", "VN"));

    private AdminAuctionManagementLogic() {}

    public static AdminAuctionData map(Auction auction) {
        Item item = auction.getItem();
        AuctionStatus auctionStatus = auction.getStatus();
        return new AdminAuctionData(
                valueOrDash(auction.getAuctionId()),
                valueOrDash(auction.getItemId()),
                valueOrDash(auction.getSellerId()),
                valueOrDash(auction.getSellerName()),
                item != null ? valueOrDash(item.getName()) : "-",
                auction.getCurrentPrice(),
                VND_FORMATTER.format(Math.round(auction.getCurrentPrice())) + " VNĐ",
                formatDateTime(auction.getEndTime()),
                auctionStatus != null ? auctionStatus.name() : "-",
                auctionStatus != null ? auctionStatus.name() : "-",
                auction.getCreatedAt(),
                auction.getEndTime()
        );
    }

    public static boolean matches(AdminAuctionData row, String keyword) {
        String normalizedKeyword = value(keyword).toLowerCase();
        return row.auctionId().toLowerCase().contains(normalizedKeyword)
                || row.itemId().toLowerCase().contains(normalizedKeyword)
                || row.sellerId().toLowerCase().contains(normalizedKeyword)
                || row.sellerName().toLowerCase().contains(normalizedKeyword)
                || row.itemName().toLowerCase().contains(normalizedKeyword)
                || row.rawStatus().toLowerCase().contains(normalizedKeyword);
    }

    public static Comparator<AdminAuctionData> comparator(String sort) {
        if ("Cũ nhất trước".equals(sort)) {
            return Comparator.comparingLong(AdminAuctionData::createdAt);
        }
        if ("Giá cao nhất".equals(sort)) {
            return Comparator.comparingDouble(AdminAuctionData::rawCurrentPrice).reversed();
        }
        if ("Giá thấp nhất".equals(sort)) {
            return Comparator.comparingDouble(AdminAuctionData::rawCurrentPrice);
        }
        if ("Sắp kết thúc".equals(sort)) {
            return Comparator.comparingLong(AdminAuctionData::endTimeMillis);
        }
        return Comparator.comparingLong(AdminAuctionData::createdAt).reversed();
    }

    public static String formatDateTime(long millis) {
        if (millis <= 0) {
            return "-";
        }
        return Instant.ofEpochMilli(millis).atZone(ZONE_ID).format(DATE_TIME_FORMATTER);
    }

    public static LocalDate createdDate(long createdAt) {
        return Instant.ofEpochMilli(createdAt).atZone(ZONE_ID).toLocalDate();
    }

    public static String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }

    public record AdminAuctionData(
            String auctionId,
            String itemId,
            String sellerId,
            String sellerName,
            String itemName,
            double rawCurrentPrice,
            String currentPrice,
            String endTime,
            String rawStatus,
            String status,
            long createdAt,
            long endTimeMillis
    ) {
    }
}
