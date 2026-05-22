package client.logic;

import common.AuctionStatus;
import common.ItemCategory;
import server.model.Auction;

public final class AuctionFilters {
    private AuctionFilters() {
    }

    public static AuctionFilterStrategy visibleActive() {
        return auction -> auction != null
                && auction.getItem() != null
                && (auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING);
    }

    public static AuctionFilterStrategy hasTimeRemaining() {
        return auction -> auction != null && auction.getTimeRemainingSeconds() > 0;
    }

    public static AuctionFilterStrategy category(ItemCategory category) {
        return auction -> category != null
                && auction != null
                && auction.getItem() != null
                && category == auction.getItem().getCategory();
    }

    public static AuctionFilterStrategy keyword(String keyword) {
        String normalizedKeyword = ProductSearchLogic.normalize(keyword);
        return auction -> ProductSearchLogic.matchesKeyword(auction, normalizedKeyword);
    }
}
