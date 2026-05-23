package client.logic;

import server.model.Auction;
import server.model.Item;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ProductSearchLogic {
    private ProductSearchLogic() {
    }

    public static List<Auction> filterVisibleMatches(List<Auction> auctions, String keyword) {
        String normalizedKeyword = normalize(keyword);
        AuctionFilterStrategy filter = AuctionFilters.visibleActive()
                .and(AuctionFilters.hasTimeRemaining())
                .and(AuctionFilters.keyword(normalizedKeyword));
        return auctions == null ? List.of() : auctions.stream()
                .filter(filter::accepts)
                .sorted(Comparator.comparingLong(Auction::getEndTime)
                        .thenComparing(auction -> normalize(auction.getItem().getName())))
                .toList();
    }

    public static boolean matchesKeyword(Auction auction, String normalizedKeyword) {
        if (normalizedKeyword == null || normalizedKeyword.isBlank()) {
            return true;
        }
        if (auction == null || auction.getItem() == null) {
            return false;
        }
        Item item = auction.getItem();
        String searchableText = normalize(String.join(" ",
                safe(item.getName()),
                item.getCategory() != null ? item.getCategory().name() : "",
                safe(auction.getSellerName())));
        return searchableText.contains(normalizedKeyword);
    }

    public static boolean isVisibleAuction(Auction auction) {
        return AuctionFilters.visibleActive().accepts(auction);
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
