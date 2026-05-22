package client.logic;

import server.model.Auction;
import server.model.Bid;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AuctionDetailLogic {
    private static final NumberFormat VND_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));

    public double resolveCurrentPrice(boolean sameAuction,
                                      double existingCurrentPrice,
                                      double incomingCurrentPrice,
                                      double cachedHighestPrice) {
        return sameAuction
                ? Math.max(Math.max(existingCurrentPrice, incomingCurrentPrice), cachedHighestPrice)
                : Math.max(incomingCurrentPrice, cachedHighestPrice);
    }

    public double resolveMinimumBidIncrement(double incomingIncrement, double fallbackIncrement) {
        return incomingIncrement > 0 ? incomingIncrement : fallbackIncrement;
    }

    public double minimumAllowedBid(double currentPrice, double minimumBidIncrement) {
        return currentPrice + minimumBidIncrement;
    }

    public double nextIncreaseBid(String currentBidText, double currentPrice, double minimumBidIncrement) {
        try {
            return parseBidAmountStrict(currentBidText) + minimumBidIncrement;
        } catch (Exception e) {
            return minimumAllowedBid(currentPrice, minimumBidIncrement);
        }
    }

    public double nextDecreaseBid(String currentBidText, double currentPrice, double minimumBidIncrement) {
        double minAllowedBid = minimumAllowedBid(currentPrice, minimumBidIncrement);
        try {
            double currentBid = parseBidAmountStrict(currentBidText);
            double decreased = currentBid - minimumBidIncrement;
            return decreased >= minAllowedBid ? decreased : currentBid;
        } catch (Exception e) {
            return minAllowedBid;
        }
    }

    public double parseBidAmount(String text, double currentPrice, double minimumBidIncrement) {
        try {
            return parseBidAmountStrict(text);
        } catch (Exception e) {
            return minimumAllowedBid(currentPrice, minimumBidIncrement);
        }
    }

    private double parseBidAmountStrict(String text) {
        return Double.parseDouble((text == null ? "" : text).replaceAll("[^0-9]", ""));
    }

    public double latestPriceFromHistory(double currentPrice, List<Bid> sortedBids) {
        if (sortedBids == null || sortedBids.isEmpty()) {
            return currentPrice;
        }
        return Math.max(currentPrice, sortedBids.get(sortedBids.size() - 1).getAmount());
    }

    public boolean shouldApplyLatestBidPrice(double amount, double currentPrice) {
        return amount > currentPrice;
    }

    public boolean isCurrentAuction(Auction auction, String currentAuctionId) {
        return auction != null && currentAuctionId != null && currentAuctionId.equals(auction.getAuctionId());
    }

    public String safeRealtimeText(String value) {
        return value == null || value.isBlank() ? "" : value;
    }

    public String formatVnd(double amount) {
        return VND_FORMATTER.format(Math.round(amount)) + " VND";
    }

    public String formatPlainNumber(double amount) {
        return VND_FORMATTER.format(Math.round(amount));
    }
}
