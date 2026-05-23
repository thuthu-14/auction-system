package client.logic;

import common.ItemCategory;
import server.model.Auction;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DashboardLogic {

    public List<Auction> endingSoonAuctions(List<Auction> auctions, int limit) {
        return visibleActiveAuctions(auctions).stream()
                .filter(auction -> auction.getTimeRemainingSeconds() > 0)
                .sorted(Comparator.comparingLong(Auction::getTimeRemainingSeconds))
                .limit(limit)
                .toList();
    }

    public List<Auction> suggestedAuctions(List<Auction> auctions,
                                           List<String> recentAuctionIds,
                                           Map<ItemCategory, Integer> categoryWeights,
                                           boolean hasSignals,
                                           int limit) {
        List<Auction> visibleAuctions = visibleActiveAuctions(auctions).stream()
                .filter(auction -> auction.getTimeRemainingSeconds() > 0)
                .toList();

        Set<String> recentIdSet = new HashSet<>(recentAuctionIds == null ? List.of() : recentAuctionIds);
        Map<String, Integer> auctionRank = auctionRank(recentAuctionIds);
        Map<ItemCategory, Integer> weights = categoryWeights == null ? Map.of() : categoryWeights;

        List<Auction> suggestions = visibleAuctions.stream()
                .filter(auction -> auction.getAuctionId() == null || !recentIdSet.contains(auction.getAuctionId()))
                .sorted(Comparator.comparingInt((Auction auction) -> recommendationScore(auction, weights, auctionRank)).reversed()
                        .thenComparingInt(this::bidCount).reversed()
                        .thenComparingDouble(Auction::getCurrentPrice).reversed()
                        .thenComparingLong(Auction::getEndTime))
                .limit(limit)
                .toList();

        if (!hasSignals || suggestions.isEmpty()) {
            return visibleAuctions.stream()
                    .sorted(Comparator.comparingInt(this::bidCount).reversed()
                            .thenComparingDouble(Auction::getCurrentPrice).reversed()
                            .thenComparingLong(Auction::getEndTime))
                    .limit(limit)
                    .toList();
        }

        return suggestions;
    }

    public int recommendationScore(Auction auction,
                                   Map<ItemCategory, Integer> categoryWeights,
                                   Map<String, Integer> auctionRank) {
        int score = 0;
        if (auction == null) {
            return score;
        }
        if (auction.getAuctionId() != null) {
            score += (auctionRank == null ? Map.<String, Integer>of() : auctionRank)
                    .getOrDefault(auction.getAuctionId(), 0) * 20;
        }
        if (auction.getItem() != null) {
            score += (categoryWeights == null ? Map.<ItemCategory, Integer>of() : categoryWeights)
                    .getOrDefault(auction.getItem().getCategory(), 0) * 12;
        }
        score += Math.min(bidCount(auction), 10);
        return score;
    }

    public boolean isVisibleAuction(Auction auction) {
        return AuctionFilters.visibleActive().accepts(auction);
    }

    public int bidCount(Auction auction) {
        return auction != null && auction.getBidIds() != null ? auction.getBidIds().size() : 0;
    }

    public Map<String, Integer> auctionRank(List<String> recentAuctionIds) {
        Map<String, Integer> auctionRank = new HashMap<>();
        List<String> ids = recentAuctionIds == null ? List.of() : recentAuctionIds;
        for (int i = 0; i < ids.size(); i++) {
            auctionRank.put(ids.get(i), ids.size() - i);
        }
        return auctionRank;
    }

    private List<Auction> visibleActiveAuctions(List<Auction> auctions) {
        return (auctions == null ? List.<Auction>of() : auctions).stream()
                .filter(Objects::nonNull)
                .filter(AuctionFilters.visibleActive()::accepts)
                .toList();
    }
}
