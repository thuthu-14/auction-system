package client.logic;

import common.AuctionStatus;
import server.model.Auction;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class SellerStatisticsLogic {
    private static final NumberFormat VND_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));

    public SellerStatisticsSummary summarize(List<Auction> auctions, long nowMillis) {
        List<Auction> safeAuctions = auctions == null ? List.of() : auctions;
        double totalRevenue = 0;
        int totalBids = 0;
        int endedAuctions = 0;
        int successfulAuctions = 0;
        int countedAuctions = 0;
        Map<Long, Double> revenueByEndTime = new TreeMap<>();

        for (Auction auction : safeAuctions) {
            if (auction == null || isCancelled(auction)) {
                continue;
            }

            countedAuctions++;
            int bidCount = bidCount(auction);
            totalBids += bidCount;

            if (isEnded(auction, nowMillis)) {
                endedAuctions++;
                if (bidCount > 0) {
                    successfulAuctions++;
                    totalRevenue += auction.getCurrentPrice();

                    long endTime = auction.getEndTime() > 0 ? auction.getEndTime() : auction.getCreatedAt();
                    revenueByEndTime.merge(endTime, auction.getCurrentPrice(), Double::sum);
                }
            }
        }

        double averageBid = countedAuctions == 0 ? 0 : (double) totalBids / countedAuctions;
        double successRate = endedAuctions == 0 ? 0 : successfulAuctions * 100.0 / endedAuctions;
        return new SellerStatisticsSummary(totalRevenue, averageBid, successRate, revenueByEndTime);
    }

    public boolean isEnded(Auction auction, long nowMillis) {
        if (isCancelled(auction)) {
            return false;
        }
        if (auction.getStatus() == AuctionStatus.FINISHED
                || auction.getStatus() == AuctionStatus.CLOSED) {
            return true;
        }
        return auction.getEndTime() > 0 && auction.getEndTime() <= nowMillis;
    }

    public boolean isCancelled(Auction auction) {
        return auction != null && auction.getStatus() == AuctionStatus.CANCELLED;
    }

    public int bidCount(Auction auction) {
        return auction != null && auction.getBidIds() != null ? auction.getBidIds().size() : 0;
    }

    public double calculateTickUnit(double max) {
        if (max <= 0) {
            return 10;
        }
        double roughTick = max / 6.0;
        double magnitude = Math.pow(10, Math.floor(Math.log10(roughTick)));
        double normalized = roughTick / magnitude;

        if (normalized <= 1) {
            return magnitude;
        }
        if (normalized <= 2) {
            return 2 * magnitude;
        }
        if (normalized <= 5) {
            return 5 * magnitude;
        }
        return 10 * magnitude;
    }

    public String formatVnd(double amount) {
        if (Math.abs(amount) >= 1_000_000) {
            return String.format(Locale.US, "%.1fM đ", amount / 1_000_000.0);
        }
        return VND_FORMATTER.format(Math.round(amount)) + " đ";
    }

    public String formatAverageBid(double averageBid) {
        return String.format(Locale.US, "%.1f", averageBid);
    }

    public String formatSuccessRate(double successRate) {
        return String.format(Locale.US, "%.1f%%", successRate);
    }

    public record SellerStatisticsSummary(double totalRevenue,
                                          double averageBid,
                                          double successRate,
                                          Map<Long, Double> revenueByEndTime) {
    }
}
